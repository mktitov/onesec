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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueOperatorNode extends AbstractOperatorNode {
    @NotNull @Parameter
    private String phoneNumbers;

    private AtomicBoolean busy;
    private AtomicReference<CallsCommutationManager> commutationManager;
    private AtomicReference<String> request;

    @Override
    protected void initFields() {
        super.initFields();
        busy = new AtomicBoolean(false);
        request = new AtomicReference<String>();
        commutationManager = new AtomicReference<CallsCommutationManager>();
    }

    @Override
    protected void doStart() throws Exception {
        busy.set(false);
        super.doStart();
    }
    
    @Parameter(readOnly=true)
    public Boolean getBusy() {
        return busy.get();
    }
    
    @Parameter(readOnly=true)
    public String getProcessingRequest(){
        return request.get();
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    protected boolean doProcessRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers)
    {
        if (!busy.compareAndSet(false, true)) {
            onBusyRequests.incrementAndGet();
            return false;
        }
        try {
            this.request.set(request.toString());
            String _nums = operatorPhoneNumbers==null||operatorPhoneNumbers.trim().length()==0?
                    phoneNumbers : operatorPhoneNumbers;
            commutationManager.set(commutate(queue, request, _nums, conversationScenario, greeting));
            return true;
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(request.logMess("Error handling request by operator"), e);
            busy.set(false);
            this.request.set(null);
            return false;
        }
    }

    @Override
    protected void doRequestProcessed(CallsCommutationManager manager, boolean callHandled) {
        if (commutationManager.compareAndSet(manager, null)) {
            busy.set(false);
            request.set(null);
        }
    }

    public CallsQueueOperator callTransferedFromOperator(String phoneNumber
            , CallsCommutationManager manager) 
    {
        doRequestProcessed(commutationManager.get(), false);
        return getCallsQueues().processCallTransferedEvent(phoneNumber, manager);
    }

    public boolean callTransferedToOperator(CallsCommutationManager manager) {
        if (busy.compareAndSet(false, true)) {
            this.commutationManager.set(manager);
            return true;
        } else
            return false;
    }
    
    /**
     * for test purposes
     */
    CallsCommutationManager getCommutationManager(){
        return commutationManager.get();
    }
}