/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueTransferOperatorNode extends AbstractOperatorNode {
    
    public final static String NAME = "Transfer operator";

    public CallsQueueTransferOperatorNode() {
        super(NAME);
    }

    @Override
    public void setName(String name) {
    }

    @Override
    protected boolean doProcessRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting, String operatorPhoneNumbers) 
    {
        if (isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("{} must not be used as normal operator", NAME);
        return false;
    }

    @Override
    protected void doRequestProcessed(CallsCommutationManager commutationManager, boolean callHandled) {
    }

    public CallsQueueOperator callTransferedFromOperator(String phoneNumber
            , CallsCommutationManager commutationManager) 
    {
        return getCallsQueues().processCallTransferedEvent(phoneNumber);
    }

    public boolean callTransferedToOperator(CallsCommutationManager commutationManager) {
        return true;
    }
}
