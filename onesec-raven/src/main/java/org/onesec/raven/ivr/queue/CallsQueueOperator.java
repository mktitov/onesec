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
public interface CallsQueueOperator extends Node, StatisticCollector {
    /**
     * Returns the total requests count received by operator
     */
    public int getTotalRequests();
    /**
     * Returns the count of the requests handled by operator
     */
    public int getHandledRequests();
    /**
     * Returns the requests count that operator received when it was busy
     */
    public int getOnBusyRequests();
    /**
     * Returns the requests count that operator rejected because of no free endpoints in the pool
     */
    public int getOnNoFreeEndpointsRequests();
    /**
     * Returns the number of requests that were returned back to the queue because of operator not picked up
     */
    public int getOnNoAnswerRequests();
    /**
     * Returns the number of requests that where returned back to the queue because of operator's not in the
     * not started state
     */
    public int getOnNotStartedRequests();
    /**
     * Process call queue request
     * Returns true if operator (this object) taken request for processing. If method returns false
     * then operator is busy for now
     */
    public boolean processRequest(CallsQueue queue, CallQueueRequestController request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers, Integer inviteTimeout);
    public boolean resetBusyTimer();
    public CallsQueueOperator callTransferedFromOperator(String phoneNumber, CallsCommutationManager commutationManager);
    public boolean callTransferedToOperator(CallsCommutationManager commutationManager);
    /**
     * Using this method commutation manager informs operator that request were processed
     */
    public void requestProcessed(CallsCommutationManager commutationManager, boolean callHandled);
    public void incOnNoFreeEndpointsRequests();
    public String getPersonId();
    public String getPersonDesc();
    public boolean isActive();
    public String translateAbonentNumber(String abonentNumber, String operatorNumber);
}
