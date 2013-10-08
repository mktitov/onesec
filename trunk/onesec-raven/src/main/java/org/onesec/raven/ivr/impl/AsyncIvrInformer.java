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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrInformerStatus;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.sched.impl.TimeWindowHelper;
import org.raven.sched.impl.TimeWindowNode;
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
import org.raven.BindingNames;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=InformerTimeWindow.class)
public class AsyncIvrInformer extends BaseNode implements DataSource, DataConsumer, Viewable, Schedulable
{
    public static final String ERROR_NO_FREE_ENDPOINT_IN_THE_POOL = "ERROR_NO_FREE_ENDPOINT_IN_THE_POOL";
    public static final String ERROR_TOO_MANY_SESSIONS = "ERROR_TOO_MANY_SESSIONS";
    public static final String INFORMER_BINDING = "informer";
    public final static String NOT_PROCESSED_STATUS = "NOT_PROCESSED";
    public static final String NUMBER_BINDING = "number";
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

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler startScheduler;

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

    @Parameter
    private Integer maxInviteDuration;

    @NotNull @Parameter(defaultValue="false")
    private Boolean waitForSession;

    @NotNull @Parameter(defaultValue="false")
    private Boolean autoStart;

    @NotNull @Parameter
    private Integer maxSessionsCount;

    @NotNull @Parameter
    private Integer endpointWaitTimeout;

    @Parameter
    private String displayFields;

    @Parameter
    private String groupField;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String numberTranslation;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useNumberTranslation;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String aNumberSubstitution;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useANumberSubstitution;

    @NotNull @Parameter(defaultValue="10")
    private Integer priority;

    private ReentrantReadWriteLock dataLock;
    private Map<Long, IvrInformerSession> sessions;
    private Condition sessionRemoved;
    private int successfullyInformedAbonents;
    private int informedAbonents;
    private String statusMessage;
    private int handledRecordSets;

    private AtomicReference<IvrInformerStatus> informerStatus;
    private AtomicBoolean informAllowed;
    private AtomicBoolean receivingData;
    private List<Record> records;
    private BindingSupportImpl bindingSupport;


    @Message private static String informAllowedMessage;
    @Message private static String defaultMaxSessionsMessage;
    @Message private static String currentMaxSessionsMessage;
    @Message private static String statusMessageTitle;
    @Message private static String informedAbonentsMessage;
    @Message private static String successfullyInformedAbonentsMessage;
    @Message private static String statMessage;
    @Message private static String sessionsMessage;
    @Message private static String terminalMessage;
    @Message private static String terminalStatusMessage;
    @Message private static String callDurationMessage;

    @Override
    protected void initFields()
    {
        super.initFields();

        dataLock = new ReentrantReadWriteLock();
        sessionRemoved = dataLock.writeLock().newCondition();
        sessions = new HashMap<Long, IvrInformerSession>();
        bindingSupport = new BindingSupportImpl();
        informAllowed = new AtomicBoolean(true);
        receivingData = new AtomicBoolean(false);

        informerStatus = new AtomicReference<IvrInformerStatus>(IvrInformerStatus.NOT_READY);
        resetStatFields();
    }

    private void resetStatFields()
    {
        successfullyInformedAbonents = 0;
        informedAbonents = 0;
        statusMessage = null;
        handledRecordSets = 0;
        records = null;
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
//        generateNodes();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        resetStatFields();
        sessions.clear();
//        generateNodes();
        informerStatus.set(IvrInformerStatus.WAITING);
        informAllowed.set(true);
    }
    
//    private void initTimeWindowNodes() {
//        for (TimeWindowNode window: NodeUtils.getChildsOfType(this, TimeWindowNode.class, false))
//            if (window.getAttr("maxSessionsCount")==null) {
//                NodeAttributeImpl attr = new NodeAttributeImpl("maxSessionsCount", Integer.class, null, null);
//            }
//    }
//
    public void executeScheduledJob(Scheduler scheduler)
    {
        if (!Status.STARTED.equals(getStatus()))
            return;
        if (TimeWindowHelper.isCurrentDateInPeriod(this))
            startProcessing();
//        else
//            stopProcessing();
    }

    void startProcessing()
    {
//        informAllowed.set(true);

//        if (!informerStatus.compareAndSet(IvrInformerStatus.WAITING, IvrInformerStatus.PROCESSING))
        if (!receivingData.compareAndSet(false, true))
        {
            if (isLogLevelEnabled(LogLevel.TRACE))
                trace("Informator already processing records");
            return;
        }
        boolean sessionsEmpty = false;
        try
        {
            try {
                if (dataLock.readLock().tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        if (!sessions.isEmpty()) {
                            if (isLogLevelEnabled(LogLevel.DEBUG)) 
                                debug("Can not start processing. Queue is not empty");
                            sessionsEmpty = true;
                            return;
                        }
                    } finally {
                        dataLock.readLock().unlock();
                    }
                } else {
                    return;
                }
            } catch (InterruptedException ex) {
                return;
            }

            statusMessage = "Requesting records from "+dataSource.getPath();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(statusMessage);
            dataSource.getDataImmediate(this, new DataContextImpl());
        }
        finally
        {
            receivingData.set(false);
            if (!sessionsEmpty){
                ++handledRecordSets;
                statusMessage = "All records sended by data source where processed.";
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(statusMessage);
            }
        }
    }

    void stopProcessing()
    {
        informAllowed.set(false);
        try
        {
            if (dataLock.writeLock().tryLock(5, TimeUnit.SECONDS))
            {
                try {
                    sessionRemoved.signal();
                } finally {
                    dataLock.writeLock().unlock();
                }
            }
        }
        catch (InterruptedException e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error notifying thread, that waiting for informer session, about infomer shutdown", e);
        }
    }

//    private void generateNodes()
//    {
//        startScheduler = (IvrInformerScheduler) getChildren(IvrInformerScheduler.START_SCHEDULER);
//        if (startScheduler==null)
//        {
//            startScheduler = new IvrInformerScheduler(IvrInformerScheduler.START_SCHEDULER);
//            addAndSaveChildren(startScheduler);
//        }
//
//        stopScheduler = (IvrInformerScheduler) getChildren(IvrInformerScheduler.STOP_SCHEDULER);
//        if (stopScheduler==null)
//        {
//            stopScheduler = new IvrInformerScheduler(IvrInformerScheduler.STOP_SCHEDULER);
//            addAndSaveChildren(stopScheduler);
//        }
//    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        informerStatus.set(IvrInformerStatus.NOT_READY);
        stopProcessing();
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public Integer getEndpointWaitTimeout()
    {
        return endpointWaitTimeout;
    }

    public void setEndpointWaitTimeout(Integer endpointWaitTimeout)
    {
        this.endpointWaitTimeout = endpointWaitTimeout;
    }

    public Integer getMaxSessionsCount()
    {
        return maxSessionsCount;
    }

    public void setMaxSessionsCount(Integer maxSessionsCount)
    {
        this.maxSessionsCount = maxSessionsCount;
    }

    public Boolean getWaitForSession()
    {
        return waitForSession;
    }

    public void setWaitForSession(Boolean waitForSession)
    {
        this.waitForSession = waitForSession;
    }

    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }

    public Integer getMaxInviteDuration() {
        return maxInviteDuration;
    }

    public void setMaxInviteDuration(Integer maxInviteDuration) {
        this.maxInviteDuration = maxInviteDuration;
    }

    public Integer getMaxCallDuration()
    {
        return maxCallDuration;
    }

    public void setMaxCallDuration(Integer maxCallDuration)
    {
        this.maxCallDuration = maxCallDuration;
    }

    @Parameter(readOnly=true)
    public Boolean getInformAllowed()
    {
        return informAllowed.get();
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

    public Scheduler getStartScheduler()
    {
        return startScheduler;
    }

    public void setStartScheduler(Scheduler startScheduler)
    {
        this.startScheduler = startScheduler;
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

    public String getGroupField() {
        return groupField;
    }

    public void setGroupField(String groupField) {
        this.groupField = groupField;
    }

    public String getDisplayFields()
    {
        return displayFields;
    }

    public void setDisplayFields(String displayFields)
    {
        this.displayFields = displayFields;
    }

    public String getNumberTranslation() {
        return numberTranslation;
    }

    public void setNumberTranslation(String numberTranslation) {
        this.numberTranslation = numberTranslation;
    }

    public Boolean getUseNumberTranslation() {
        return useNumberTranslation;
    }

    public void setUseNumberTranslation(Boolean useNumberTranslation) {
        this.useNumberTranslation = useNumberTranslation;
    }

    public String getaNumberSubstitution() {
        return aNumberSubstitution;
    }

    public void setaNumberSubstitution(String aNumberSubstitution) {
        this.aNumberSubstitution = aNumberSubstitution;
    }

    public Boolean getUseANumberSubstitution() {
        return useANumberSubstitution;
    }

    public void setUseANumberSubstitution(Boolean useANumberSubstitution) {
        this.useANumberSubstitution = useANumberSubstitution;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        if (Status.STARTED.equals(getStatus()))
            return dataSource.getDataImmediate(this, context);
        else {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Node not started! Can't initiate pull request (getDataImmediate)");
            return false;
        }
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (!Status.STARTED.equals(getStatus()) || !informAllowed.get())
            return;
        
        TimeWindowNode timeWindow = TimeWindowHelper.getTimeWindowForCurrentDate(this);
        if (timeWindow==null)
            return;
        
        if (data!=null && !(data instanceof Record))
            return;
        
        Record rec = (Record) data;
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Tring to inform abonent: "+getRecordInfo(rec));
        try {
            if (dataLock.writeLock().tryLock(2, TimeUnit.SECONDS)) {
                try {
                    bindingSupport.put(RECORD_BINDING, rec);
                    bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
                    List<Record> recordsChain = null;
                    if (rec!=null) {
                        String _groupField = groupField;
                        if (_groupField!=null) {
                            if (records==null)
                                records = new ArrayList<Record>();
                            if (records.isEmpty() || ObjectUtils.equals(
                                    rec.getValue(_groupField), records.get(0).getValue(_groupField)))
                                records.add(rec);
                            else {
                                recordsChain = records;
                                records = new ArrayList();
                                records.add(rec);
                            }
                        } else {
                            recordsChain = Arrays.asList(rec);
                        }
                    } else if (records!=null) {
                        recordsChain = records;
                        records = null;
                    }
                    if (recordsChain!=null) {
                        try{
                            for (Record record: recordsChain)
                                initFields(record);
                            createSession(recordsChain, context, calcMaxSessionsCount(timeWindow));
                        } finally{
                            recordsChain = null;
                        }
                    }
                } finally {
                    dataLock.writeLock().unlock();
                    bindingSupport.reset();
                }
            }
        } catch (Exception ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error while triyng to inform abonent: "+getRecordInfo(rec), ex);
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    private int calcMaxSessionsCount(TimeWindowNode window) {
        NodeAttribute attr = window.getAttr("maxSessionsCount");
        Integer count = null;
        if (attr!=null && Integer.class.equals(attr.getType())) 
            count = attr.getRealValue();
        return count==null? maxSessionsCount : count;
    }
    
    String getRecordInfo(Record rec)
    {
        if (rec==null)
            return "";
        try
        {
            return String.format(
                    "id (%s), abon_number (%s)"
                    , rec.getValue(ID_FIELD)
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

    void sendDataToConsumers(Object data, DataContext context)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null && !depNodes.isEmpty())
            for (Node dep: depNodes)
                if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus()))
                    ((DataConsumer)dep).setData(this, data, context);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        List<ViewableObject> voList = new ArrayList<ViewableObject>(5);

        TableImpl statTable = new TableImpl(new String[]{
            informAllowedMessage, defaultMaxSessionsMessage, currentMaxSessionsMessage, 
            informedAbonentsMessage, successfullyInformedAbonentsMessage, statusMessageTitle});
        TimeWindowNode window = TimeWindowHelper.getTimeWindowForCurrentDate(this);
        statTable.addRow(new Object[]{
            getInformAllowed() && window!=null, maxSessionsCount, calcMaxSessionsCount(window),
            informedAbonents, successfullyInformedAbonents, statusMessage});
        
        voList.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+statMessage+"</b>:"));
        voList.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, statTable));

        dataLock.readLock().lock();
        try {
            if (!sessions.isEmpty()) {
                String fieldNames = displayFields;
                String[] fields = fieldNames==null? null : fieldNames.split("\\s*,\\s*");
                int addCols = 3;
                String[] columnNames = new String[fields.length+addCols];
                Map<String, RecordSchemaField> recordFields = RavenUtils.getRecordSchemaFields(recordSchema);
                columnNames[0] = terminalMessage;
                columnNames[1] = terminalStatusMessage;
                columnNames[2] = callDurationMessage;
                for (int i=0; i<fields.length; ++i) {
                    RecordSchemaField recordField = recordFields.get(fields[i]);
                    if (recordField!=null)
                        columnNames[i+addCols]=recordField.getDisplayName();
                    else
                        columnNames[i+addCols]=fields[i];
                }

                TableImpl table = new TableImpl(columnNames);

                for (IvrInformerSession session: sessions.values()) {
                    Object[] row = new Object[columnNames.length];
                    IvrEndpoint endpoint = session.getEndpoint();
                    if (endpoint!=null) {
                        row[0] = endpoint.getName();
                        row[1] = endpoint.getEndpointState().getIdName();
                    }
                    Record rec = session.getCurrentRecord();
                    if (rec!=null)
                        row[2] = new Long((System.currentTimeMillis()-((Timestamp)rec.getValue(CALL_START_TIME_FIELD)).getTime())/1000);
                    for (int i=0; i<fields.length; ++i)
                        if (recordFields.containsKey(fields[i])) {
                            Record record = session.getCurrentRecord();
                            if (record!=null)
                                row[i+addCols] = session.getCurrentRecord().getValue(fields[i]);
                        }
                    table.addRow(row);
                }
                voList.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+sessionsMessage+"</b>:"));
                voList.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
            }
        } finally {
            dataLock.readLock().unlock();
        }

        return voList;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public int getSessionsCount()
    {
        dataLock.readLock().lock();
        try
        {
            return sessions.size();
        }
        finally
        {
            dataLock.readLock().unlock();
        }
    }

    public IvrInformerSession getSession(long id)
    {
        dataLock.readLock().lock();
        try
        {
            return sessions.get(id);
        }
        finally
        {
            dataLock.readLock().unlock();
        }
    }

    public IvrInformerSession getSessionByAbonentNumber(String abonentNumber) throws RecordException
    {
        dataLock.readLock().lock();
        try
        {
            for (IvrInformerSession session: sessions.values())
                if (session.containsAbonentNumber(abonentNumber))
                    return session;
            return null;
        }
        finally
        {
            dataLock.readLock().unlock();
        }
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
            if (session.containsAbonentNumber(number))
            {
//                rec.setValue(COMPLETION_CODE_FIELD, ALREADY_INFORMING);
//                sendRecordToConsumers(rec);
                return true;
            }
        }

        return false;
    }

    private IvrInformerSession createSession(List<Record> records, DataContext context, int maxSessions) throws Exception
    {
        boolean alreadyInforming = false;
        for (Record record: records) {
            if (isAlreadyInforming(record)) {
                alreadyInforming = true;
                break;
            }
        }
        if (alreadyInforming) {
            for (Record record: records) {
                record.setValue(COMPLETION_CODE_FIELD, ALREADY_INFORMING);
                sendRecordToConsumers(record, context);
            }
            return null;
        }
        
        if (sessions.size()>=maxSessions && !waitForSession) {
            for (Record record: records) {
                record.setValue(COMPLETION_CODE_FIELD, ERROR_TOO_MANY_SESSIONS);
                sendRecordToConsumers(record, context);
            }
            return null;
        }

        if (sessions.size()>=maxSessions)
            sessionRemoved.await();

        if (!informAllowed.get())
            return null;

        Long id = converter.convert(Long.class, records.iterator().next().getValue(ID_FIELD), null);
        IvrInformerSession session = new IvrInformerSession(records, this, maxInviteDuration
                , maxCallDuration, conversationScenario, endpointWaitTimeout, context, priority);
        sessions.put(id, session);
        endpointPool.requestEndpoint(session);

        return session;
    }

    void removeSession(IvrInformerSession session)
    {
        Record firstRecord = session.getRecords().get(0);
        try {
            if (session.getEndpoint()!=null)
                endpointPool.releaseEndpoint(session.getEndpoint());
            Long id = converter.convert(Long.class, firstRecord.getValue(ID_FIELD), null);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Removing session: "+id);
            dataLock.writeLock().lock();
            try {
                session = sessions.remove(id);
                if (session != null) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug("Session successfully removed: "+id);
                    sessionRemoved.signal();
                } else
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug("Session with id ("+id+") not found");
            } finally {
                dataLock.writeLock().unlock();
            }
        } catch (RecordException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error removing session from the list: "+getRecordInfo(firstRecord), ex);
        }
    }

    void sendRecordToConsumers(Record record, DataContext context)
    {
        sendDataToConsumers(record, context);
        sendDataToConsumers(null, context);
    }

    void incSuccessfullyInformedAbonents()
    {
        ++successfullyInformedAbonents;
    }

    void incInformedAbonents()
    {
        ++informedAbonents;
    }

    String translateNumber(String number, Record record) {
        if (!useNumberTranslation)
            return number;
        try {
            bindingSupport.put(NUMBER_BINDING, number);
            bindingSupport.put(RECORD_BINDING, record);
            String translatedNumber = numberTranslation;
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Number ({}) was translated to ({})", number, translatedNumber);
            return translatedNumber;
        } finally {
            bindingSupport.reset();
        }
    }
    
    String substituteANumber(Record record, DataContext context) throws RecordException {
        if (!useANumberSubstitution)
            return null;
        try {
            bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
            bindingSupport.put(RECORD_BINDING, record);
            String aNumber = aNumberSubstitution;
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Using ({}) as A number to inform: {}", aNumber, record.getValue(ABONENT_NUMBER_FIELD));
            return aNumber;
        } finally {
            bindingSupport.reset();
        }
    }
}
