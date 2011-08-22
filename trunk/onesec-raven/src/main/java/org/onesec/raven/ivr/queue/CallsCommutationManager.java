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

import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public interface CallsCommutationManager
{
    public final static String CALL_QUEUE_OPERATOR_BINDING = "queueOperator";
    public final static String CALL_QUEUE_REQUEST_BINDING = "queueRequest";
    
    /**
     * Informs about that the operator's conversation is ready to commutation
     * @param operatorConversation the operator conversation
     */
    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation);
    /**
     * Informs about that the abonent conversation is ready to commutation
     * @param operatorConversation the operator conversation
     */
    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation);
}
