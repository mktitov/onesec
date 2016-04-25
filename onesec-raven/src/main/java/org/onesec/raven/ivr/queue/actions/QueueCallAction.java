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
package org.onesec.raven.ivr.queue.actions;

import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.PlayAudioDP;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.impl.CallQueueRequestImpl;
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.NonUniqueNameException;
import org.raven.dp.impl.Behaviour;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public class QueueCallAction extends AbstractAction
{
    private final static String ACTION_NAME = "Queue call";
    public final static String QUEUED_CALL_STATUS_BINDING = "queuedCallStatus";
    
    public final static String COMMUTATED = "COMMUTATED";
    public final static String COMMUTATE = "COMMUTATE";
    public final static String DISCONNECTED = "DISCONNECTED";
    
    private final boolean continueConversationOnReadyToCommutate;
    private final boolean continueConversationOnReject;
    private final CallQueueRequestSender requestSender;
    private final int priority;
    private final String queueId;
    private final boolean playOperatorGreeting;
    private final String operatorPhoneNumbers;
    
    private IvrEndpointConversation conversation;
    private RequestListener requestListener;

    public QueueCallAction(CallQueueRequestSender requestSender
            , boolean continueConversationOnReadyToCommutate, boolean continueConversationOnReject
            , int priority, String queueId, boolean playOperatorGreeting, String operatorPhoneNumbers)
    {
        super(ACTION_NAME);
        this.continueConversationOnReadyToCommutate = continueConversationOnReadyToCommutate;
        this.continueConversationOnReject = continueConversationOnReject;
        this.requestSender = requestSender;
        this.priority = priority;
        this.queueId = queueId;
        this.playOperatorGreeting = playOperatorGreeting;
        this.operatorPhoneNumbers = operatorPhoneNumbers;
    }

    public boolean isContinueConversationOnReadyToCommutate() {
        return continueConversationOnReadyToCommutate;
    }

    public boolean isContinueConversationOnReject() {
        return continueConversationOnReject;
    }

    public int getPriority() {
        return priority;
    }

    public String getQueueId() {
        return queueId;
    }

    public CallQueueRequestSender getRequestSender() {
        return requestSender;
    }

    @Override
    public void postStop() {
        super.postStop();
        if (requestListener!=null)
            requestListener.removeListener();
    }
    
    //play greeting behaviour. Processing cancel event, disconnect event, 
    //wait for commutated behaviour. Processing cancel event, disconnect event, commutated event
    //wait for disconnect behaviour. Processing cancel event, disconnect event
    private final Behaviour disonnected = new Behaviour("Disconnected") {
        @Override public Object processData(Object message) throws Exception {
            if (message==DISCONNECTED) {
                final ConversationScenarioState state = conversation.getConversationScenarioState();
                state.enableDtmfProcessing();
                state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
                if (getLogger().isDebugEnabled())
                    getLogger().debug("Commutation disconnected");
                sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
                return VOID;
            }
            return UNHANDLED;
        }
    };
    
    public final Behaviour cancel = new CancelBehaviour(ACTION_EXECUTED_then_EXECUTE_NEXT);
    
    private class PlayGreeting extends Behaviour {
        private final QueuedCallStatus callStatus;
        
        public PlayGreeting(AudioFile greeting, QueuedCallStatus callStatus) throws NonUniqueNameException {
            super("Play greeting");
            this.callStatus = callStatus;
            DataProcessorFacade greetingPlayer = getContext().addChild(
                    getContext().createChild("Greeting player", new PlayAudioDP(conversation.getAudioStream())));
            getFacade().sendTo(greetingPlayer, new PlayAudioDP.PlayAudioFile(greeting));
        }

        @Override public Object processData(Object message) throws Exception {
            if (message == PlayAudioDP.PLAYED) {
                become(commutating);
                getFacade().send(callStatus);
                return VOID;
            }
            return UNHANDLED;
        }
    };
    
    private final Behaviour commutating = new Behaviour("Commutating") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof QueuedCallStatus) {
                conversation.getConversationScenarioState().getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
                ((QueuedCallStatus)message).replayToReadyToCommutate();
                if (getLogger().isDebugEnabled())
                    getLogger().debug("Operator and abonent are ready to commutate. Commutating...");
                return VOID;
            } else if (message==COMMUTATED) {
                return becomeCommutated();
            }
            return UNHANDLED;
        }
    }.andThen(disonnected).andThen(cancel);

    private Object becomeCommutated() {
        become(commutated);
        conversation.getConversationScenarioState().disableDtmfProcessing();
        if (getLogger().isDebugEnabled())
            getLogger().debug("Abonent and operator were commutated. Waiting for disconnected event...");
        return VOID;
    }

    private final Behaviour commutated = new Behaviour("Commutated") {
        @Override public Object processData(Object dataPackage) throws Exception {
            return UNHANDLED;
        }
    }.andThen(disonnected).andThen(cancel);
    

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        conversation = message.getConversation();
        final ConversationScenarioState state = conversation.getConversationScenarioState();
        QueuedCallStatus callStatus = (QueuedCallStatus) state.getBindings().get(QUEUED_CALL_STATUS_BINDING);        
        if (callStatus==null) {
            DataContext context = (DataContext) state.getBindings().get(BindingNames.DATA_CONTEXT_BINDING);
            if (context==null)
                context = requestSender.createDataContext();
            callStatus = new CallQueueRequestImpl(
                    conversation, priority, queueId, operatorPhoneNumbers
                    , continueConversationOnReadyToCommutate
                    , continueConversationOnReject, context, getLogger());
            state.setBinding(QUEUED_CALL_STATUS_BINDING, callStatus, BindingScope.POINT);
            requestSender.sendCallQueueRequest(callStatus, context);
            return ACTION_EXECUTED_then_EXECUTE_NEXT;
        } else if (callStatus.isReadyToCommutate()) {
            requestListener = new RequestListener(callStatus);
            callStatus.addRequestListener(requestListener);
            if (playOperatorGreeting && callStatus.getOperatorGreeting()!=null)
                become(new PlayGreeting(callStatus.getOperatorGreeting(), callStatus).andThen(cancel));
            else {
                become(commutating);
                getFacade().send(callStatus);
            }
            return null;
        } else if (callStatus.isCommutated()) { //“ака€ ситуаци€ может возникнуть тогда, когда проверка 
                                                //callStatus.isReadyToCommutate() в коде выше, отработает 
                                                //раньше continueConversationOnReadyToCommutate
            requestListener = new RequestListener(callStatus);
            callStatus.addRequestListener(requestListener);
            becomeCommutated();
            return null;
        } else
            return ACTION_EXECUTED_then_EXECUTE_NEXT;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
    
    private class RequestListener implements CallQueueRequestListener {
        private final QueuedCallStatus callStatus;

        public RequestListener(QueuedCallStatus callStatus) {
            this.callStatus = callStatus;
        }
        
        public void removeListener() {
            callStatus.removeRequestListener(this);
        }

        @Override public void requestCanceled(String cause) { }
        @Override public void conversationAssigned(IvrEndpointConversation conversation) { }

        @Override  public void commutated() {
            getFacade().send(COMMUTATED);
        }
        @Override public void disconnected() {
            getFacade().send(DISCONNECTED);
        }
    }

//    @Override
//    protected void doExecute(IvrEndpointConversation conversation) throws Exception
//    {
//        final ConversationScenarioState state = conversation.getConversationScenarioState();
//        final Bindings bindings = state.getBindings();
//        QueuedCallStatus callStatus = (QueuedCallStatus) state.getBindings().get(
//                QUEUED_CALL_STATUS_BINDING);
//        if (callStatus==null){
//            DataContext context = (DataContext) state.getBindings().get(BindingNames.DATA_CONTEXT_BINDING);
//            if (context==null)
//                context = requestSender.createDataContext();
//            callStatus = new CallQueueRequestImpl(
//                    conversation, priority, queueId, operatorPhoneNumbers
//                    , continueConversationOnReadyToCommutate
//                    , continueConversationOnReject, context, logger);
//            state.setBinding(QUEUED_CALL_STATUS_BINDING, callStatus, BindingScope.POINT);
//            requestSender.sendCallQueueRequest(callStatus, context);
//        } else if (callStatus.isReadyToCommutate() || callStatus.isCommutated()) {
//            if (callStatus.isReadyToCommutate()) {
//                if (playOperatorGreeting){
//                    if (logger.isDebugEnabled())
//                        logger.debug("Playing operator greeting");
//                    AudioFile greeting = callStatus.getOperatorGreeting();
//                    if (greeting!=null)
//                        IvrUtils.playAudioInAction(this, conversation, greeting);
//                }
//                if (logger.isDebugEnabled())
//                    logger.debug("Operator and abonent are ready to commutate. Commutating...");
//                //disable audio stream reset??? Then when t to enable???
//                bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
//                callStatus.replayToReadyToCommutate();
//                do {
//                    TimeUnit.MILLISECONDS.sleep(10);
//                } while (!callStatus.isCommutated() && !hasCancelRequest() && !callStatus.isDisconnected());
//                if (hasCancelRequest()) return;
//            }
//            if (logger.isDebugEnabled())
//                logger.debug("Abonent and operator were commutated. Waiting for disconnected event...");
//            state.disableDtmfProcessing();
//            try {
//                do {
//                    TimeUnit.MILLISECONDS.sleep(10);
//                } while (!callStatus.isDisconnected() && !hasCancelRequest());
//            } finally {
//                state.enableDtmfProcessing();
//                bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
//            }            
//            if (logger.isDebugEnabled())
//                logger.debug("Commutation disconnected");
//        }
//    }

}
