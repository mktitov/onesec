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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.script.Bindings;
import org.onesec.raven.ivr.*;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
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
public class CallRecorderNode extends BaseNode implements IvrConversationsBridgeListener, Viewable {
    
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
    
    @Parameter
    private RecordSchemaNode recordSchema;
    
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
    }
    
    public void bridgeActivated(IvrConversationsBridge bridge) {
        if (!applyFilter(bridge)) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Recording for bridge ({}) where filtered", bridge);
            return;
        }
        try {
            File file = generateRecordFile(bridge);
            final Recorder recorder = new Recorder(bridge, codecManager, file);
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
    
    private boolean applyFilter(IvrConversationsBridge bridge) {
        try {
            createBindings(bridge);
            Boolean res = filterExpression;
            return res==null || !res? false : true;
        } finally {
            bindingSupport.reset();
        }
    }
    
    private File generateRecordFile(IvrConversationsBridge bridge) throws Exception {
        Date date = new Date();
        File dir = new File(baseDirFile, 
                new SimpleDateFormat("yyyy.MM").format(date) + File.separator
                + new SimpleDateFormat("dd").format(date));
        if (!dir.exists())
            dir.mkdirs();
        if (!dir.exists())
            throw new Exception(String.format("Can't create directory (%s)", dir));
        try {
            createBindings(bridge);
            bindingSupport.put("time", new SimpleDateFormat("HHmmss_S").format(date));
            bindingSupport.put("date", new SimpleDateFormat("yyyyMMdd").format(date));
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
                fmt.format(new Date(rec.recordStartTime)),
                (curTime-rec.recordStartTime)/1000,
                rec.file.toString()
                });
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, tab));
        return vos;
    }

    private void createBindings(IvrConversationsBridge bridge) {
        bindingSupport.put("c1NumA", bridge.getConversation1().getCallingNumber());
        bindingSupport.put("c1NumB", bridge.getConversation1().getCalledNumber());
        bindingSupport.put("c2NumA", bridge.getConversation2().getCallingNumber());
        bindingSupport.put("c2NumB", bridge.getConversation2().getCalledNumber());
        bindingSupport.put("conversation1", bridge.getConversation1());
        bindingSupport.put("conversation2", bridge.getConversation2());
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    private String logMess(IvrConversationsBridge bridge, String mess, Object... args) {
        return (bridge==null? "" : bridge.toString())+"Recorder. "+String.format(mess, args);
    }

    private String logMess(String mess, Object... args) {
        return logMess(null, mess, args);
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
        private final long recordStartTime = System.currentTimeMillis();
        private final RealTimeDataSourceMerger merger;
        private final AudioFileWriterDataSource fileWriter;
        private final File file;
        
        private volatile boolean stopped = false;
        private volatile IncomingRtpStream inRtp1;
        private volatile IncomingRtpStream inRtp2;

        public Recorder(IvrConversationsBridge bridge, CodecManager codecManager, File file) 
                throws FileWriterDataSourceException 
        {
            this.bridge = bridge;
            this.file = file;
            merger = new RealTimeDataSourceMerger(codecManager, CallRecorderNode.this
                    , logMess(bridge, ""), executor);
            fileWriter = new AudioFileWriterDataSource(CallRecorderNode.this, file, merger, codecManager
                    , FileTypeDescriptor.WAVE, logMess(bridge, ""));
        }
        
        public void startRecording() {
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
        
        public void stopRecording() {
            stopped = true;
            fileWriter.stop();
        }
        
        public void handleStreamSubstitution() {
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
            return new Long(recordStartTime).compareTo(o.recordStartTime);
        }
    }
}
