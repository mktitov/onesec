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

import org.raven.ds.DataContext;
import org.onesec.raven.ivr.queue.CallsQueue;
import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.DataContextImpl;
import org.raven.tree.Node;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.event.CallQueuedEvent;
import org.onesec.raven.ivr.queue.event.CommutatedQueueEvent;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.event.NumberChangedQueueEvent;
import org.onesec.raven.ivr.queue.event.ReadyToCommutateQueueEvent;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
import org.onesec.raven.ivr.queue.event.impl.CallQueueEventImpl;
import org.onesec.raven.ivr.queue.event.impl.NumberChangedQueueEventImpl;
import org.onesec.raven.ivr.queue.event.impl.RejectedQueueEventImpl;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
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
    private final DataContext context;

    private int priority;
    private String queueId;
    private long requestId;
    private Integer positionInQueue;
    private CallsQueue queue;
    private StringBuilder log;
    private Record cdr;
    private int onBusyBehaviourStep;
    private CallsQueueOnBusyBehaviour onBusyBehaviour;

    public CallQueueRequestWrapperImpl(
            CallsQueuesNode owner, CallQueueRequest request, DataContext context)
        throws RecordException
    {
        this.owner = owner;
        this.request = request;
        this.context = context;
        this.requestId = 0;
        this.queue = null;
        this.positionInQueue = 0;
        this.onBusyBehaviourStep = 0;
        RecordSchemaNode schema = owner.getCdrRecordSchema();
        if (schema!=null) {
            cdr = schema.createRecord();
            cdr.setValue(CALLING_NUMBER, request.getConversation().getCallingNumber());
        }
    }

    public CallsQueueOnBusyBehaviour getOnBusyBehaviour() {
        return onBusyBehaviour;
    }

    public void setOnBusyBehaviour(CallsQueueOnBusyBehaviour onBusyBehaviour) {
        this.onBusyBehaviour = onBusyBehaviour;
    }

    public int getOnBusyBehaviourStep() {
        return onBusyBehaviourStep;
    }

    public void setOnBusyBehaviourStep(int onBusyBehaviourStep) {
        this.onBusyBehaviourStep = onBusyBehaviourStep;
    }

    public DataContext getContext() {
        return context;
    }

    public CallQueueRequest getWrappedRequest() {
        return request;
    }

    public IvrEndpointConversation getConversation()
    {
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

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public CallsQueue getCallsQueue() {
        return queue;
    }

    public void setCallsQueue(CallsQueue queue) {
        this.queue = queue;
    }

    public void callQueueChangeEvent(CallQueueEvent event)
    {
        try{
            if (cdr!=null){
                if        (event instanceof DisconnectedQueueEvent) {
                    cdr.setValue(DISCONNECTED_TIME, getTimestamp());
                    cdr.setValue(LOG, log.toString());
                    sendCdrToConsumers();
                } else if (event instanceof RejectedQueueEvent) {
                    cdr.setValue(REJECETED_TIME, getTimestamp());
                    cdr.setValue(LOG, log.toString());
                    sendCdrToConsumers();
                } else if (event instanceof CallQueuedEvent) {
                    cdr.setValue(QUEUED_TIME, getTimestamp());
                    cdr.setValue(CALLING_NUMBER, ((CallQueuedEvent)event).getQueueId());
                } else if (event instanceof NumberChangedQueueEvent) {
                    addToLog("#"+((NumberChangedQueueEvent)event).getCurrentNumber());
                } else if (event instanceof ReadyToCommutateQueueEvent) {
                    cdr.setValue(READY_TO_COMMUTATE_TIME, getTimestamp());
                } else if (event instanceof CommutatedQueueEvent) {
                    cdr.setValue(COMMUTATED_TIME, getTimestamp());
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

    private Timestamp getTimestamp(){
        return new Timestamp(System.currentTimeMillis());
    }
    
    public void fireRejectedQueueEvent(){
        callQueueChangeEvent(new RejectedQueueEventImpl(queue, requestId));
    }

    public void fireCallQueuedEvent() {
        callQueueChangeEvent(new CallQueueEventImpl(queue, requestId));
    }
    
    private void sendCdrToConsumers()
    {
        Collection<Node> deps = owner.getDependentNodes();
        if (deps!=null && !deps.isEmpty()) {
            for (Node dep: deps)
                if (dep instanceof DataConsumer && Node.Status.STARTED.equals(dep.getStatus()))
                    ((DataConsumer)dep).setData(owner, cdr, context);
        }
    }

    @Override
    public String toString() 
    {
        return getConversation().getObjectName();
    }
}
