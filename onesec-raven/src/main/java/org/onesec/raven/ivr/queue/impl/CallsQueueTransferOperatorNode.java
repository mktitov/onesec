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

import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueTransferOperatorNode extends BaseNode implements CallsQueueOperator {
    
    public final static String NAME = "Transfer operator";
    
    private AtomicInteger totalRequests;
    private AtomicInteger handledRequests;
    private AtomicInteger onNoFreeEndpointsRequests;
    private AtomicInteger onNoAnswerRequests;

    public CallsQueueTransferOperatorNode() {
        super(NAME);
    }

    public void resetStat() {
        totalRequests.set(0);
        handledRequests.set(0);
        onNoAnswerRequests.set(0);
        onNoAnswerRequests.set(0);
    }

    @Override
    public void setName(String name) { }

    @Override
    protected void initFields() {
        super.initFields();
        totalRequests = new AtomicInteger();
        handledRequests = new AtomicInteger();
        onNoAnswerRequests = new AtomicInteger();
        onNoFreeEndpointsRequests = new AtomicInteger();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        resetStat();
    }

    public boolean isActive() {
        return true;
    }
    
    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getHandledRequests() {
        return handledRequests.get();
    }

    public int getOnBusyRequests() {
        return 0;
    }

    public int getOnNoFreeEndpointsRequests() {
        return onNoFreeEndpointsRequests.get();
    }

    public int getOnNoAnswerRequests() {
        return onNoAnswerRequests.get();
    }

    public int getOnNotStartedRequests() {
        return 0;
    }

    public String getPersonDesc() {
        return null;
    }

    public String getPersonId() {
        return null;
    }

    public boolean processRequest(CallsQueue queue, CallQueueRequestController request, 
            IvrConversationScenario conversationScenario, AudioFile greeting, String operatorPhoneNumbers, 
            Integer inviteTimeout) 
    {
        if (isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("Transfer operator must not be used as normal operators");
        return false;
    }

    public boolean resetBusyTimer() {
        return false;
    }

    @Override
    public String translateAbonentNumber(String abonentNumber, String operatorNumber) {
        return abonentNumber;
    }

    public CallsQueueOperator callTransferedFromOperator(String phoneNumber, CallsCommutationManager commutationManager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean callTransferedToOperator(CallsCommutationManager commutationManager) {
        totalRequests.incrementAndGet();
        return true;
    }

    public void requestProcessed(CallsCommutationManager commutationManager, boolean callHandled) {
        if (callHandled)
            handledRequests.incrementAndGet();
        else
            onNoAnswerRequests.incrementAndGet();
    }

    public void incOnNoFreeEndpointsRequests() {
        onNoFreeEndpointsRequests.incrementAndGet();
    }
}
