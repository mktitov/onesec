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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.queue.*;
import static org.onesec.raven.ivr.queue.impl.CallQueueCdrRecordSchemaNode.*;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.*;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CallsQueuesNode  extends BaseNode implements DataPipe
{
    public final static String QUEUED_EVENT = "QUEUED"; 
    public final static String ASSIGNED_TO_OPERATOR_EVENT = "ASSIGNED_TO_OPERATOR";
    public final static String CONVERSATION_STARTED_EVENT = "CONVERSATION_STARTED";
    public final static String CALL_FINISHED_EVENT = "CALL_FINISHED";
    
    private final static Set<String> AVAILABLE_EVENTS = new HashSet<String>(Arrays.asList(
        QUEUED_EVENT, ASSIGNED_TO_OPERATOR_EVENT, CONVERSATION_STARTED_EVENT, CALL_FINISHED_EVENT));
    
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode cdrRecordSchema;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useOnlyRegisteredOperators;
    
    @Parameter(defaultValue=CALL_FINISHED_EVENT)
    private String permittedEventTypes;

    private AtomicLong requestIdSeq;
    private RecordSchemaNode _cdrRecordSchema;
    private CallsQueueOperatorsNode operatorsNode;
    private CallsQueuesContainerNode queuesNode;
    private CallsQueueTransferOperatorNode transferOperator;
    private OperatorRegistratorNode operatorRegistrator;
    private Set<String> permittedEvents; 
    

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        checkRecordSchema(cdrRecordSchema);
        permittedEvents = initPermittedEvents();
        initNodes();
    }
    
    private void initNodes() {
        requestIdSeq = new AtomicLong();
        operatorRegistrator = (OperatorRegistratorNode) getChildren(OperatorRegistratorNode.NAME);
        if (operatorRegistrator==null) {
            operatorRegistrator = new OperatorRegistratorNode();
            addAndSaveChildren(operatorRegistrator);
            operatorRegistrator.start();
        }
        operatorsNode = (CallsQueueOperatorsNode) getChildren(CallsQueueOperatorsNode.NAME);
        if (operatorsNode==null){
            operatorsNode = new CallsQueueOperatorsNode();
            addAndSaveChildren(operatorsNode);
            operatorsNode.start();
        }
        transferOperator = (CallsQueueTransferOperatorNode) operatorsNode.getChildren(
                CallsQueueTransferOperatorNode.NAME);
        if (transferOperator==null) {
            transferOperator = new CallsQueueTransferOperatorNode();
            operatorsNode.addAndSaveChildren(transferOperator);
            transferOperator.start();
        }
        queuesNode = (CallsQueuesContainerNode) getChildren(CallsQueuesContainerNode.NAME);
        if (queuesNode==null){
            queuesNode = new CallsQueuesContainerNode();
            addAndSaveChildren(queuesNode);
            queuesNode.start();
        }
    }

    private void queueCall(CallQueueRequest request) throws CallQueueException
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("{}. CallsQueues. Queueing call to the queue {}"
                    , request.getConversationInfo(), request.getQueueId());
        try{
            CallQueueRequestController requestWrapper = 
                    new CallQueueRequestControllerImpl(this, request, requestIdSeq.incrementAndGet());
            if (!Status.STARTED.equals(getStatus())){
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn(
                            logMess(request, "Rejected because of queues selector stopped"));
                requestWrapper.addToLog("queues selector stopped");
                requestWrapper.fireRejectedQueueEvent();
                return;
            }
            if (request.getQueueId()==null){
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(
                            logMess(request, "Rejected because of queueId can not be null"));
                requestWrapper.addToLog("null queue id");
                requestWrapper.fireRejectedQueueEvent();
                return;
            }
            Node queue = queuesNode.getChildren(request.getQueueId());
            if (queue!=null && Status.STARTED.equals(queue.getStatus())) {
                ((CallsQueue)queue).queueCall(requestWrapper);
            } else {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(logMess(
                            request, "Rejected because of queue (%s) not found or stopped"
                            , request.getQueueId()));
                requestWrapper.addToLog("queue not found");
                requestWrapper.fireRejectedQueueEvent();
            }
        }catch(Throwable e){
            String message = logMess(request, "Call queuing error ", e.getMessage());
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(message);
            throw new CallQueueException(message, e);
        }
    }
    
    public Set<String> getPermittedEvent() {
        return permittedEvents;
    }

    public String getPermittedEventTypes() {
        return permittedEventTypes;
    }

    public void setPermittedEventTypes(String permittedEventTypes) {
        this.permittedEventTypes = permittedEventTypes;
    }

    public CallsQueueOperatorsNode getOperatorsNode() {
        return operatorsNode;
    }

    public OperatorRegistratorNode getOperatorRegistrator() {
        return operatorRegistrator;
    }

    public CallsQueuesContainerNode getQueuesNode() {
        return queuesNode;
    }

    public CallsQueueTransferOperatorNode getTransferOperator() {
        return transferOperator;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException("Pull method not supported by this data source");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException(
                "refreshData operation is unsupported by this data source");
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (!(data instanceof CallQueueRequest))
            return;
        try {
            queueCall((CallQueueRequest)data);
        } catch (CallQueueException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(
                        logMess((CallQueueRequest)data, "Error processing request from %s"
                            , dataSource.getPath())
                        , ex);
        }
    }
    
    public CallsQueueOperator processCallTransferedEvent(String phoneNumber
            , CallsCommutationManager commutationManager) 
    {
        CallsQueueOperator oper = getOperatorByPhoneNumber(phoneNumber);
        if (oper==null || !oper.callTransferedToOperator(commutationManager)) 
            oper = transferOperator;
        return oper;
    }

    String logMess(CallQueueRequest req, String message, Object... args)
    {
        return req.getConversationInfo()+". CallsQueues.  "+String.format(message, args);
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
            checkRecordSchemaField(fields, TARGET_QUEUE, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, CALLING_NUMBER, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, OPERATOR_ID, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, OPERATOR_NUMBER, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, LOG, RecordSchemaFieldType.STRING);
            checkRecordSchemaField(fields, QUEUED_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkRecordSchemaField(fields, REJECTED_TIME, RecordSchemaFieldType.TIMESTAMP);
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

    public Boolean getUseOnlyRegisteredOperators() {
        return useOnlyRegisteredOperators;
    }

    public void setUseOnlyRegisteredOperators(Boolean useOnlyRegisteredOperators) {
        this.useOnlyRegisteredOperators = useOnlyRegisteredOperators;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public RecordSchemaNode getCdrRecordSchema() {
        return cdrRecordSchema;
    }

    public void setCdrRecordSchema(RecordSchemaNode cdrRecordSchema) {
        this.cdrRecordSchema = cdrRecordSchema;
    }
    
    public CallsQueueOperatorNode getOperatorByPhoneNumber(String phoneNumber) {
        phoneNumber = normalizePhoneNumber(phoneNumber);
        for (CallsQueueOperatorNode oper: NodeUtils.getChildsOfType(operatorsNode, CallsQueueOperatorNode.class)) {
            String[] phones = RavenUtils.split(oper.getPhoneNumbers());
            if (phones!=null)
                for (String phone: phones) {
                    phone = normalizePhoneNumber(phone);
                    if (phone.equals(phoneNumber))
                        return oper;
                }
        }
        return null;
    }
    
    private String normalizePhoneNumber(String num) {
        return num.length()<=10? num : num.substring(num.length()-10);
    }

    private Set<String> initPermittedEvents() throws Exception {
        String[] toks = RavenUtils.split(permittedEventTypes);
        if (toks==null || toks.length==0)
            return Collections.EMPTY_SET;
        else {
            for (int i=0; i<toks.length; ++i) {
                toks[i] = toks[i].toUpperCase();
                if (!AVAILABLE_EVENTS.contains(toks[i]))
                    throw new Exception("Invalid event type: "+toks[i]);
            }
            return new HashSet<String>(Arrays.asList(toks));
        }
    }
}
