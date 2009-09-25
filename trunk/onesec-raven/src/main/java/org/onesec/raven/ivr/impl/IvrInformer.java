/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.IvrInformerStatus;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Message;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrInformer 
        extends BaseNode 
        implements DataSource, ConversationCompletionCallback, DataConsumer, Viewable
{
    public static final String INFORMER_BINDING = "informer";
    public final static String NOT_PROCESSED_STATUS = "NOT_PROCESSED";
    public final static String PROCESSING_ERROR_STATUS = "PROCESSING_ERROR";
    public static final String RECORD_BINDING = "record";
    public final static String SKIPPED_STATUS = "SKIPPED";
    public final static String NUMBER_BUSY_STATUS = "NUMBER_BUSY";
    public final static String NUMBER_NOT_ANSWERED_STATUS = "NUMBER_NOT_ANSWERED";
    public final static String COMPLETED_BY_INFORMER_STATUS = "COMPLETED_BY_INFORMER";
    public final static String COMPLETED_BY_ABONENT_STATUS = "COMPLETED_BY_ABONENT";
    public final static String TRANSFER_SUCCESSFULL_STATUS = "TRANSFER_SUCCESSFULL";
    public final static String TRANSFER_DESTINATION_NOT_ANSWER_STATUS = "TRANSFER_DESTINATION_NOT_ANSWER";
    public final static String TRANSFER_ERROR_STATUS = "TRANSFER_ERROR";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpoint endpoint;

    @NotNull @Parameter(defaultValue="5")
    private Short maxTries;

    private AtomicReference<IvrInformerStatus> informerStatus;
    private String statusMessage;
    private Record currentRecord;
    private String lastSuccessfullyProcessedAbonId;
    private String lastAbonId;
    private Lock recordLock;
    private Condition recordProcessed;
    private ConversationResult conversationResult;
    private IvrInformerScheduler startScheduler;
    private IvrInformerScheduler stopScheduler;

    private int processedRecordsCount;
    private int processedAbonsCount;
    private int informedAbonsCount;

    @Message
    private static String currentStatusMessage;
    @Message
    private static String currentRecordMessage;
    @Message
    private static String fieldNameColumnMessage;
    @Message
    private static String valueColumnMessage;
    @Message
    private static String statisticsMessage;
    @Message
    private static String statisticNameColumnMessage;
    @Message
    private static String statisticValueColumnMessage;
    @Message
    private static String  processedRecordsCountMessage;
    @Message
    private static String processedAbonsCountMessage;
    @Message
    private static String informedAbonsCountMessage;

    @Override
    protected void initFields()
    {
        super.initFields();
        recordLock = new ReentrantLock();
        recordProcessed = recordLock.newCondition();
        informerStatus = new AtomicReference<IvrInformerStatus>(IvrInformerStatus.NOT_READY);
        resetStatFields();
    }

    private void resetStatFields()
    {
        processedRecordsCount = 0;
        processedAbonsCount = 0;
        informedAbonsCount = 0;
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        generateNodes();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        resetStatFields();
        generateNodes();
        lastSuccessfullyProcessedAbonId = null;
        lastAbonId = null;
        informerStatus.set(IvrInformerStatus.WAITING);
    }

    private void generateNodes()
    {
        startScheduler = (IvrInformerScheduler) getChildren(IvrInformerScheduler.START_SCHEDULER);
        if (startScheduler==null)
        {
            startScheduler = new IvrInformerScheduler(IvrInformerScheduler.START_SCHEDULER);
            addAndSaveChildren(startScheduler);
        }

        stopScheduler = (IvrInformerScheduler) getChildren(IvrInformerScheduler.STOP_SCHEDULER);
        if (stopScheduler==null)
        {
            stopScheduler = new IvrInformerScheduler(IvrInformerScheduler.STOP_SCHEDULER);
            addAndSaveChildren(stopScheduler);
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        informerStatus.set(IvrInformerStatus.NOT_READY);
    }

    public IvrInformerStatus getInformerStatus()
    {
        return informerStatus.get();
    }

    public void setInformerStatus(IvrInformerStatus informerStatus)
    {
        this.informerStatus.set(informerStatus);
    }

    public IvrConversationScenarioNode getConversationScenario()
    {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario)
    {
        this.conversationScenario = conversationScenario;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public IvrEndpoint getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(IvrEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public Short getMaxTries()
    {
        return maxTries;
    }

    public void setMaxTries(Short maxTries)
    {
        this.maxTries = maxTries;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public void conversationCompleted(ConversationResult conversationResult)
    {
        this.conversationResult = conversationResult;
        recordLock.lock();
        try
        {
            recordProcessed.signal();
        }
        finally
        {
            recordLock.unlock();
        }
    }

    public void startProcessing()
    {
        if (!informerStatus.compareAndSet(IvrInformerStatus.WAITING, IvrInformerStatus.PROCESSING))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Informator already processing records");
            return;
        }
        try
        {
            statusMessage = "Requesting records from "+dataSource.getPath();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(statusMessage);
            dataSource.getDataImmediate(this, null);
            sendDataToConsumers(null);
        }
        finally
        {
            lastSuccessfullyProcessedAbonId = null;
            lastAbonId = null;
            informerStatus.set(IvrInformerStatus.WAITING);
            statusMessage = "All records sended by data source where processed.";
        }
    }

    public void stopProcessing()
    {

        informerStatus.compareAndSet(
                IvrInformerStatus.PROCESSING, IvrInformerStatus.STOP_PROCESSING);
    }

    public void setData(DataSource dataSource, Object data)
    {
        if (!Status.STARTED.equals(getStatus()) 
            || !IvrInformerStatus.PROCESSING.equals(informerStatus.get()))
        {
            return;
        }
        if (!(data instanceof Record))
            return;
        Record rec = (Record) data;
        if (!(rec.getSchema() instanceof IvrInformerRecordSchemaNode))
            return;

        ++processedRecordsCount;
        currentRecord = rec;
        statusMessage = "Recieved new record: "+getRecordShortDesc();
        try
        {
            Short tries = (Short) currentRecord.getValue(TRIES_FIELD);
            if (tries==null || tries<maxTries)
            {
                String abonId = (String) currentRecord.getValue(ABONENT_ID_FIELD);
                if (!ObjectUtils.equals(lastAbonId, abonId))
                {
                    ++processedAbonsCount;
                    lastAbonId = abonId;
                }
                if (ObjectUtils.equals(lastSuccessfullyProcessedAbonId, abonId))
                    skipRecord();
                else
                    informAbonent();
                sendDataToConsumers(currentRecord);
                statusMessage = String.format(
                        "Abonent informed (%s). Waiting for new record", getRecordShortDesc());
            }
            else
                statusMessage = String.format(
                        "Abonent skiped (%s). Max tries (%s) reached"
                        , getRecordShortDesc(), maxTries);
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(getRecordInfo(), e);
        }
    }

    private String getRecordShortDesc()
    {
        try
        {
            return String.format(
                    "id (%s), abon_id (%s), abon_number (%s)"
                    , currentRecord.getValue(ID_FIELD)
                    , currentRecord.getValue(ABONENT_ID_FIELD)
                    , currentRecord.getValue(ABONENT_NUMBER_FIELD));
        } catch (RecordException ex)
        {
            return "";
        }
    }

    private String getRecordInfo()
    {
        try
        {
            return String.format(
                    "Error processing record: id (%s), abon_id (%s), abon_number (%s)"
                    , currentRecord.getValue(ID_FIELD)
                    , currentRecord.getValue(ABONENT_ID_FIELD)
                    , currentRecord.getValue(ABONENT_NUMBER_FIELD));
        } catch (RecordException ex)
        {
            return "Error processing record";
        }
    }
        

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        return null;
    }

    private void skipRecord() throws RecordException
    {
        statusMessage = "Skiping record: "+getRecordShortDesc();
        currentRecord.setValue(COMPLETION_CODE_FIELD, SKIPPED_STATUS);
        Timestamp curTs = new Timestamp(System.currentTimeMillis());
        currentRecord.setValue(CALL_START_TIME_FIELD, curTs);
        currentRecord.setValue(CALL_END_TIME_FIELD, curTs);
    }

    private void informAbonent() throws Exception
    {
        recordLock.lock();
        try
        {
            conversationResult = null;
            String abonNumber = (String) currentRecord.getValue(ABONENT_NUMBER_FIELD);
            try{
                statusMessage = 
                        "Waiting for endpoint IN_SERVICE state to process record: "
                        +getRecordShortDesc();
                endpoint.getEndpointState().waitForState(
                        new int[]{IvrEndpointState.IN_SERVICE}, Long.MAX_VALUE);
                statusMessage = "Informing abonent: "+getRecordShortDesc();
                Map<String, Object> bindings = new HashMap<String, Object>();
                bindings.put(RECORD_BINDING, currentRecord);
                bindings.put(INFORMER_BINDING, this);
                endpoint.invite(abonNumber, conversationScenario, this, bindings);
                recordProcessed.await();
                String status = null;
                if (conversationResult==null)
                    status = PROCESSING_ERROR_STATUS;
                else
                {
                    boolean sucProc = false;
                    switch(conversationResult.getCompletionCode())
                    {
                        case COMPLETED_BY_ENDPOINT: 
                            status = COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
                        case COMPLETED_BY_OPPONENT: 
                            status = COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
                        case OPPONENT_BUSY: status = NUMBER_BUSY_STATUS; break;
                        case OPPONENT_NO_ANSWERED: status = NUMBER_NOT_ANSWERED_STATUS; break;
                        case OPPONENT_UNKNOWN_ERROR: status = PROCESSING_ERROR_STATUS; break;
                    }
                    if (sucProc)
                    {
                        lastSuccessfullyProcessedAbonId =
                                (String) currentRecord.getValue(ABONENT_ID_FIELD);
                        ++informedAbonsCount;
                    }
                    currentRecord.setValue(COMPLETION_CODE_FIELD, status);
                    currentRecord.setValue(
                            CALL_START_TIME_FIELD, conversationResult.getCallStartTime());
                    currentRecord.setValue(
                            CALL_END_TIME_FIELD, conversationResult.getCallEndTime());
                    currentRecord.setValue(
                            CALL_DURATION_FIELD, conversationResult.getCallDuration());
                    currentRecord.setValue(
                            CONVERSATION_START_TIME_FIELD
                            , conversationResult.getConversationStartTime());
                    currentRecord.setValue(
                            CONVERSATION_DURATION_FIELD
                            , conversationResult.getConversationDuration());
                    Short tries = (Short) currentRecord.getValue(TRIES_FIELD);
                    if (tries==null)
                        tries = 1;
                    else
                        tries = (short)(tries + 1);
                    currentRecord.setValue(TRIES_FIELD, tries);
                    if (conversationResult.getTransferCompletionCode()!=null)
                    {
                        String transferStatus = null;
                        switch(conversationResult.getTransferCompletionCode())
                        {
                            case ERROR : transferStatus = TRANSFER_ERROR_STATUS; break;
                            case NORMAL : transferStatus = TRANSFER_SUCCESSFULL_STATUS; break;
                            case NO_ANSWER :
                                transferStatus = TRANSFER_DESTINATION_NOT_ANSWER_STATUS;
                                break;
                        }
                        currentRecord.setValue(TRANSFER_COMPLETION_CODE_FIELD, transferStatus);
                        currentRecord.setValue(
                                TRANSFER_ADDRESS_FIELD, conversationResult.getTransferAddress());
                        currentRecord.setValue(
                                TRANSFER_TIME_FIELD, conversationResult.getTransferTime());
                        currentRecord.setValue(TRANSFER_CONVERSATION_START_TIME_FIELD
                                , conversationResult.getTransferConversationStartTime());
                        currentRecord.setValue(TRANSFER_CONVERSATION_DURATION_FIELD
                                , conversationResult.getTransferConversationDuration());
                    }
                }
            }
            catch(IvrEndpointException ex)
            {
                currentRecord.setValue(COMPLETION_CODE_FIELD, PROCESSING_ERROR_STATUS);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(getRecordInfo(), ex);
            }
        }
        finally
        {
            recordLock.unlock();
        }
    }

    private void sendDataToConsumers(Object data)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null && !depNodes.isEmpty())
            for (Node dep: depNodes)
                if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus()))
                    ((DataConsumer)dep).setData(this, data);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        List<ViewableObject> viewableObjects = new ArrayList<ViewableObject>(5);

        //current status
        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+currentStatusMessage+"</b>: ("+informerStatus+") "+statusMessage));

        //current record
        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+currentRecordMessage+"</b>: "));

        Record rec = currentRecord;
        if (rec!=null)
        {
            TableImpl table = new TableImpl(
                    new String[]{fieldNameColumnMessage, valueColumnMessage});
            for (RecordSchemaField field: rec.getSchema().getFields())
            {
                String fieldName = field.getDisplayName();
                String value = converter.convert(
                        String.class, rec.getValue(field.getName()), field.getPattern());
                table.addRow(new Object[]{fieldName, value});
            }
            viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        }

        //statistics
        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+statisticsMessage+"</b>: "));
        TableImpl table = new TableImpl(
                new String[]{statisticNameColumnMessage, statisticValueColumnMessage});
        table.addRow(new Object[]{processedRecordsCountMessage, processedRecordsCount});
        table.addRow(new Object[]{processedAbonsCountMessage, processedAbonsCount});
        table.addRow(new Object[]{informedAbonsCountMessage, informedAbonsCount});
        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        return viewableObjects;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }
}
