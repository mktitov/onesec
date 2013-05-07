/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.script.Bindings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.onesec.raven.ivr.*;
import static org.onesec.raven.ivr.impl.CallRecordingRecordSchemaNode.*;
import org.raven.BindingNames;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.*;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CallRecorderNode extends BaseNode 
    implements IvrConversationsBridgeListener, Viewable, Schedulable, org.raven.ds.DataSource
{
    public static final String CONV1_BINDING = "conversation1";
    public static final String CONV1_BINDINGS = "conv1Bindings";
    public static final String CONV2_BINDING = "conversation2";
    public static final String CONV2_BINDINGS = "conv2Bindings";
    public static final String DATE_BINDING = "date";
    public static final String PATH_BINDING = "path";
    public static final String TIME_BINDING = "time";
    
    @Service
    private static CodecManager codecManager;
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationBridgeManager;
    
    @NotNull @Parameter
    private String baseDir;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean filterExpression;
    
    @NotNull 
    @Parameter(defaultValue="\"${c2NumB}_${c1NumA}_${time}.wav\".toString()"
            , valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String fileNameExpression;
    
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;
    
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler cleanupScheduler;
    
    @Parameter(defaultValue="60")
    private Integer keepRecordsForDays;
    
    @Parameter(defaultValue="3")
    private Integer noiseLevel;
    
    @Parameter(defaultValue="3")
    private Double maxGainCoef;
    
    @Message private static String callInfoMessage;
    @Message private static String recordStartTimeMessage;
    @Message private static String recordDurationMessage;
    @Message private static String recordFileMessage;
    
    private ConcurrentHashMap<IvrConversationsBridge, Recorder> recorders;
    private volatile File baseDirFile;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        recorders = new ConcurrentHashMap<IvrConversationsBridge, Recorder>();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        baseDirFile = new File(baseDir);
        if (!baseDirFile.exists())
            if (!baseDirFile.mkdirs())
                throw new Exception(String.format("Can't create directory (%s)", baseDir));
        if (!baseDirFile.isDirectory())
            throw new Exception(String.format("File (%s) is not a directory", baseDir));
        if (!baseDirFile.canWrite())
            throw new Exception(String.format("No rights to create files in the directory (%s)", baseDir));
        checkRecord();
    }
    
    private void checkRecord() throws Exception {
        RecordSchemaNode schema = recordSchema;
        if (schema==null)
            return;
        Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(schema);
        try {
            checkSchemaField(fields, CallRecordingRecordSchemaNode.ID, RecordSchemaFieldType.LONG);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.CONV1_NUMA, RecordSchemaFieldType.STRING);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.CONV1_NUMB, RecordSchemaFieldType.STRING);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.CONV2_NUMA, RecordSchemaFieldType.STRING);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.CONV2_NUMB, RecordSchemaFieldType.STRING);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.FILE, RecordSchemaFieldType.STRING);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.RECORDING_TIME, RecordSchemaFieldType.TIMESTAMP);
            checkSchemaField(fields, CallRecordingRecordSchemaNode.RECORDING_DURATION, RecordSchemaFieldType.INTEGER);
        } catch(Exception e) {
            throw new Exception(String.format(
                    "Record schema (%s) validation error. %s"
                    , schema.getPath(), e.getMessage()));
        }
    }
    
    private void checkSchemaField(Map<String, RecordSchemaField> fields, String name
            , RecordSchemaFieldType type) 
        throws Exception 
    {
        RecordSchemaField field = fields.get(name);
        if (field==null)
            throw new Exception("Schema does not contains field - "+name);
        if (!type.equals(field.getFieldType()))
            throw new Exception(String.format(
                    "Invalid type of the field (%s). Expected (%s) but was (%s)"
                    , name, type, field.getFieldType()));
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("DataSource not supports pull operations");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
    
    public void bridgeActivated(IvrConversationsBridge bridge) {
        DataContext context = new DataContextImpl();
        if (!applyFilter(bridge, context)) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Recording for bridge ({}) where filtered", bridge);
            return;
        }
        try {
            File file = generateRecordFile(bridge, context);
            final Recorder recorder = new Recorder(bridge, codecManager, file, recordSchema
                    , noiseLevel, maxGainCoef, context);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug(logMess(bridge, "Recorder created. Recorording to the file (%s)", file));
            String mess = "Recording conversation from bridge: "+bridge;
            executor.execute(new AbstractTask(this, mess) {
                @Override public void doRun() throws Exception {
                    recorder.startRecording();
                }
            });
            recorders.put(bridge, recorder);
        } catch (Exception e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(logMess(bridge, "Error creating recorder"), e);
        }
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) {
        final Recorder recorder = recorders.get(bridge);
        if (recorder==null)
            return;
        try {
            executor.execute(new AbstractTask(this, logMess(bridge, "Handling stream substitution")) {
                @Override public void doRun() throws Exception {
                    recorder.handleStreamSubstitution();
                }
            });
        } catch (Exception e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(logMess(bridge, "Error handling stream substitution"), e);
        }
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
        final Recorder recorder = recorders.remove(bridge);
        try {
            if (recorder!=null) {
                executor.execute(new AbstractTask(this, logMess(bridge, "Finishing recording")) {
                    @Override public void doRun() throws Exception {
                        recorder.stopRecording();
                    }
                });
            }
        } catch (ExecutorServiceException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(logMess(bridge, "Error stop recording"), e);
        }
    }
    
    private boolean applyFilter(IvrConversationsBridge bridge, DataContext context) {
        try {
            createBindings(bridge, context);
            Boolean res = filterExpression;
            return res==null || !res? false : true;
        } finally {
            bindingSupport.reset();
        }
    }
    
    private File generateRecordFile(IvrConversationsBridge bridge, DataContext context) throws Exception {
        Date date = new Date();
        File dir = new File(baseDirFile, 
                new SimpleDateFormat("yyyy.MM").format(date) + File.separator
                + new SimpleDateFormat("dd").format(date));
        if (!dir.exists())
            dir.mkdirs();
        if (!dir.exists())
            throw new Exception(String.format("Can't create directory (%s)", dir));
        try {
            createBindings(bridge, context);
            bindingSupport.put(PATH_BINDING, dir.getAbsolutePath());
            bindingSupport.put(TIME_BINDING, new SimpleDateFormat("HHmmss_S").format(date));
            bindingSupport.put(DATE_BINDING, new SimpleDateFormat("yyyyMMdd").format(date));
            String filename = fileNameExpression;
            if (filename==null || filename.isEmpty())
                throw new Exception("Error generating file name for recording, fileNameExpression "
                        + "return null or empty string");
            return new File(dir, filename);
        } finally {
            bindingSupport.reset();
        }
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(1);
        TableImpl tab = new TableImpl(new String[]{callInfoMessage, recordStartTimeMessage
                , recordDurationMessage, recordFileMessage});
        List<Recorder> recordersList = new ArrayList<Recorder>(recorders.values());
        Collections.sort(recordersList);
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        long curTime = System.currentTimeMillis();
        for (Recorder rec: recordersList)
            tab.addRow(new Object[]{
                rec.bridge.toString(), 
                fmt.format(new Date(rec.recordingStartTime)),
                (curTime-rec.recordingStartTime)/1000,
                rec.file.toString()
                });
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, tab));
        return vos;
    }

    private void createBindings(IvrConversationsBridge bridge, DataContext context) {
        bindingSupport.put(CONV1_NUMA, bridge.getConversation1().getCallingNumber());
        bindingSupport.put(CONV1_NUMB, bridge.getConversation1().getCalledNumber());
        bindingSupport.put(CONV2_NUMA, bridge.getConversation2().getCallingNumber());
        bindingSupport.put(CONV2_NUMB, bridge.getConversation2().getCalledNumber());
        bindingSupport.put(CONV1_BINDING, bridge.getConversation1());
        bindingSupport.put(CONV2_BINDING, bridge.getConversation2());
        bindingSupport.put(CONV1_BINDINGS, bridge.getConversation1().getConversationScenarioState().getBindings());
        bindingSupport.put(CONV2_BINDINGS, bridge.getConversation2().getConversationScenarioState().getBindings());
        bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public void executeScheduledJob(Scheduler scheduler) {
        Integer days = keepRecordsForDays;
        if (days==null || days<=0) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Can't clean up old call records because of keepRecordsForDays "
                        + "is null or has invalid value");
            return;
        }
        File[] dirs = baseDirFile.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1*days);
        int yearMonth = new Integer(new SimpleDateFormat("yyyyMM").format(c.getTime()));
        int day = c.get(Calendar.DATE);
        if (dirs!=null)
            for (File dir: dirs) {
                try {
                    String elems[] = dir.getName().split("\\.");
                    if (elems==null || elems.length!=2)
                        throw new Exception(String.format(
                                "Invalid directory name (%s) must have a format (yyyy.MM)"
                                , dir.getName()));
                    int dirYearMonth = new Integer(elems[0]+elems[1]);
                    if (dirYearMonth<yearMonth) 
                        deleteDir(dir);
                    else if (dirYearMonth==yearMonth) {
                        File[] dayDirs = dir.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
                        if (dayDirs!=null)
                            for (File dayDir: dayDirs) {
                                int dirDay = new Integer(dayDir.getName());
                                if (dirDay<day)
                                    deleteDir(dayDir);
                            }
                    }
                } catch (Throwable e) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error(String.format(
                                "Error analyzing (%s) directory for old calls", dir.getName()), e);
                }
            }
    }
    
    private void deleteDir(File dir) throws IOException {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Deleting directory ({})", dir);
        FileUtils.forceDelete(dir);
    }
    
    private String logMess(IvrConversationsBridge bridge, String mess, Object... args) {
        return (bridge==null? "" : bridge.toString())+"Recorder. "+String.format(mess, args);
    }

    private String logMess(String mess, Object... args) {
        return logMess(null, mess, args);
    }

    public Double getMaxGainCoef() {
        return maxGainCoef;
    }

    public void setMaxGainCoef(Double maxGainCoef) {
        this.maxGainCoef = maxGainCoef;
    }

    public Integer getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(Integer noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Scheduler getCleanupScheduler() {
        return cleanupScheduler;
    }

    public void setCleanupScheduler(Scheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
    }

    public Integer getKeepRecordsForDays() {
        return keepRecordsForDays;
    }

    public void setKeepRecordsForDays(Integer keepRecordsForDays) {
        this.keepRecordsForDays = keepRecordsForDays;
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public IvrConversationsBridgeManager getConversationBridgeManager() {
        return conversationBridgeManager;
    }

    public void setConversationBridgeManager(IvrConversationsBridgeManager conversationBridgeManager) {
        this.conversationBridgeManager = conversationBridgeManager;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public String getFileNameExpression() {
        return fileNameExpression;
    }

    public void setFileNameExpression(String fileNameExpression) {
        this.fileNameExpression = fileNameExpression;
    }

    public Boolean getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(Boolean filterExpression) {
        this.filterExpression = filterExpression;
    }
    
    private class Recorder implements IncomingRtpStreamDataSourceListener, Comparable<Recorder> {
        
        private final IvrConversationsBridge bridge;
        private final long recordingStartTime = System.currentTimeMillis();
        private final RealTimeMixer merger;
        private final AudioFileWriterDataSource fileWriter;
        private final File file;
        private final RecordSchemaNode schema;
        private final DataContext context;
        
        private volatile boolean stopped = false;
        private volatile IncomingRtpStream inRtp1;
        private volatile IncomingRtpStream inRtp2;

        public Recorder(IvrConversationsBridge bridge, CodecManager codecManager, File file
                , RecordSchemaNode schema, int noiseLevel, double maxGainCoef, DataContext context) 
            throws FileWriterDataSourceException 
        {
            this.bridge = bridge;
            this.file = file;
            this.schema = schema;
            this.context = context;
            merger = new RealTimeMixer(codecManager, CallRecorderNode.this
                    , logMess(bridge, ""), executor, noiseLevel, maxGainCoef);
            fileWriter = new AudioFileWriterDataSource(CallRecorderNode.this, file, merger, codecManager
                    , FileTypeDescriptor.WAVE, logMess(bridge, ""));
        }
        
        public synchronized void startRecording() {
            if (stopped)
                return;
            try {
                inRtp1 = bridge.getConversation1().getIncomingRtpStream();
                inRtp2 = bridge.getConversation2().getIncomingRtpStream();
                inRtp1.addDataSourceListener(this, null);
                inRtp2.addDataSourceListener(this, null);
                fileWriter.start();
            } catch (Exception ex) {
                stopped = true;
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(logMess(bridge, "Error starting writing to file (%s)", file), ex);
                fileWriter.stop();
            }
        }
        
        public synchronized void stopRecording() {
            stopped = true;
            fileWriter.stop();
            createAndSendRecord();
        }
        
        private void createAndSendRecord() {
            if (schema==null)
                return;
            try {
                Record rec = schema.createRecord();
                rec.setValue(CONV1_NUMA, bridge.getConversation1().getCallingNumber());
                rec.setValue(CONV1_NUMB, bridge.getConversation1().getCalledNumber());
                rec.setValue(CONV2_NUMA, bridge.getConversation2().getCallingNumber());
                rec.setValue(CONV2_NUMB, bridge.getConversation2().getCalledNumber());
                rec.setValue(RECORDING_TIME, new Date(recordingStartTime));
                rec.setValue(RECORDING_DURATION, (System.currentTimeMillis()-recordingStartTime)/1000);
                rec.setValue(FILE, file.getPath());
                DataSourceHelper.sendDataToConsumers(CallRecorderNode.this, rec, context);
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error while generating and sending record to consumers", e);
            }
        }
        
        public synchronized void handleStreamSubstitution() {
            try {
                if (inRtp1 != bridge.getConversation1().getIncomingRtpStream()) {
                    inRtp1 = bridge.getConversation1().getIncomingRtpStream();
                    inRtp1.addDataSourceListener(this, null);
                }
                if (inRtp2 != bridge.getConversation2().getIncomingRtpStream()) {
                    inRtp2 = bridge.getConversation2().getIncomingRtpStream();
                    inRtp2.addDataSourceListener(this, null);
                }
            } catch (RtpStreamException e) {
                stopped = true;
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(logMess(bridge, "Error handling stream substitution"), e);
                fileWriter.stop();
            }
        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
            try {
                if (!stopped)
                    merger.addDataSource((PushBufferDataSource)dataSource);
            } catch (Exception e) {
                stopped = true;
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(logMess(bridge, "Error starting writing to file (%s)", file), e);
                fileWriter.stop();
            }
        }

        public void streamClosing(IncomingRtpStream stream) {
        }

        public int compareTo(Recorder o) {
            return new Long(recordingStartTime).compareTo(o.recordingStartTime);
        }
    }
}
