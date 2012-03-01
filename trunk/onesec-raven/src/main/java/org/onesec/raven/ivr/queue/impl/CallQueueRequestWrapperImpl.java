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
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.raven.ds.DataContext;
import org.onesec.raven.ivr.queue.CallsQueue;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.RequestWrapperListener;
import org.onesec.raven.ivr.queue.event.*;
import org.onesec.raven.ivr.queue.event.impl.*;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;

import static org.onesec.raven.ivr.queue.impl.CallQueueCdrRecordSchemaNode.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestWrapperImpl implements CallQueueRequestWrapper
{
    private final CallQueueRequest request;
    private final CallsQueuesNode owner;
    private final Listener listener;
    private final long requestId;
    private final AtomicBoolean cdrSent = new AtomicBoolean(false);
    private final Set<RequestWrapperListener> listeners = new HashSet<RequestWrapperListener>();

    private int priority;
    private String queueId;
    private Integer positionInQueue;
    private CallsQueue queue;
    private CallsQueue targetQueue;
    private StringBuilder log;
    private Record cdr;
    private int onBusyBehaviourStep;
    private CallsQueueOnBusyBehaviour onBusyBehaviour;
    private AtomicBoolean valid;
    private AtomicBoolean handlingByOperator = new AtomicBoolean();
    private int operatorIndex;
    private int operatorHops;
    private long lastQueuedTime;
    private boolean forceResetCallsQueueFlag = false;

    public CallQueueRequestWrapperImpl(
            CallsQueuesNode owner, CallQueueRequest request, long requestId)
        throws RecordException
    {
        this.owner = owner;
        this.request = request;
        this.requestId = requestId;
        this.queue = null;
        this.positionInQueue = 0;
        this.onBusyBehaviourStep = 0;
        this.valid = new AtomicBoolean(true);
        this.operatorIndex = -1;
        this.operatorHops = 0;
        listener = new Listener(request.getConversation(), request);
        request.getConversation().addConversationListener(listener);
        RecordSchemaNode schema = owner.getCdrRecordSchema();
        if (schema!=null) {
            cdr = schema.createRecord();
            cdr.setValue(CALLING_NUMBER, request.getConversation().getCallingNumber());
            cdr.setValue(QUEUED_TIME, getTimestamp());
            cdr.setValue(PRIORITY, request.getPriority());
        }
    }

    public void addRequestListener(CallQueueRequestListener listener) { }

    public void addRequestWrapperListener(RequestWrapperListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public boolean isValid() {
        return valid.get() && !request.isCanceled();
    }

    public boolean isHandlingByOperator() {
        return handlingByOperator.get();
    }

    public boolean isCanceled() {
        return request.isCanceled();
    }
    
    private void invalidate() {
        if (valid.compareAndSet(true, false)){
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Conversation stopped by abonent"));
            addToLog("conversation stopped by abonent");
            fireDisconnectedQueueEvent();
            fireRequestInvalidated();
        }
    }

    public int getOperatorIndex() {
        return operatorIndex;
    }

    public void setOperatorIndex(int index) {
        this.operatorIndex = index;
        if (index==-1)
            operatorHops = 0;
    }

    public void incOperatorHops() {
        operatorHops++;
    }

    public int getOperatorHops() {
        return operatorHops;
    }

    public CallsQueueOnBusyBehaviour getOnBusyBehaviour() {
        return onBusyBehaviour;
    }

    public void setOnBusyBehaviour(CallsQueueOnBusyBehaviour onBusyBehaviour) {
        this.onBusyBehaviour = onBusyBehaviour;
        this.onBusyBehaviourStep = 0;
    }

    public int getOnBusyBehaviourStep() {
        return onBusyBehaviourStep;
    }

    public void setOnBusyBehaviourStep(int onBusyBehaviourStep) {
        this.onBusyBehaviourStep = onBusyBehaviourStep;
    }

    public DataContext getContext() {
        return request.getContext();
    }

    public CallQueueRequest getWrappedRequest() {
        return request;
    }

    public IvrEndpointConversation getConversation() {
        return request.getConversation();
    }

    public Integer getPositionInQueue() {
        return positionInQueue;
    }

    public void setPositionInQueue(Integer positionInQueue) 
    {
        if (this.positionInQueue!=positionInQueue)
            callQueueChangeEvent(new NumberChangedQueueEventImpl(
                    queue, requestId, this.positionInQueue, positionInQueue));
        this.positionInQueue = positionInQueue;
    }
    
    public int getPriority() {
        return request.getPriority();
    }

    public void setPriority(int priority) {
        request.setPriority(priority);
    }

    public String getQueueId() {
        return request.getQueueId();
    }

    public void setQueueId(String queueId) {
        request.setQueueId(queueId);
    }

    public String getOperatorPhoneNumbers() {
        return request.getOperatorPhoneNumbers();
    }

    public void setOperatorPhoneNumbers(String phoneNumbers) {
        request.setOperatorPhoneNumbers(phoneNumbers);
    }

    public long getRequestId() {
        return requestId;
    }

    public CallsQueue getCallsQueue() {
        return queue;
    }

    public long getLastQueuedTime() {
        return lastQueuedTime;
    }

    public void setForceResetCallsQueueFlag() {
        forceResetCallsQueueFlag = true;
    }

    public CallsQueue getTargetQueue() {
        return targetQueue;
    }

    public void setCallsQueue(CallsQueue queue)
    {
        if (queue!=this.queue || forceResetCallsQueueFlag){
//            requestId=0;
            operatorIndex=-1;
            operatorHops = 0;
            lastQueuedTime = System.currentTimeMillis();
            forceResetCallsQueueFlag = false;
            if (targetQueue==null)
                targetQueue = queue;
            addToLog(String.format("moved to queue (%s)", queue.getName()));
        }
        this.queue = queue;
    }

    public void callQueueChangeEvent(CallQueueEvent event)
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Received event: %s", event.getClass().getName()));
        try{
            if (cdr!=null){
                if        (event instanceof DisconnectedQueueEvent) {
                    if (cdr.getValue(DISCONNECTED_TIME)==null) {
                        Timestamp ts = getTimestamp();
                        cdr.setValue(DISCONNECTED_TIME, ts);
                        if (cdr.getValue(COMMUTATED_TIME)!=null){
                            Timestamp startTs = (Timestamp)cdr.getValue(COMMUTATED_TIME);
                            long dur = (ts.getTime()-startTs.getTime())/1000;
                            cdr.setValue(CONVERSATION_DURATION, dur);
                        }
                        sendCdrToConsumers();
                    }
                } else if (event instanceof RejectedQueueEvent) {
                    cdr.setValue(REJECTED_TIME, getTimestamp());
                    sendCdrToConsumers();
                } else if (event instanceof CallQueuedEvent) {
                    if (cdr.getValue(QUEUED_TIME)==null)
                        cdr.setValue(QUEUED_TIME, getTimestamp());
                    if (cdr.getValue(TARGET_QUEUE)==null)
                        cdr.setValue(TARGET_QUEUE, ((CallQueuedEvent)event).getQueueId());
                    cdr.setValue(HANDLED_BY_QUEUE, ((CallQueuedEvent)event).getQueueId());
                } else if (event instanceof NumberChangedQueueEvent) {
                    addToLog("#"+((NumberChangedQueueEvent)event).getCurrentNumber());
                } else if (event instanceof ReadyToCommutateQueueEvent) {
                    cdr.setValue(READY_TO_COMMUTATE_TIME, getTimestamp());
                } else if (event instanceof CommutatedQueueEvent) {
                    cdr.setValue(COMMUTATED_TIME, getTimestamp());
                    cdr.setValue(CONVERSATION_START_TIME, getTimestamp());
                } else if (event instanceof OperatorQueueEvent) {
                    cdr.setValue(OPERATOR_ID, ((OperatorQueueEvent)event).getOperatorId());
                } else if (event instanceof OperatorNumberQueueEvent) {
                    cdr.setValue(OPERATOR_NUMBER, ((OperatorNumberQueueEvent)event).getOperatorNumber());
                } else if (event instanceof CallTransferedQueueEvent) {
                    cdr.setValue(TRANSFERED, 'T');
                    CallTransferedQueueEvent transferEvent = (CallTransferedQueueEvent) event;
                    cdr.setValue(OPERATOR_ID, transferEvent.getOperatorId());
                    cdr.setValue(OPERATOR_NUMBER, transferEvent.getOperatorNumber());
                    addToLog(String.format("transfered to op. (%s) number (%s)"
                            , transferEvent.getOperatorId(), transferEvent.getOperatorNumber()));

                }
            }
        }catch(Throwable e){
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(owner.logMess(request, "Error setting value to cdr"), e);
        }
        request.callQueueChangeEvent(event);
    }

    public void addToLog(String message)
    {
        String time = new SimpleDateFormat("hh:mm:ss").format(new Date());
        if (log==null)
            log = new StringBuilder(time);
        else
            log.append("; ").append(time);
        log.append(" ").append(message);
    }

    private void setCdrValue(String fieldName, Object value)
    {
        try {
            if (cdr != null) 
                cdr.setValue(fieldName, value);
        } catch (RecordException e) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(
                        String.format("Can't set value (%s) for CDR record field (%s)", fieldName, value)
                        , e);
        }
    }

    private Timestamp getTimestamp(){
        return new Timestamp(System.currentTimeMillis());
    }
    
    public void fireRejectedQueueEvent(){
        callQueueChangeEvent(new RejectedQueueEventImpl(queue, requestId));
    }

    public void fireCallQueuedEvent() {
        callQueueChangeEvent(new CallQueuedEventImpl(queue, requestId, queue.getName()));
    }
    
    public void fireDisconnectedQueueEvent(){
        callQueueChangeEvent(new DisconnectedQueueEventImpl(queue, requestId));
    }

    public boolean fireReadyToCommutateQueueEvent(CommutationManagerCall operator) {
        if (handlingByOperator.compareAndSet(false, true)) {
            callQueueChangeEvent(new ReadyToCommutateQueueEventImpl(queue, requestId, operator));
            fireRequestProcessingByOperator(operator);
            return true;
        } 
        return false;
    }

    public void fireCommutatedEvent() {
        callQueueChangeEvent(new CommutatedQueueEventImpl(queue, requestId));
    }

    public void fireOperatorQueueEvent(String operatorId) {
        callQueueChangeEvent(new OperatorQueueEventImpl(queue, requestId, operatorId));
    }

    public void fireOperatorNumberQueueEvent(String operatorNumber) {
        callQueueChangeEvent(new OperatorNumberQueueEventImpl(queue, requestId, operatorNumber));
    }

    public void fireCallTransfered(String operatorId, String operatorNumber) {
        callQueueChangeEvent(new CallTransferedQueueEventImpl(queue, requestId, operatorId, operatorNumber));
    }

    public void fireOperatorGreetingQueueEvent(AudioFile greeting) {
        callQueueChangeEvent(new OperatorGreetingQueueEventImpl(queue, requestId, greeting));
    }
    
    private void sendCdrToConsumers() throws RecordException {
        if (!cdrSent.compareAndSet(false, true))
            return;
        cdr.setValue(LOG, log.toString());
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Sending CDR to consumers"));
        DataSourceHelper.sendDataToConsumers(owner, cdr, request.getContext());
    }

    @Override
    public String toString() 
    {
        return getConversation().getObjectName();
    }

    public String logMess(String message, Object... args)
    {
        return request.getConversation().toString()
                +" [reqId: "+requestId+(queue==null?"":"; queue: "+queue.getName())+"]. "
                +String.format(message, args);
    }

    private void fireRequestInvalidated() {
        synchronized(listeners) {
            for (RequestWrapperListener listener: listeners) 
                listener.requestInvalidated();
        }
    }

    private void fireRequestProcessingByOperator(CommutationManagerCall operator) {
        synchronized(listeners) {
            for (RequestWrapperListener listener: listeners) 
                listener.processingByOperator(operator);
        }
    }
    
    private class Listener implements IvrEndpointConversationListener, CallQueueRequestListener
    {
        private final IvrEndpointConversation conversation;
        private final CallQueueRequest request;

        public Listener(IvrEndpointConversation conversation, CallQueueRequest request) {
            this.conversation = conversation;
            this.request = request;
            conversation.addConversationListener(this);
            request.addRequestListener(this);
            if (request.isCanceled())
                invalidate();
        }

        public void requestCanceled() {
            invalidate();
        }

        public void listenerAdded(IvrEndpointConversationEvent event) {
            if (conversation.getState().getId()==IvrEndpointConversationState.INVALID)
                invalidate();
        }

        public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }

        public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }
        
        public void conversationStarted(IvrEndpointConversationEvent event) { }

        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            invalidate();
        }

        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
            invalidate();
        }
    }
}
