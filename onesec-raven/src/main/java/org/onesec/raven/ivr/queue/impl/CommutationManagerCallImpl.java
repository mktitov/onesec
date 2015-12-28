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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.IvrEndpointConversationListenerAdapter;
import org.onesec.raven.ivr.queue.*;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class  CommutationManagerCallImpl 
    implements CommutationManagerCall, IvrConversationsBridgeListener, EndpointRequest
            , RequestControllerListener
{
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";
    public enum State {INIT, NO_FREE_ENDPOINTS, INVITING, OPERATOR_READY, ABONENT_READY, 
        COMMUTATED, CONVERSATION_STARTED, HANDLED, INVALID}
    public final static Map<State, EnumSet<State>> TRANSITIONS = new EnumMap<>(State.class);

    private final CallsCommutationManager manager;
    private String number;
    private final LoggerHelper logger;
    
    private final AtomicReference<String> statusMessage;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final List<CallsCommutationManagerListener> listeners = new LinkedList<>();
    private final AtomicReference<IvrEndpointConversation> operatorConversation = new AtomicReference<>();
    private final AtomicReference<IvrConversationsBridge> bridge = new AtomicReference<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
    private IvrEndpoint endpoint = null;
    private IvrEndpointConversation conversation = null;
    
    static {
        TRANSITIONS.put(State.INIT, EnumSet.of(State.INVITING, State.NO_FREE_ENDPOINTS, State.INVALID));
        TRANSITIONS.put(State.NO_FREE_ENDPOINTS, EnumSet.of(State.INVALID));
        TRANSITIONS.put(State.INVITING, EnumSet.of(State.OPERATOR_READY, State.INVALID));
        TRANSITIONS.put(State.OPERATOR_READY, EnumSet.of(State.ABONENT_READY, State.INVALID));
        TRANSITIONS.put(State.ABONENT_READY, EnumSet.of(State.COMMUTATED, State.INVALID));
        TRANSITIONS.put(State.COMMUTATED, EnumSet.of(State.CONVERSATION_STARTED, State.INVALID));
        TRANSITIONS.put(State.CONVERSATION_STARTED, EnumSet.of(State.HANDLED, State.INVALID));
        TRANSITIONS.put(State.HANDLED, EnumSet.of(State.INVALID));
        TRANSITIONS.put(State.INVALID, null);
    }

    public CommutationManagerCallImpl(CallsCommutationManager manager, String number) {
        this.manager = manager;
        this.number = number;
        this.logger = new LoggerHelper(manager.getOperator(), "Operator ("+manager.getOperator().getName()+"). ");
//        this.logger = manager.getOperator().getLogger();
        this.statusMessage = new AtomicReference<>(
                String.format(SEARCHING_FOR_ENDPOINT_MSG, manager.getRequest().toString()));
    }
    
    private synchronized void moveToState(State newState, Throwable e, CompletionCode completionCode) {
        //if current state is invalid then do nothing
        if (state.get()==State.INVALID || state.get()==newState)
            return;
        //first, check is transition possible
        State nextState = null;
        Throwable nextException = null;
        if (newState==State.COMMUTATED && (state.get()==State.COMMUTATED || state.get()==State.CONVERSATION_STARTED))
            return;
        if (newState==State.CONVERSATION_STARTED && state.get()==State.ABONENT_READY) {
            newState = State.COMMUTATED;
            nextState = State.CONVERSATION_STARTED;
        }
        if (!TRANSITIONS.get(state.get()).contains(newState)) {
            if (logger.isErrorEnabled())
                logger.error(String.format("Invalid transition. Can't move from state (%s) to state (%s)"
                        , state.get().name(), newState.name()));
            moveToState(State.INVALID, null, null);
        }
        try {            
            switch (newState) {
                case NO_FREE_ENDPOINTS: 
                    if (e!=null) {
                        if (logger.isErrorEnabled())
                            logger.error("Get endpoint from pool error", e);
                        addToLog("get endpoint from pool error");
                    } else {
                        if (logger.isWarnEnabled())
                            logger.warn(String.format("Can't process call queue request because of no "
                                    + "free endpoints in the pool (%s)"
                                    , manager.getEndpointPool().getName()));
                        addToLog("no free endpoints in the pool");
                        manager.incOnNoFreeEndpointsRequests();
                    }
                    nextState = State.INVALID;
                    break;
                case INVITING: /*getRequest().fireOperatorNumberQueueEvent(getOperatorNumber());*/ 
                    this.getRequest().addRequestWrapperListener(this);
                    break;
                case OPERATOR_READY: 
                    if (logger.isDebugEnabled())
                        logger.debug(String.format("Number (%s) ready to commutate", getOperatorNumber()));
                    addToLog("op. number (%s) ready to commutate", getOperatorNumber());
//                    this.getRequest().addRequestWrapperListener(this);
                    if (!getRequest().fireReadyToCommutateQueueEvent(this)) 
                        nextState = State.INVALID;
                    break;
                case ABONENT_READY:
                    if (logger.isDebugEnabled())
                        logger.debug("Abonent ready to commutate");
                    addToLog("abonent ready to commutate");
                    fireAbonentReadyEvent();
                    commutateCalls();
                    break;
                case COMMUTATED: getRequest().fireCommutatedEvent(); break;
                case CONVERSATION_STARTED: getRequest().fireConversationStartedEvent(); break;
                case HANDLED: 
                    if (logger.isDebugEnabled())
                        logger.debug("Operator's conv. completed");
                    addToLog(String.format("conv. for op.num. (%s) completed (%s)", getOperatorNumber()
                            , completionCode));
                    manager.getRequest().fireDisconnectedQueueEvent(completionCode==null?null:completionCode.name());
                    nextState = State.INVALID;
                    break;
                case INVALID: 
                    getRequest().removeRequestWrapperListener(this);
                    boolean success = ObjectUtils.in(state.get(), State.HANDLED, State.OPERATOR_READY, State.ABONENT_READY)
                                        || canceled.get();
                    if (!success) {
                        if (completionCode!=null) {
                            if (logger.isDebugEnabled())
                                logger.debug(String.format("Operator's number (%s) not answered", getOperatorNumber()));
                            addToLog("no answer from (%s)", getOperatorNumber());
                        }
                    } else if (ObjectUtils.in(state.get(), State.OPERATOR_READY, State.ABONENT_READY, State.COMMUTATED) || canceled.get()) {
                        final String cause = completionCode==null? "SYSTEM_ERROR" : completionCode.name();
                        manager.getRequest().fireDisconnectedQueueEvent(cause);
                    }
                    manager.callFinished(this, success);
                    if (endpoint!=null) {
                        if (conversation!=null)
                            conversation.stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
                        manager.getEndpointPool().releaseEndpoint(endpoint);
                    }
                    break;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully moved to state "+newState+" from state "+state.get());
            }
            state.set(newState);
        } catch (Throwable ex) {
            nextState = State.INVALID;
            if (logger.isErrorEnabled())
                logger.error(String.format("Error make a transition from state (%s) to state (%s)"
                        , state.get(), newState), ex);
        }
        if (nextState!=null)
            moveToState(nextState, nextException, completionCode);
    }

    @Override
    public void requestInvalidated() {
        callMoveToState(getState()==State.CONVERSATION_STARTED? State.HANDLED:State.INVALID, null, null);
    }

    @Override
    public void processingByOperator(CommutationManagerCall operatorCall) {
        if (operatorCall!=this)
            callMoveToState(State.INVALID, null, null);
    }
    
    private void callMoveToState(final State newState, final Throwable ex, final CompletionCode completionCode) {
        String mess = String.format("Making transition from state (%s) to state (%s)"
                , getState().name(), newState.name());
        if (logger.isDebugEnabled())
            logger.debug(mess);
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
    
//    private boolean isLogLevelEnabled(LogLevel logLevel) {
////        logger.is
//        return manager.getOperator().isLogLevelEnabled(logLevel);
//    }

    @Override
    public boolean isCommutationValid() {
        return getState()!=State.INVALID;
    }

    @Override
    public String getOperatorNumber(){
        return number;
    }

    @Override
    public IvrEndpointConversation getOperatorConversation() {
        return operatorConversation.get();
    }

    @Override
    public CallsQueueOperator getOperator() {
        return manager.getOperator();
    }

    @Override
    public void transferToOperator(String operatorNumber) {
        IvrEndpointConversation conv = operatorConversation.get();
        if (conv!=null)
            conv.makeLogicalTransfer(operatorNumber);
    }
    
    private void reactivateBridge() {
        IvrConversationsBridge _bridge = bridge.get();
        if (_bridge != null)
            _bridge.reactivateBridge();
    }

    public CallQueueRequestController getRequest() {
        return manager.getRequest();
    }

    @Override
    public void addListener(CallsCommutationManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CallsCommutationManagerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void commutate() {
        try {
            if (manager.getRequest().isValid() && !manager.getRequest().isHandlingByOperator())
                manager.getEndpointPool().requestEndpoint(this);
            else
                moveToState(State.INVALID, null, null);
        } catch (ExecutorServiceException ex) {
            moveToState(State.NO_FREE_ENDPOINTS, ex, null);
        }
    }

    @Override
    public void cancel() {
        canceled.set(true);
    }

    @Override
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
        final String operNumber = getOperatorNumber();
        final String abonentNumber = manager.getOperator().translateAbonentNumber(getRequest().getAbonentNumber(), operNumber);
        endpoint.invite(getOperatorNumber(), (int)manager.getInviteTimeout()/1000, 0
                , new OperatorConversationListener()
                , manager.getConversationScenario(), bindings, 
                abonentNumber);
    }

    public void commutateCalls() throws IvrConversationBridgeExeption {
        IvrConversationsBridge _bridge = manager.getConversationsBridgeManager().createBridge(
                getRequest().getConversation(), operatorConversation.get(), logger.getPrefix());
        bridge.set(_bridge);
        final ConversationStartedListener convStartListener = new ConversationStartedListener(_bridge);
        convStartListener.init();
        _bridge.addBridgeListener(this);
        _bridge.activateBridge();
        
    }

    @Override
    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation) {
        this.operatorConversation.set(operatorConversation);
        callMoveToState(State.OPERATOR_READY, null, null);
    }

    @Override
   public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        callMoveToState(State.ABONENT_READY, null, null);
    }

    @Override
    public void bridgeReactivated(IvrConversationsBridge bridge) { }

    @Override
    public void bridgeActivated(IvrConversationsBridge bridge) {
        if (state.get()!=State.COMMUTATED)
            callMoveToState(State.COMMUTATED, null, null);
    }

    @Override
    public void bridgeDeactivated(IvrConversationsBridge bridge) { }

//    String logMess(String message, Object... args) {
//        return manager.getRequest().logMess("Operator ("+manager.getOperator().getName()+"). "+message, args);
//    }

    @Override
    public long getWaitTimeout() {
        return manager.getWaitTimeout();
    }

    @Override
    public Node getOwner() {
        return manager.getOperator();
    }

    @Override
    public String getStatusMessage() {
        return statusMessage.get();
    }

    @Override
    public int getPriority() {
        return manager.getRequest().getPriority();
    }

    private void fireAbonentReadyEvent() {
        final List<CallsCommutationManagerListener> _listeners = new ArrayList<>(listeners);
        if (_listeners.isEmpty())
            return;
        manager.getExecutor().executeQuietly(new AbstractTask(manager.getOperator(), "Delivering abonent ready event") {
            @Override public void doRun() throws Exception {
                for (CallsCommutationManagerListener listener: _listeners)
                    listener.abonentReady();
            }
        });
    }
    
    private class ConversationStartedListener extends IvrEndpointConversationListenerAdapter {
        private final IvrConversationsBridge bridge;

        public ConversationStartedListener(final IvrConversationsBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public void listenerAdded(IvrEndpointConversationEvent event) {
            checkConversationStart();
        }

        public void init() {
            bridge.getConversation1().addConversationListener(this);
            bridge.getConversation2().addConversationListener(this);            
        }

        @Override
        public void connectionEstablished(IvrEndpointConversationEvent event) {
            System.out.println("\n\n\n CONNECTION ESTABLISHED: "+event.getConversation()+"\n\n\n");
            checkConversationStart();
        }

        private void checkConversationStart() {
            if (bridge.getConversation1().isConnectionEstablished() && bridge.getConversation2().isConnectionEstablished())
                callMoveToState(State.CONVERSATION_STARTED, null, null);
        }
    }
    
    private class OperatorConversationListener extends IvrEndpointConversationListenerAdapter {
        @Override
        public void listenerAdded(IvrEndpointConversationEvent event) {
            conversation = event.getConversation();
        }

        @Override
        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            moveToState(getState()==State.CONVERSATION_STARTED? State.HANDLED:State.INVALID, null, event.getCompletionCode());
        }

        @Override
        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
            synchronized(CommutationManagerCallImpl.this) {
                if (state.get()==State.COMMUTATED || state.get()==State.CONVERSATION_STARTED) {
                    number = event.getTransferAddress();
                    manager.callTransfered(number);
                }
            }
            reactivateBridge();
        }
    }
}
