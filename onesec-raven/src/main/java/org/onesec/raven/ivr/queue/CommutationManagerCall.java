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
public interface CommutationManagerCall
{
    public final static String CALLS_COMMUTATION_MANAGER_BINDING = "queueCommutationManager";
    public final static String CALL_QUEUE_REQUEST_BINDING = "queueRequest";

    public enum State {INIT, NO_FREE_ENDPOINTS, INVITING, OPERATOR_READY, ABONENT_READY, 
        COMMUTATED, CONVERSATION_STARTED, HANDLED, INVALID}
    /**
     * Returns the operator number (the B number)
     */
    public String getOperatorNumber();
//    /**
//     * Returns the operator's display number. The phone number that abonent will see at phone display (B number)
//     */
//    public String getOperatorDisplayNumber();
    /**
     * Returns operator's conversation
     */
    public IvrEndpointConversation getOperatorConversation();
    /**
     * Make a logical transfer to operator. If in queue exists operator with passed operatorNumber then
     * call attached to this operator else call attached to transfer operator
     * @param operatorNumber 
     */
    public void transferToOperator(String operatorNumber);
    /**
     * Returns the operator associated with a call
     */
    public CallsQueueOperator getOperator();
    /**
     * Commutates operator with abonent
     */
    public void commutate();
    /**
     * Cancel processing request. 
     */
    public void cancel();
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
    /**
     * Returns <b>true</b> while the state of the communication between two calls is valid
     */ 
    public boolean isCommutationValid();
    /**
     * Adds listener for commutation manager events
     */
    public void addListener(CallsCommutationManagerListener listener);
    /**
     * Removes listener of the commutation manager events
     */
    public void removeListener(CallsCommutationManagerListener listener);
}
