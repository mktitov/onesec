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

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.DataContextImpl;
import org.raven.tree.Node;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CallQueueEvent;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CommutatedQueueEvent;
import org.onesec.raven.ivr.queue.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.NumberChangedQueueEvent;
import org.onesec.raven.ivr.queue.ReadyToCommutateQueueEvent;
import org.onesec.raven.ivr.queue.RejectedQueueEvent;
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

    private StringBuilder log;
    private Record cdr;

    public CallQueueRequestWrapperImpl(CallsQueuesNode owner, CallQueueRequest request)
            throws RecordException
    {
        this.owner = owner;
        this.request = request;
        RecordSchemaNode schema = owner.getCdrRecordSchema();
        if (schema!=null) {
            cdr = schema.createRecord();
            cdr.setValue(QUEUED_TIME, getTimestamp());
        }
    }

    public IvrEndpointConversation getConversation()
    {
        return request.getConversation();
    }

    public void callQueueChangeEvent(CallQueueEvent event)
    {
        try{
            if        (event instanceof DisconnectedQueueEvent) {
                if (cdr!=null){
                    cdr.setValue(DISCONNECTED_TIME, getTimestamp());
                    cdr.setValue(LOG, log.toString());
                    sendCdrToConsumers();
                }
            } else if (event instanceof RejectedQueueEvent) {
                if (cdr!=null) {
                    cdr.setValue(REJECETED_TIME, getTimestamp());
                    cdr.setValue(LOG, log.toString());
                    sendCdrToConsumers();
                }
            } else if (event instanceof NumberChangedQueueEvent) {
                addToLog("#"+((NumberChangedQueueEvent)event).getCurrentNumber());
            } else if (event instanceof ReadyToCommutateQueueEvent) {
                if (cdr!=null)
                    cdr.setValue(READY_TO_COMMUTATE_TIME, getTimestamp());
            } else if (event instanceof CommutatedQueueEvent) {
                if (cdr!=null)
                    cdr.setValue(COMMUTATED_TIME, getTimestamp());
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

    private void sendCdrToConsumers()
    {
        Collection<Node> deps = owner.getDependentNodes();
        if (deps!=null && !deps.isEmpty()) {
            DataContextImpl context = new DataContextImpl();
            for (Node dep: deps)
                if (dep instanceof DataConsumer)
                    ((DataConsumer)dep).setData(owner, cdr, context);
        }
    }
}
