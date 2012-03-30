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
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.processing.Processor;
import javax.media.DataSink;
import javax.script.Bindings;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class CallRecorderNode extends BaseNode implements IvrConversationsBridgeListener {
    
    @Service
    protected static CodecManager codecManager;
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationBridgeManager;
    
    @NotNull @Parameter
    private String baseDir;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean filterExpression;
    
    @NotNull @Parameter(defaultValue="\"${c2NumB}_${c1NumA}_${time}.wav\"")
    private String fileNameExpression;
    
    @Parameter
    private RecordSchemaNode recordSchema;
    
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
        final Recorder recorder = new Recorder(bridge);
        try {
            String mess = "Recording conversation from bridge: "+bridge;
            executor.execute(new AbstractTask(this, mess) {
                @Override public void doRun() throws Exception {
                    recorder.startRecording();
                }
            });
            recorders.put(bridge, recorder);
        } catch (ExecutorServiceException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(String.format(
                        "Error creating RECORDER for conversation bridge: %s", bridge)
                        , e);
        }
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
        Recorder recorder = recorders.remove(bridge);
        if (recorder!=null) {
            
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
            bindingSupport.put("time", new SimpleDateFormat("HHmmss_S"));
            String filename = fileNameExpression;
            if (filename==null || filename.isEmpty())
                throw new Exception("Error generating file name for recording, fileNameExpression "
                        + "return null or empty string");
            return new File(dir, filename);
        } finally {
            bindingSupport.reset();
        }
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
    
    private class Recorder {
        
        private final IvrConversationsBridge bridge;
        private final long recordStartTime = System.currentTimeMillis();
        private final RealTimeDataSourceMerger merger;
        
        private volatile String statusMessage;
        private volatile boolean stopped = false;
        private volatile Processor processor = null;
        private volatile DataSink dataSink = null;

        public Recorder(IvrConversationsBridge bridge) {
            this.bridge = bridge;
            merger = new RealTimeDataSourceMerger(codecManager, CallRecorderNode.this, null, executor);
        }
        
        public void startRecording() throws ExecutorServiceException {
            statusMessage = "Recording conversation from bridge: "+bridge;
        }
        
        public void stopRecording() {
            
        }
        
        public void handleStreamSubstitution() {
            
        }

    }
}
