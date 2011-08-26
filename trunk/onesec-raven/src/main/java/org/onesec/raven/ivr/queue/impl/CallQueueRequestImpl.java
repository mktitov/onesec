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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.queue.event.CommutatedQueueEvent;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.event.NumberChangedQueueEvent;
import org.onesec.raven.ivr.queue.event.ReadyToCommutateQueueEvent;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestImpl implements QueuedCallStatus
{

    private final IvrEndpointConversation conversation;
    private final boolean continueConversationOnReadyToCommutate;
    private int priority;
    private String queueId;
    private Status status;
    private int serialNumber;
    private int prevSerialNumber;
    private CallsCommutationManager commutationManager;

    public CallQueueRequestImpl(IvrEndpointConversation conversation, int priority, String queueId,
            boolean continueConversationOnReadyToCommutate)
    {
        this.conversation = conversation;
        this.priority = priority;
        this.queueId = queueId;
        this.continueConversationOnReadyToCommutate = continueConversationOnReadyToCommutate;
        this.status = Status.QUEUEING;
        this.serialNumber = -1;
        this.prevSerialNumber = -1;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public boolean isContinueConversationOnReadyToCommutate() {
        return continueConversationOnReadyToCommutate;
    }

    public IvrEndpointConversation getConversation() {
        return conversation;
    }

    public synchronized void callQueueChangeEvent(CallQueueEvent event)
    {
        if (event instanceof ReadyToCommutateQueueEvent) {
            status = Status.READY_TO_COMMUTATE;
            commutationManager = ((ReadyToCommutateQueueEvent)event).getCommutationManager();
            if (continueConversationOnReadyToCommutate)
                conversation.continueConversation('-');
        } else if (event instanceof CommutatedQueueEvent) {
            status = Status.COMMUTATED;
        } else if (event instanceof DisconnectedQueueEvent) {
            status = Status.DISCONNECTED;
        } else if (event instanceof NumberChangedQueueEvent) {
            prevSerialNumber = serialNumber;
            serialNumber = ((NumberChangedQueueEvent)event).getCurrentNumber();
        } else if (event instanceof RejectedQueueEvent) {
            status = Status.REJECTED;
        }
    }

    public synchronized void replayToReadyToCommutate()
    {
        status = Status.COMMUTATING;
        commutationManager.abonentReadyToCommutate(conversation);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public synchronized boolean isQueueing() {
        return status == Status.QUEUEING;
    }

    public synchronized boolean isReadyToCommutate() {
        return status==Status.READY_TO_COMMUTATE;
    }

    public synchronized boolean isCommutated() {
        return status==Status.COMMUTATED;
    }

    public synchronized boolean isDisconnected() {
        return status==Status.DISCONNECTED;
    }

    public synchronized int getSerialNumber() {
        return serialNumber;
    }

    public synchronized int getPrevSerialNumber() {
        return prevSerialNumber;
    }
}
