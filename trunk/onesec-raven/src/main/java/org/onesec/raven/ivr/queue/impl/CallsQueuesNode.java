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
import java.util.Map;
import org.onesec.raven.ivr.queue.CallQueueException;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueues;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import static org.onesec.raven.ivr.queue.impl.CallQueueCdrRecordSchemaNode.*;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CallsQueuesNode  extends BaseNode implements CallsQueues, DataSource
{
    @Parameter
    private RecordSchemaNode cdrRecordSchema;

    private RecordSchemaNode _cdrRecordSchema;

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        checkRecordSchema(cdrRecordSchema);
    }

    public void queueCall(String queueId, CallQueueRequest request) throws CallQueueException
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Queueing call {} to the query {}"
                    , request.getConversation().getObjectName(), queueId);
        try{
            CallQueueRequestWrapper requestWrapper = new CallQueueRequestWrapperImpl(this, request);
            CallsQueue queue = (CallsQueue) getChildren(queueId);
            if (queue!=null)
                queue.queueCall(requestWrapper);
            else {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(
                            "Rejecting queue request for call {}. Because of queue ({}) not found"
                            , request.getConversation().getObjectName(), queueId);
                request.callQueueChangeEvent(new RejectedQueueEventImpl(null, 0));
            }
        }catch(Throwable e){
            String message = logMess(request, "Call queuing error ", e.getMessage());
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(message);
            throw new CallQueueException(message, e);
        }
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException("Pull method not supported by this data source");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    String logMess(CallQueueRequest req, String message, Object... args)
    {
        return req.getConversation().getObjectName()+" "+String.format(message, args);
    }

    private void checkRecordSchema(RecordSchemaNode schema) throws Exception
    {
        if (schema==null)
            return;
        try{
            Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(schema);
            if (fields==null || fields.isEmpty())
                throw new Exception("Schema does not contain fields");
            checkRecordSchemaField(fields, ID, RecordSchemaFieldType.LONG);
            checkRecordSchemaField(fields, QUEUE_ID, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, CALLING_NUMBER, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, OPERATOR_ID, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, OPERATOR_NUMBER, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, LOG, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, QUEUED_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, REJECETED_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, READY_TO_COMMUTATE_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, COMMUTATED_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, DISCONNECTED_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, CONVERSATION_START_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, CONVERSATION_DURATION, RecordSchemaFieldType.INTEGER);
            
        }catch (Exception e){
            throw new Exception(String.format(
                    "Invalid record schema (%s). %s", schema.getPath(), e.getMessage()), e);
        }
        _cdrRecordSchema = schema;
    }

    private void checkRecordSchemaField(
        Map<String, RecordSchemaField> fields, String fieldName, RecordSchemaFieldType type)
            throws Exception
    {
        RecordSchemaField field = fields.get(fieldName);
        if (field==null)
            throw new Exception(String.format("Column (%s) not found", fieldName));
        if (!type.equals(field.getFieldType()))
            throw new Exception(String.format(
                    "Invalid type for column (%s). Expected (%s) but was (%s)"
                    , fieldName, type.toString(), field.getFieldType().toString()));        
    }

    public RecordSchemaNode getCdrRecordSchema() {
        return cdrRecordSchema;
    }

    public void setCdrRecordSchema(RecordSchemaNode cdrRecordSchema) {
        this.cdrRecordSchema = cdrRecordSchema;
    }
}
