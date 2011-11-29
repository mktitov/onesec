/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.queue.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.IvrEndpointConversationListenerAdapter;
import org.onesec.raven.ivr.queue.*;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class  CommutationManagerCallImpl 
    implements CommutationManagerCall, IvrConversationsBridgeListener, EndpointRequest
            , RequestWrapperListener
{
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";
    public enum State {INIT, NO_FREE_ENDPOINTS, INVITING, OPERATOR_READY, ABONENT_READY, COMMUTATED
        , HANDLED, INVALID}
    public final static Map<State, EnumSet<State>> TRANSITIONS = 
            new EnumMap<State, EnumSet<State>>(State.class);

    private final CallsCommutationManager manager;
    private final String number;
    private final Logger logger;
    
    private final AtomicReference<String> statusMessage;
    private final List<CallsCommutationManagerListener> listeners =
            new LinkedList<CallsCommutationManagerListener>();
//    private AtomicBoolean operatorConversationFlag = new AtomicBoolean(false);
//    private boolean operatorReadyToCommutate = false;
//    private boolean readyToCommutateSended = false;
//    private boolean abonentReadyToCommutate = false;
//    private boolean commutated = false;
//    private AtomicBoolean commutationValid = new AtomicBoolean(true);
    private IvrEndpointConversation operatorConversation = null;
    private ReentrantLock lock = new ReentrantLock();
//    private Condition eventCondition = lock.newCondition();
    private State state = State.INIT;
    private IvrEndpoint endpoint = null;
    
    static {
        TRANSITIONS.put(State.INIT, EnumSet.of(State.INVITING, State.NO_FREE_ENDPOINTS, State.INVALID));
        TRANSITIONS.put(State.NO_FREE_ENDPOINTS, EnumSet.of(State.INVALID));
        TRANSITIONS.put(State.INVITING, EnumSet.of(State.OPERATOR_READY, State.INVALID));
        TRANSITIONS.put(State.OPERATOR_READY, EnumSet.of(State.ABONENT_READY, State.INVALID));
        TRANSITIONS.put(State.ABONENT_READY, EnumSet.of(State.COMMUTATED, State.INVALID));
        TRANSITIONS.put(State.COMMUTATED, EnumSet.of(State.HANDLED, State.INVALID));
        TRANSITIONS.put(State.HANDLED, EnumSet.of(State.INVALID));
        TRANSITIONS.put(State.INVALID, null);
    }

    public CommutationManagerCallImpl(CallsCommutationManager manager, String number) {
        this.manager = manager;
        this.number = number;
        this.logger = manager.getOperator().getLogger();
        this.statusMessage = new AtomicReference<String>(
                String.format(SEARCHING_FOR_ENDPOINT_MSG, manager.getRequest().toString()));
    }
    
    private synchronized void moveToState(State newState, Throwable e, CompletionCode completionCode) {
        //if current state is invalid then do nothing
        if (state==State.INVALID)
            return;
        //first, check is transition is possible
        if (!TRANSITIONS.get(state).contains(newState)) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(logMess("Invalid transition. Can't move from state (%s) to state (%s)"
                        , state.name(), newState.name()));
            moveToState(State.INVALID, null, null);
        }
        State nextState = null;
        Throwable nextException = null;
        try {
            switch (newState) {
                case NO_FREE_ENDPOINTS: 
                    if (e!=null) {
                        if (isLogLevelEnabled(LogLevel.ERROR))
                            logger.error(logMess("Get endpoint from pool error"), e);
                        addToLog("get endpoint from pool error");
                    } else {
                        if (isLogLevelEnabled(LogLevel.WARN))
                            logger.warn(logMess("Can't process call queue request because of no "
                                    + "free endpoints in the pool (%s)"
                                    , manager.getEndpointPool().getName()));
                        addToLog("no free endpoints in the pool");
                        manager.incOnNoFreeEndpointsRequests();
                    }
                    nextState = State.INVALID;
                    break;
                case INVITING: getRequest().fireOperatorNumberQueueEvent(getNumber()); break;
                case OPERATOR_READY: 
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Number (%s) ready to commutate", getNumber()));
                    addToLog("op. number (%s) ready to commutate", getNumber());
                    if (!manager.getRequest().fireReadyToCommutateQueueEvent(this)) 
                        nextState = State.INVALID;
                    break;
                case ABONENT_READY:
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Abonent ready to commutate"));
                    addToLog("abonent ready to commutate");
                    fireAbonentReadyEvent();
                    commutateCalls();
                    break;
                case COMMUTATED: manager.getRequest().fireCommutatedEvent(); break;
                case HANDLED: 
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Operator's conversation completed"));
                    addToLog(String.format("conv. for op. number (%s) completed (%s)", getNumber()
                            , completionCode));
                    manager.getRequest().fireDisconnectedQueueEvent();
                    nextState = State.INVALID;
                    break;
                case INVALID: 
                    boolean success = state==State.HANDLED;
                    if (!success) {
                        if (completionCode!=null) {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                logger.debug(logMess("Operator's number (%s) not answered", getNumber()));
                            addToLog("number (%s) not answer", getNumber());
                        }
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            logger.debug(logMess("Call not handled"));
                        addToLog("operator (%s) didn't handle a call", manager.getOperator().getName());
                    } 
                    manager.callFinished(this, success);
                    if (endpoint!=null)
                        manager.getEndpointPool().releaseEndpoint(endpoint);
                    break;
            }
            state = newState;
        } catch (Throwable ex) {
            nextState = State.INVALID;
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(logMess("Error make a transition from state (%s) to state (%s)"
                        , state, newState), ex);
        }
        if (nextState!=null)
            moveToState(nextState, nextException, completionCode);
    }

    public void requestInvalidated() {
        callMoveToState(getState()==State.COMMUTATED? State.HANDLED:State.INVALID, null, null);
    }
    
    private void callMoveToState(final State newState, final Throwable ex, final CompletionCode completionCode) {
        String mess = String.format("Making transition from state (%s) to state (%s)"
                , getState().name(), newState.name());
        boolean res = manager.getExecutor().executeQuietly(
                new AbstractTask(manager.getOperator(), mess) {
                    @Override public void doRun() throws Exception {
                        moveToState(newState, ex, completionCode);
                    }
                });
        if (!res)
            moveToState(State.INVALID, null, null);
    }
    
    private synchronized State getState() {
        return state;
    }
    
    private void addToLog(String mess, Object... args) {
        manager.getRequest().addToLog(String.format(mess, args));
    }
    
    private boolean isLogLevelEnabled(LogLevel logLevel) {
        return manager.getOperator().isLogLevelEnabled(logLevel);
    }

//    private boolean checkState() {
//        commutationValid.compareAndSet(true, manager.getRequest().isValid()
//                && (!manager.getRequest().isHandlingByOperator() || readyToCommutateSended)
//                && operatorConversationFlag.get());
//        return commutationValid.get();
//    }
//
    public boolean isCommutationValid() {
        return getState()==State.COMMUTATED;
    }

    private String getNumber(){
        return number;
    }

    public CallQueueRequestWrapper getRequest() {
        return manager.getRequest();
    }

    public void addListener(CallsCommutationManagerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CallsCommutationManagerListener listener) {
        listeners.remove(listener);
    }

    public void commutate() {
        try {
            getRequest().addRequestWrapperListener(this);
            if (manager.getRequest().isValid() && !manager.getRequest().isHandlingByOperator())
                manager.getEndpointPool().requestEndpoint(this);
            else
//                manager.callFinished(this, false);
                moveToState(State.INVALID, null, null);
        } catch (ExecutorServiceException ex) {
            moveToState(State.NO_FREE_ENDPOINTS, ex, null);
//            if (manager.getOperator().isLogLevelEnabled(LogLevel.ERROR))
//                manager.getOperator().getLogger().error(logMess(
//                        "Get endpoint from pool error"), ex);
//            manager.getRequest().addToLog("get endpoint from pool error");
//            manager.callFinished(this, false);
//            manager.incOnNoFreeEndpointsRequests();
        }
    }

    public void processRequest(IvrEndpoint endpoint) {
//        boolean callHandled = false;
//        try{
            if (endpoint==null) {
                callMoveToState(State.NO_FREE_ENDPOINTS, null, null);
//                if (manager.getOperator().isLogLevelEnabled(LogLevel.WARN))
//                    manager.getOperator().getLogger().warn(logMess(
//                            "Can't process call queue request because of no free endpoints in the pool (%s)"
//                            , manager.getEndpointPool().getName()));
//                manager.getRequest().addToLog("no free endpoints in the pool");
//                manager.incOnNoFreeEndpointsRequests();;
                return;
            }
            synchronized (this) {
                this.endpoint = endpoint;
            }
            Map<String, Object> bindings = new HashMap<String, Object>();
            bindings.put(CALLS_COMMUTATION_MANAGER_BINDING, this);
            bindings.put(CALL_QUEUE_REQUEST_BINDING, manager.getRequest());
            callMoveToState(State.INVITING, null, null);
            endpoint.invite(getNumber(), (int)manager.getInviteTimeout()/1000, 0
                    , new OperatorConversationListener()
                    , manager.getConversationScenario(), bindings);
//            
//            try{
//                if (callToOperator(endpoint, bindings))
//                    callHandled = true;
//                else {
//                    if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
//                        manager.getOperator().getLogger().debug(logMess("Call not handled"));
//                    manager.getRequest().addToLog(String.format(
//                            "operator (%s) didn't handle a call", manager.getOperator().getName()));
//                }
//            } catch(Exception e){
//                if (manager.getOperator().isLogLevelEnabled(LogLevel.ERROR))
//                    manager.getOperator().getLogger().error(logMess("Error handling by operator"), e);
//                manager.getRequest().addToLog("error handling by operator");
//            }
//        } finally {
//            manager.callFinished(this, callHandled);
//        }
    }

//    public boolean callToOperator(IvrEndpoint endpoint, Map<String, Object> bindings) throws Exception
//    {
//        operatorConversationFlag.set(true);
//        if (!checkState())
//            return false;
//        manager.getRequest().fireOperatorNumberQueueEvent(getNumber());
//        //TODO: Fix invite
////        endpoint.invite(getNumber(), manager.getConversationScenario(), this, bindings);
//        lock.lock();
//        try {
//            long callStartTime = System.currentTimeMillis();
//            while (checkState()){
//                eventCondition.await(500, TimeUnit.MILLISECONDS);
//                if (!checkState())
//                    break;
//                if (!checkInviteTimeout(callStartTime, endpoint))
//                    return false;
//                if (operatorReadyToCommutate && !readyToCommutateSended){
//                    if (manager.getRequest().fireReadyToCommutateQueueEvent(this)) 
//                        readyToCommutateSended=true;
//                }
//                if (abonentReadyToCommutate && !commutated)
//                    commutateCalls();
//            }
//
//            if (!checkState() && endpoint.getEndpointState().getId()==IvrEndpointState.INVITING)
//                restartEndpoint(endpoint);
//
//            if (commutated) {
//                manager.getRequest().fireDisconnectedQueueEvent();
//                return true;
//            } else
//                return false;
//        } finally {
//            lock.unlock();
//        }
//        
//    }
//
//    private boolean checkInviteTimeout(long callStartTime, IvrEndpoint endpoint) throws Exception
//    {
//        if (endpoint.getEndpointState().getId()==IvrEndpointState.INVITING && manager.getInviteTimeout()<=0) {
//            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
//                manager.getOperator().getLogger().debug(logMess("Operator's number (%s) not answered", getNumber()));
//            manager.getRequest().addToLog(String.format("number (%s) not answer", getNumber()));
//            //restarting endpoint
//            restartEndpoint(endpoint);
//            return false;
//        }
//        return true;
//    }

    public void commutateCalls() throws IvrConversationBridgeExeption {
        IvrConversationsBridge bridge = manager.getConversationsBridgeManager().createBridge(
                getRequest().getConversation(), operatorConversation, logMess(""));
        bridge.addBridgeListener(this);
        bridge.activateBridge();
//        commutated = true;
    }

    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation)
    {
        this.operatorConversation = operatorConversation;
        callMoveToState(State.OPERATOR_READY, null, null);
//        lock.lock();
//        try {
//            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
//                manager.getOperator().getLogger().debug(logMess("Number (%s) ready to commutate", getNumber()));
//            operatorReadyToCommutate=true;
//            manager.getRequest().addToLog(String.format("op. number (%s) ready to commutate", getNumber()));
//            this.operatorConversation = operatorConversation;
//            eventCondition.signal();
//        } finally {
//            lock.unlock();
//        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        callMoveToState(State.ABONENT_READY, null, null);
//        lock.lock();
//        try {
//            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
//                manager.getOperator().getLogger().debug(logMess("Abonent ready to commutate"));
//            manager.getRequest().addToLog("abonent ready to commutate");
//            fireAbonentReadyEvent();
//            abonentReadyToCommutate = true;
//            eventCondition.signal();
//        } finally {
//            lock.unlock();
//        }
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) { }

    public void bridgeActivated(IvrConversationsBridge bridge) {
        callMoveToState(State.COMMUTATED, null, null);
//        manager.getRequest().fireCommutatedEvent();
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) { }

    String logMess(String message, Object... args) {
        return manager.getRequest().logMess("Operator ("+manager.getOperator().getName()+"). "+message, args);
    }

//    private void freeResources(){        
//    }
//
    public long getWaitTimeout() {
        return manager.getWaitTimeout();
    }

    public Node getOwner() {
        return manager.getOperator();
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public int getPriority() {
        return manager.getRequest().getPriority();
    }

//    public void conversationCompleted(ConversationCdr res) {
//        if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
//            manager.getOperator().getLogger().debug(logMess("Operator's conversation completed"));
//        lock.lock();
//        try {
//            operatorConversationFlag.set(false);
//            checkState();
//            manager.getRequest().addToLog(String.format(
//                    "conv. for op. number (%s) completed (%s)", getNumber(), res.getCompletionCode()));
//            eventCondition.signal();
//        } finally {
//            lock.unlock();
//        }
//    }

    private void fireAbonentReadyEvent() {
        for (CallsCommutationManagerListener listener: listeners)
            listener.abonentReady();
    }
    
    private class OperatorConversationListener extends IvrEndpointConversationListenerAdapter {
        @Override
        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            moveToState(getState()==State.COMMUTATED? State.HANDLED:State.INVALID, null, event.getCompletionCode());
        }
    }

//    private void restartEndpoint(IvrEndpoint endpoint) throws Exception {
//        endpoint.stop();
//        TimeUnit.SECONDS.sleep(1);
//        endpoint.start();
//        StateWaitResult res = endpoint.getEndpointState().waitForState(
//                new int[]{IvrEndpointState.IN_SERVICE}, 10000);
//        if (res.isWaitInterrupted())
//            throw new Exception("Wait for IN_SERVICE timeout");
//    }
}
