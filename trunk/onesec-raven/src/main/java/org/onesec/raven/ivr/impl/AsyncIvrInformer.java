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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrInformerStatus;
import org.raven.RavenUtils;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
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
public class AsyncIvrInformer extends BaseNode implements DataSource, DataConsumer, Viewable
{
    public static final String ERROR_NO_FREE_ENDPOINT_IN_THE_POOL = "ERROR_NO_FREE_ENDPOINT_IN_THE_POOL";
    public static final String ERROR_TOO_MANY_SESSIONS = "ERROR_TOO_MANY_SESSIONS";
    public static final String INFORMER_BINDING = "informer";
    public final static String NOT_PROCESSED_STATUS = "NOT_PROCESSED";
    public final static String PROCESSING_ERROR_STATUS = "PROCESSING_ERROR";
    public static final String RECORD_BINDING = "record";
    public final static String SKIPPED_STATUS = "SKIPPED";
    public final static String NUMBER_BUSY_STATUS = "NUMBER_BUSY";
    public final static String NUMBER_NOT_ANSWERED_STATUS = "NUMBER_NOT_ANSWERED";
    public final static String COMPLETED_BY_INFORMER_STATUS = "COMPLETED_BY_INFORMER";
    public final static String COMPLETED_BY_ABONENT_STATUS = "COMPLETED_BY_ABONENT";
    public final static String ALREADY_INFORMING = "ALREADY_INFORMING";
    public final static String TRANSFER_SUCCESSFULL_STATUS = "TRANSFER_SUCCESSFULL";
    public final static String TRANSFER_DESTINATION_NOT_ANSWER_STATUS = "TRANSFER_DESTINATION_NOT_ANSWER";
    public final static String TRANSFER_ERROR_STATUS = "TRANSFER_ERROR";

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorServiceNode executor;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    @Parameter
    private Integer maxCallDuration;

    @NotNull @Parameter(defaultValue="false")
    private Boolean waitForSession;

    @NotNull @Parameter
    private Integer maxSessionsCount;

    @NotNull @Parameter
    private Integer endpointWaitTimeout;

    @Parameter
    private String displayFields;

    private ReentrantReadWriteLock dataLock;
    private Map<Long, IvrInformerSession> sessions;
    private Condition sessionRemoved;
    private int successfullyInformedAbonents;
    private int informedAbonents;
    private String statusMessage;
    private int handledRecordSets;

    private AtomicReference<IvrInformerStatus> informerStatus;
    private IvrInformerScheduler startScheduler;
    private IvrInformerScheduler stopScheduler;

    @Message
    private static String informedAbonentsMessage;
    @Message
    private static String successfullyInformedAbonentsMessage;
    @Message
    private static String statMessage;
    @Message
    private static String sessionsMessage;

    @Override
    protected void initFields()
    {
        super.initFields();

        dataLock = new ReentrantReadWriteLock();
        sessionRemoved = dataLock.writeLock().newCondition();
        sessions = new HashMap<Long, IvrInformerSession>();

        informerStatus = new AtomicReference<IvrInformerStatus>(IvrInformerStatus.NOT_READY);
        resetStatFields();
    }

    private void resetStatFields()
    {
        successfullyInformedAbonents = 0;
        informedAbonents = 0;
        statusMessage = null;
        handledRecordSets = 0;
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

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public Integer getMaxCallDuration()
    {
        return maxCallDuration;
    }

    public void setMaxCallDuration(Integer maxCallDuration)
    {
        this.maxCallDuration = maxCallDuration;
    }

    public IvrInformerStatus getInformerStatus()
    {
        return informerStatus.get();
    }

    public void setInformerStatus(IvrInformerStatus informerStatus)
    {
        this.informerStatus.set(informerStatus);
    }

    public ExecutorServiceNode getExecutor()
    {
        return executor;
    }

    public void setExecutor(ExecutorServiceNode executor)
    {
        this.executor = executor;
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

    public IvrEndpointPool getEndpointPool()
    {
        return endpointPool;
    }

    public void setEndpointPool(IvrEndpointPool endpointPool)
    {
        this.endpointPool = endpointPool;
    }

    public String getDisplayFields()
    {
        return displayFields;
    }

    public void setDisplayFields(String displayFields)
    {
        this.displayFields = displayFields;
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

    public void startProcessing()
    {
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
            ++handledRecordSets;
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
//        if (!Status.STARTED.equals(getStatus())
//            || !IvrInformerStatus.PROCESSING.equals(informerStatus.get()))
//        {
//            return;
//        }
        if (!(data instanceof Record))
            return;

        Record rec = (Record) data;
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Tring to inform abonent: "+getRecordInfo(rec));
        try
        {
            if (dataLock.writeLock().tryLock(2, TimeUnit.SECONDS))
            {
                try{
                    initFields(rec);
                    createSession(rec);
                }finally{
                    dataLock.writeLock().unlock();
                }

            }
        } catch (Exception ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error while triyng to inform abonent: "+getRecordInfo(rec), ex);
        }
    }
    
    String getRecordInfo(Record rec)
    {
        try
        {
            return String.format(
                    "id (%s), abon_id (%s), abon_number (%s)"
                    , rec.getValue(ID_FIELD)
                    , rec.getValue(ABONENT_ID_FIELD)
                    , rec.getValue(ABONENT_NUMBER_FIELD));
        } catch (RecordException ex)
        {
            return "Error acquire information from record. "+ex.getMessage();
        }
    }


    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        return null;
    }

    void sendDataToConsumers(Object data)
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
        List<ViewableObject> voList = new ArrayList<ViewableObject>(5);

        TableImpl statTable = new TableImpl(
                new String[]{informedAbonentsMessage, successfullyInformedAbonentsMessage});
        statTable.addRow(new Object[]{informedAbonents, successfullyInformedAbonents});
        
        voList.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+statMessage+"</b>:"));
        voList.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, statTable));

        dataLock.readLock().lock();
        try
        {
            if (!sessions.isEmpty())
            {
                String fieldNames = displayFields;
                String[] fields = fieldNames==null? null : fieldNames.split("\\s*,\\s*");
                String[] columnNames = new String[fields.length];
                Map<String, RecordSchemaField> recordFields = RavenUtils.getRecordSchemaFields(recordSchema);
                for (int i=0; i<fields.length; ++i)
                {
                    RecordSchemaField recordField = recordFields.get(fields[i]);
                    if (recordField!=null)
                        columnNames[i]=recordField.getDisplayName();
                    else
                        columnNames[i]=fields[i];
                }

                TableImpl table = new TableImpl(fields);

                for (IvrInformerSession session: sessions.values())
                {
                    Object[] row = new Object[fields.length];
                    for (int i=0; i<fields.length; ++i)
                        if (recordFields.containsKey(fields[i]))
                            row[i] = session.getRecord().getValue(fields[i]);
                    table.addRow(row);
                }

                voList.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+sessionsMessage+"</b>:"));
                voList.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
            }
        }
        finally
        {
            dataLock.readLock().unlock();
        }

//        //current status
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+currentStatusMessage+"</b>: ("+informerStatus+") "+statusMessage));
//
//        //endpoint status
//        IvrEndpoint term = endpoint;
//        String termStatus = term==null? "UNKNOWN" : term.getEndpointState().getIdName();
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+endpointStatusMessage+"</b>: "+termStatus));
//
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+handledRecordSetsMessage+"</b>: "+handledRecordSets));
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+processRecordDurationMessage+"</b>: "+getCurrentCallDuration()));
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+currentCallDurationMessage+"</b>: "+getCurrentCallDuration()));
//
//        //current record
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+currentRecordMessage+"</b>: "));
//
//
//        Record rec = currentRecord;
//        if (rec!=null)
//        {
//            String fieldNames = displayFields;
//            String[] fields = fieldNames==null? null : fieldNames.split("\\s*,\\s*");
//            Set<String> fieldsSet = new HashSet<String>();
//            if (fields!=null)
//                for (String name: fields)
//                    fieldsSet.add(name);
//            TableImpl table = new TableImpl(
//                    new String[]{fieldNameColumnMessage, valueColumnMessage});
//            for (RecordSchemaField field: rec.getSchema().getFields())
//            {
//                if (fieldsSet.isEmpty() || fieldsSet.contains(field.getName()))
//                {
//                    String fieldName = field.getDisplayName();
//                    String value = converter.convert(
//                            String.class, rec.getValue(field.getName()), field.getPattern());
//                    table.addRow(new Object[]{fieldName, value});
//                }
//            }
//            viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
//        }
//
//        //statistics
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE
//                , "<b>"+statisticsMessage+"</b>: "));
//        TableImpl table = new TableImpl(
//                new String[]{statisticNameColumnMessage, statisticValueColumnMessage});
//        table.addRow(new Object[]{processedRecordsCountMessage, processedRecordsCount});
//        table.addRow(new Object[]{processedAbonsCountMessage, processedAbonsCount});
//        table.addRow(new Object[]{informedAbonsCountMessage, informedAbonsCount});
//        viewableObjects.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        return voList;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    private void initFields(Record rec) throws RecordException
    {
        rec.setValue(CALL_START_TIME_FIELD, new Timestamp(System.currentTimeMillis()));
    }

    private boolean isAlreadyInforming(Record rec) throws RecordException
    {
        Object number = rec.getValue(ABONENT_NUMBER_FIELD);
        for (IvrInformerSession session: sessions.values())
        {
            if (ObjectUtils.equals(number, session.getRecord().getValue(ABONENT_NUMBER_FIELD)))
            {
                rec.setValue(COMPLETION_CODE_FIELD, ALREADY_INFORMING);
                sendDataToConsumers(rec);
                return true;
            }
        }

        return false;
    }

    private IvrInformerSession createSession(Record rec) throws Exception
    {
        if (isAlreadyInforming(rec))
            return null;
        
        if (sessions.size()>=maxSessionsCount && !waitForSession)
        {
            rec.setValue(COMPLETION_CODE_FIELD, ERROR_TOO_MANY_SESSIONS);
            sendDataToConsumers(rec);
            return null;
        }

        if (sessions.size()>=maxSessionsCount)
            sessionRemoved.await();

        Long id = converter.convert(Long.class, rec.getValue(ID_FIELD), null);
        IvrEndpoint endpoint = endpointPool.getEndpoint(endpointWaitTimeout);
        if (endpoint==null)
        {
            rec.setValue(COMPLETION_CODE_FIELD, ERROR_NO_FREE_ENDPOINT_IN_THE_POOL);
            sendDataToConsumers(rec);
            return null;
        }

        IvrInformerSession session = new IvrInformerSession(
                rec, this, endpoint, maxCallDuration, conversationScenario);
        sessions.put(id, session);
        executor.execute(session);

        return session;
    }

    void removeSession(IvrInformerSession session)
    {
        try
        {
            endpointPool.releaseEndpoint(session.getEndpoint());
            
            Long id = converter.convert(Long.class, session.getRecord().getValue(ID_FIELD), null);
            dataLock.writeLock().lock();
            try
            {
                session = sessions.remove(id);
                if (session != null)
                {
                    sessionRemoved.signal();
                }
            } finally
            {
                dataLock.writeLock().unlock();
            }
        }
        catch (RecordException ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error removing session from the list: "+getRecordInfo(session.getRecord()), ex);
        }
    }

    void sendRecordToConsumers(Record record)
    {
        sendDataToConsumers(record);
        sendDataToConsumers(null);
    }

    void incSuccessfullyInformedAbonents()
    {
        ++successfullyInformedAbonents;
    }

    void incInformedAbonents()
    {
        ++informedAbonents;
    }

}
