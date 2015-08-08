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

import java.util.concurrent.TimeUnit;
import javax.script.Bindings;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.impl.CallQueueRequestImpl;
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public class QueueCallAction extends AsyncAction
{
    private final static String ACTION_NAME = "Queue call action";
    public final static String QUEUED_CALL_STATUS_BINDING = "queuedCallStatus";
    
    private final boolean continueConversationOnReadyToCommutate;
    private final boolean continueConversationOnReject;
    private final CallQueueRequestSender requestSender;
    private final int priority;
    private final String queueId;
    private final boolean playOperatorGreeting;
    private final String operatorPhoneNumbers;

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
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        final ConversationScenarioState state = conversation.getConversationScenarioState();
        final Bindings bindings = state.getBindings();
        QueuedCallStatus callStatus = (QueuedCallStatus) state.getBindings().get(
                QUEUED_CALL_STATUS_BINDING);
        if (callStatus==null){
            DataContext context = (DataContext) state.getBindings().get(BindingNames.DATA_CONTEXT_BINDING);
            if (context==null)
                context = requestSender.createDataContext();
            callStatus = new CallQueueRequestImpl(
                    conversation, priority, queueId, operatorPhoneNumbers
                    , continueConversationOnReadyToCommutate
                    , continueConversationOnReject, context);
            state.setBinding(QUEUED_CALL_STATUS_BINDING, callStatus, BindingScope.POINT);
            requestSender.sendCallQueueRequest(callStatus, context);
        } else if (callStatus.isReadyToCommutate() || callStatus.isCommutated()) {
            if (callStatus.isReadyToCommutate()) {
                if (playOperatorGreeting){
                    if (logger.isDebugEnabled())
                        logger.debug("Playing operator greeting");
                    AudioFile greeting = callStatus.getOperatorGreeting();
                    if (greeting!=null)
                        IvrUtils.playAudioInAction(this, conversation, greeting);
                }
                if (logger.isDebugEnabled())
                    logger.debug("Operator and abonent are ready to commutate. Commutating...");
                //disable audio stream reset??? Then when t to enable???
                bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
                callStatus.replayToReadyToCommutate();
                do {
                    TimeUnit.MILLISECONDS.sleep(10);
                } while (!callStatus.isCommutated() && !hasCancelRequest() && !callStatus.isDisconnected());
                if (hasCancelRequest()) return;
            }
            if (logger.isDebugEnabled())
                logger.debug("Abonent and operator were commutated. Waiting for disconnected event...");
            state.disableDtmfProcessing();
            try {
                do {
                    TimeUnit.MILLISECONDS.sleep(10);
                } while (!callStatus.isDisconnected() && !hasCancelRequest());
            } finally {
                state.enableDtmfProcessing();
                bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            }            
            if (logger.isDebugEnabled())
                logger.debug("Commutation disconnected");
        }
    }

    @Override
    public boolean isFlowControlAction() {
        return false;
    }
}
