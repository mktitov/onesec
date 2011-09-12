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

package org.onesec.raven.ivr.queue;

import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface CallsQueueOperator extends Node
{
    /**
     * Returns the request currently processing by this operator or null
     */
    public CallQueueRequestWrapper getProcessingRequest();
    /**
     * Returns processed request count
     */
    public long getProcessedRequestCount();
    /**
     * Process call queue request
     * Returns true if operator (this object) taken request for processing. If method returns false
     * then operator is busy for now
     */
    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting);
}
