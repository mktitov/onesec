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
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.impl.CallQueueRequestImpl;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;

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

    public QueueCallAction(CallQueueRequestSender requestSender
            , boolean continueConversationOnReadyToCommutate, boolean continueConversationOnReject
            , int priority, String queueId, boolean playOperatorGreeting)
    {
        super(ACTION_NAME);
        this.continueConversationOnReadyToCommutate = continueConversationOnReadyToCommutate;
        this.continueConversationOnReject = continueConversationOnReject;
        this.requestSender = requestSender;
        this.priority = priority;
        this.queueId = queueId;
        this.playOperatorGreeting = playOperatorGreeting;
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
        ConversationScenarioState state = conversation.getConversationScenarioState();
        QueuedCallStatus callStatus = (QueuedCallStatus) state.getBindings().get(
                QUEUED_CALL_STATUS_BINDING);
        if (callStatus==null){
            DataContext context = requestSender.createDataContext();
            callStatus = new CallQueueRequestImpl(
                    conversation, priority, queueId, continueConversationOnReadyToCommutate
                    , continueConversationOnReject, context);
            state.setBinding(QUEUED_CALL_STATUS_BINDING, callStatus, BindingScope.POINT);
            requestSender.sendCallQueueRequest(callStatus, context);
        } else if (callStatus.isReadyToCommutate() || callStatus.isCommutated()) {
            if (callStatus.isReadyToCommutate()) {
                if (playOperatorGreeting){
                    if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                        conversation.getOwner().getLogger().debug(logMess("Playing operator greeting"));
                    AudioFile greeting = callStatus.getOperatorGreeting();
                    if (greeting!=null)
                        IvrUtils.playAudioInAction(this, conversation, greeting);
                }
                if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                    conversation.getOwner().getLogger().debug(logMess(
                            "Operator and abonent are ready to commutate. Commutating..."));
                callStatus.replayToReadyToCommutate();
                do {
                    TimeUnit.MILLISECONDS.sleep(10);
                } while (!callStatus.isCommutated() && !hasCancelRequest());
                if (hasCancelRequest()) return;
            }
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                conversation.getOwner().getLogger().debug(logMess(
                        "Abonent and operator were commutated. Waiting for disconnected event..."));
            state.disableDtmfProcessing();
            try {
                do {
                    TimeUnit.MILLISECONDS.sleep(10);
                } while (!callStatus.isDisconnected() && !hasCancelRequest());
            } finally {
                state.enableDtmfProcessing();
            }
            
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                conversation.getOwner().getLogger().debug(logMess(
                        "Commutation disconnected"));
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
