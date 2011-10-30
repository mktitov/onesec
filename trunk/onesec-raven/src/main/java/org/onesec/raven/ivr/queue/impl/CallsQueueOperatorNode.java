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
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
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
    private AtomicReference<CallsCommutationManagerImpl> commutationManager;

    @Override
    protected void initFields() {
        super.initFields();
        busy = new AtomicBoolean(false);
        commutationManager = new AtomicReference<CallsCommutationManagerImpl>();
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    protected boolean doProcessRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting)
    {
        if (!busy.compareAndSet(false, true)) {
            onBusyRequests.incrementAndGet();
            return false;
        }
        commutationManager.set(commutate(queue, request, phoneNumbers, conversationScenario, greeting));
        return true;
    }

    @Override
    protected void doRequestProcessed(CallsCommutationManagerImpl manager, boolean callHandled) {
        if (commutationManager.compareAndSet(manager, null))
            busy.set(false);
    }
    
    /**
     * for test purposes
     */
    CallsCommutationManagerImpl getCommutationManager(){
        return commutationManager.get();
    }
}
