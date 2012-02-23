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
    private String number;
    private final Logger logger;
    
    private final AtomicReference<String> statusMessage;
    private final List<CallsCommutationManagerListener> listeners =
            new LinkedList<CallsCommutationManagerListener>();
    private IvrEndpointConversation operatorConversation = null;
    private ReentrantLock lock = new ReentrantLock();
    private AtomicReference<State> state = new AtomicReference<State>(State.INIT);
    private IvrEndpoint endpoint = null;
    private IvrEndpointConversation conversation = null;
    
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
        if (state.get()==State.INVALID)
            return;
        //first, check is transition possible
        if (!TRANSITIONS.get(state.get()).contains(newState)) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(logMess("Invalid transition. Can't move from state (%s) to state (%s)"
                        , state.get().name(), newState.name()));
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
                case INVITING: getRequest().fireOperatorNumberQueueEvent(getOperatorNumber()); break;
                case OPERATOR_READY: 
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Number (%s) ready to commutate", getOperatorNumber()));
                    addToLog("op. number (%s) ready to commutate", getOperatorNumber());
                    if (!getRequest().fireReadyToCommutateQueueEvent(this)) 
                        nextState = State.INVALID;
                    break;
                case ABONENT_READY:
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Abonent ready to commutate"));
                    addToLog("abonent ready to commutate");
                    fireAbonentReadyEvent();
                    commutateCalls();
                    break;
                case COMMUTATED: getRequest().fireCommutatedEvent(); break;
                case HANDLED: 
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(logMess("Operator's conv. completed"));
                    addToLog(String.format("conv. for op.num. (%s) completed (%s)", getOperatorNumber()
                            , completionCode));
                    manager.getRequest().fireDisconnectedQueueEvent();
                    nextState = State.INVALID;
                    break;
                case INVALID: 
                    boolean success = state.get()==State.HANDLED;
                    if (!success) {
                        if (completionCode!=null) {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                logger.debug(logMess("Operator's number (%s) not answered", getOperatorNumber()));
                            addToLog("no answer from (%s)", getOperatorNumber());
                        }
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            logger.debug(logMess("Call not handled by operator number (%s)", number));
                        //TODO: Не совсем корректно в случае параллельного вызова
//                        addToLog("operator (%s) didn't handle a call", manager.getOperator().getName());
                    } 
                    manager.callFinished(this, success);
                    if (endpoint!=null) {
                        if (conversation!=null)
                            conversation.stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
                        manager.getEndpointPool().releaseEndpoint(endpoint);
                    }
                    break;
            }
            state.set(newState);
        } catch (Throwable ex) {
            nextState = State.INVALID;
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(logMess("Error make a transition from state (%s) to state (%s)"
                        , state.get(), newState), ex);
        }
        if (nextState!=null)
            moveToState(nextState, nextException, completionCode);
    }

    public void requestInvalidated() {
        callMoveToState(getState()==State.COMMUTATED? State.HANDLED:State.INVALID, null, null);
    }

    public void processingByOperator(CommutationManagerCall operatorCall) {
        if (operatorCall!=this)
            callMoveToState(State.INVALID, null, null);
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
    
    private State getState() {
        return state.get();
    }
    
    private void addToLog(String mess, Object... args) {
        manager.getRequest().addToLog(String.format(mess, args));
    }
    
    private boolean isLogLevelEnabled(LogLevel logLevel) {
        return manager.getOperator().isLogLevelEnabled(logLevel);
    }

    public boolean isCommutationValid() {
        return getState()!=State.INVALID;
    }

    public String getOperatorNumber(){
        return number;
    }

    public CallsQueueOperator getOperator() {
        return manager.getOperator();
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
                moveToState(State.INVALID, null, null);
        } catch (ExecutorServiceException ex) {
            moveToState(State.NO_FREE_ENDPOINTS, ex, null);
        }
    }

    public void processRequest(IvrEndpoint endpoint) {
        if (endpoint==null) {
            callMoveToState(State.NO_FREE_ENDPOINTS, null, null);
            return;
        }
        synchronized (this) {
            this.endpoint = endpoint;
        }
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(CALLS_COMMUTATION_MANAGER_BINDING, this);
        bindings.put(CALL_QUEUE_REQUEST_BINDING, manager.getRequest());
        callMoveToState(State.INVITING, null, null);
        endpoint.invite(getOperatorNumber(), (int)manager.getInviteTimeout()/1000, 0
                , new OperatorConversationListener()
                , manager.getConversationScenario(), bindings);
    }

    public void commutateCalls() throws IvrConversationBridgeExeption {
        IvrConversationsBridge bridge = manager.getConversationsBridgeManager().createBridge(
                getRequest().getConversation(), operatorConversation, logMess(""));
        bridge.addBridgeListener(this);
        bridge.activateBridge();
    }

    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation) {
        this.operatorConversation = operatorConversation;
        callMoveToState(State.OPERATOR_READY, null, null);
    }

   public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        callMoveToState(State.ABONENT_READY, null, null);
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) { }

    public void bridgeActivated(IvrConversationsBridge bridge) {
        callMoveToState(State.COMMUTATED, null, null);
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) { }

    String logMess(String message, Object... args) {
        return manager.getRequest().logMess("Operator ("+manager.getOperator().getName()+"). "+message, args);
    }

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

    private void fireAbonentReadyEvent() {
        for (CallsCommutationManagerListener listener: listeners)
            listener.abonentReady();
    }
    
    private class OperatorConversationListener extends IvrEndpointConversationListenerAdapter {
        @Override
        public void listenerAdded(IvrEndpointConversationEvent event) {
            conversation = event.getConversation();
        }

        @Override
        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            moveToState(getState()==State.COMMUTATED? State.HANDLED:State.INVALID, null, event.getCompletionCode());
        }

        @Override
        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
            synchronized(CommutationManagerCallImpl.this) {
                if (state.get()==State.COMMUTATED) {
                    number = event.getTransferAddress();
                    manager.callTransfered(number);
                }
            }
        }
    }
}
