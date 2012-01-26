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

import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.annotations.NodeClass;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
//TODO: tests and icon
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueVirtualOperatorNode extends AbstractOperatorNode {

    @Override
    protected boolean doProcessRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers)
    {
        if (request.getOperatorPhoneNumbers()==null) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(request.logMess("Operator (%s), Can't process request because of "
                        + "it does not contains operator phone numbers", getName()));
            return false;
        }
        commutate(queue, request, request.getOperatorPhoneNumbers(), conversationScenario, greeting);
        return true;
    }

    @Override
    protected void doRequestProcessed(CallsCommutationManagerImpl commutationManager, boolean callHandled) {
    }
}
