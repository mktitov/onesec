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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.queue.event.CallQueuedEvent;
import org.onesec.raven.ivr.queue.event.CommutatedQueueEvent;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.event.NumberChangedQueueEvent;
import org.onesec.raven.ivr.queue.event.OperatorGreetingQueueEvent;
import org.onesec.raven.ivr.queue.event.ReadyToCommutateQueueEvent;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestImpl implements QueuedCallStatus
{
    private final IvrEndpointConversation conversation;
    private final boolean continueConversationOnReadyToCommutate;
    private final boolean continueConversationOnReject;
    private int priority;
    private String queueId;
    private String operatorPhoneNumbers;
    private Status status;
    private int serialNumber;
    private int prevSerialNumber;
    private CommutationManagerCall commutationManager;
    private AudioFile operatorGreeting;
    private final AtomicBoolean canceledFlag = new AtomicBoolean(false);
    private final List<CallQueueRequestListener> listeners = new LinkedList<>();
    private final DataContext context;
    private final AtomicReference<CallsQueue> lastQueue;
    private final AtomicLong lastQueuedTime; 

    public CallQueueRequestImpl(IvrEndpointConversation conversation, int priority, String queueId,
            String operatorPhoneNumbers,
            boolean continueConversationOnReadyToCommutate, boolean continueConversationOnReject,
            DataContext context)
    {
        this.conversation = conversation;
        this.priority = priority;
        this.queueId = queueId;
        this.continueConversationOnReadyToCommutate = continueConversationOnReadyToCommutate;
        this.continueConversationOnReject = continueConversationOnReject;
        this.status = Status.QUEUEING;
        this.serialNumber = -1;
        this.prevSerialNumber = -1;
        this.context = context;
        this.operatorPhoneNumbers = operatorPhoneNumbers;
        this.lastQueue = new AtomicReference<>();
        this.lastQueuedTime = new AtomicLong();
    }

    @Override
    public void addRequestListener(CallQueueRequestListener listener) {
        listeners.add(listener);
        listener.conversationAssigned(conversation);
        if (canceledFlag.get())
            listener.requestCanceled("CANCELED");
    }

    @Override
    public synchronized Status getStatus() {
        return status;
    }

    public boolean isContinueConversationOnReadyToCommutate() {
        return continueConversationOnReadyToCommutate;
    }

    public boolean isContinueConversationOnReject() {
        return continueConversationOnReject;
    }

    @Override
    public IvrEndpointConversation getConversation() {
        return conversation;
    }

    @Override
    public String getConversationInfo() {
        return conversation.getObjectName();
    }

    @Override
    public long getLastQueuedTime() {
        return lastQueuedTime.get();
    }

    @Override
    public CallsQueue getLastQueue() {
        return lastQueue.get();
    }

    @Override
    public DataContext getContext() {
        return context;
    }

    @Override
    public void callQueueChangeEvent(CallQueueEvent event)
    {
        boolean continueConversation = false;
        synchronized(this){
            if (event instanceof CallQueuedEvent) {
                CallsQueue eventQueue = ((CallQueuedEvent)event).getCallsQueue();
                if (eventQueue!=lastQueue.get()) {
                    lastQueue.set(eventQueue);
                    lastQueuedTime.set(System.currentTimeMillis());
                }
            } else if (event instanceof ReadyToCommutateQueueEvent) {
                status = Status.READY_TO_COMMUTATE;
                commutationManager = ((ReadyToCommutateQueueEvent)event).getCommutationManager();
                if (continueConversationOnReadyToCommutate)
                    continueConversation = true;
            } else if (event instanceof CommutatedQueueEvent) {
                status = Status.COMMUTATED;
            } else if (event instanceof DisconnectedQueueEvent) {
                status = Status.DISCONNECTED;
            } else if (event instanceof NumberChangedQueueEvent) {
                prevSerialNumber = serialNumber;
                serialNumber = ((NumberChangedQueueEvent)event).getCurrentNumber();
            } else if (event instanceof RejectedQueueEvent) {
                status = Status.REJECTED;
                if (continueConversationOnReject)
                    continueConversation = true;
            } else if (event instanceof OperatorGreetingQueueEvent)
                operatorGreeting = ((OperatorGreetingQueueEvent)event).getOperatorGreeting();
        }
        if (continueConversation)
            conversation.continueConversation('-');
    }

    @Override
    public synchronized void replayToReadyToCommutate() {
        if (status==Status.DISCONNECTED || status==Status.REJECTED)
            return;
        status = Status.COMMUTATING;
        commutationManager.abonentReadyToCommutate(conversation);
    }

    @Override
    public void cancel() {
        if (canceledFlag.compareAndSet(false, true))
            fireRequestCanceled("CANCELED");
    }
    
    private void fireRequestCanceled(String cause) {
        for (CallQueueRequestListener listener: listeners)
            listener.requestCanceled(cause);
    }

    @Override
    public boolean isCanceled() {
        return canceledFlag.get();
    }

    @Override
    public synchronized int getPriority() {
        return priority;
    }

    @Override
    public synchronized void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public synchronized String getQueueId() {
        return queueId;
    }

    @Override
    public synchronized void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    @Override
    public String getOperatorPhoneNumbers() {
        return operatorPhoneNumbers;
    }

    @Override
    public void setOperatorPhoneNumbers(String operatorPhoneNumbers) {
        this.operatorPhoneNumbers = operatorPhoneNumbers;
    }

    @Override
    public synchronized boolean isQueueing() {
        return status == Status.QUEUEING;
    }

    @Override
    public synchronized boolean isReadyToCommutate() {
        return status==Status.READY_TO_COMMUTATE;
    }

    @Override
    public synchronized boolean isCommutated() {
        return status==Status.COMMUTATED;
    }

    @Override
    public synchronized boolean isDisconnected() {
        return status==Status.DISCONNECTED;
    }

    @Override
    public synchronized int getSerialNumber() {
        return serialNumber;
    }

    @Override
    public synchronized int getPrevSerialNumber() {
        return prevSerialNumber;
    }

    @Override
    public synchronized AudioFile getOperatorGreeting() {
        return operatorGreeting;
    }
}
