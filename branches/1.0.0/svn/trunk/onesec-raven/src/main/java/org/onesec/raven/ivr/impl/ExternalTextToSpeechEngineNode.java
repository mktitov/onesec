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
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.Bindings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class ExternalTextToSpeechEngineNode extends AbstractDataSource implements Viewable {
    
    public final static String TEXT_ATTR = "textToSpeech";
    public final static String TEXT_ATTR_DESC_KEY = "textToSpeechAttrDescription";
    public final static String TEXT_FILENAME_BINDING = "textFilename";
    public final static String WAV_FILENAME_BINDING = "wavFilename";
    
    public final static long PROCESS_EXECUTION_TIMEOUT = 5000;
    
    @Service
    private static MessagesRegistry messReg;
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String commandLine;
    @NotNull @Parameter(defaultValue="utf-8")
    private Charset textFileEncoding;
    @NotNull @Parameter
    private String cacheDir;
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    @NotNull @Parameter(defaultValue="utf-8")
    private Charset commandStreamsEncoding;
    @NotNull @Parameter(defaultValue="5000")
    private Long commandExecutionTimeout;
    
    @Message private static String textColumnMessage;
    @Message private static String wavFileColumnMessage;
    @Message private static String tableHeaderMessage;
    
    private Map<String, FileInputStreamSource> cache;
    private BindingSupportImpl bindingSupport;
    private volatile long executionTime;
    private volatile long operationsCount;
    private AtomicInteger cacheHitCount;
    private AtomicInteger errorsCount;

    @Override
    protected void initFields() {
        super.initFields();
        cache = new ConcurrentHashMap<String, FileInputStreamSource>();
        bindingSupport = new BindingSupportImpl();
        resetStat();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        resetStat();
        readCache();
    }
    
    private void resetStat() {
        executionTime = 0;
        operationsCount = 0;
        cacheHitCount = new AtomicInteger();
        errorsCount = new AtomicInteger();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        cache.clear();
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context) throws Exception {
        String text = context.getSessionAttributes().get(TEXT_ATTR).getValue();
        FileInputStreamSource source = cache.get(text);
        if (source!=null) {
            cacheHitCount.incrementAndGet();
            dataConsumer.setData(this, source, context);
            return true;
        }
        File textFile = File.createTempFile("audio_", ".txt", new File(cacheDir));
        FileUtils.writeStringToFile(textFile, text, textFileEncoding.name());
        String wavFileName = FilenameUtils.removeExtension(textFile.getAbsolutePath())+".wav";
        String _commandLine = null;
        try {
            bindingSupport.put(TEXT_FILENAME_BINDING, textFile.getAbsolutePath());
            bindingSupport.put(WAV_FILENAME_BINDING, wavFileName);
            bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
            bindingSupport.put(BindingNames.REQUESTER_BINDING, dataConsumer);
            _commandLine = commandLine;
        } finally {
            bindingSupport.reset();
        }
        if (!executeProcess(_commandLine, dataConsumer))
            dataConsumer.setData(this, null, context);
        else {
            File wavFile = new File(wavFileName);
            if (!wavFile.exists()) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("WAV file ({}) not created after commans execution ({})"
                            , wavFileName, _commandLine);
                dataConsumer.setData(this, null, context);
            } else {
                source = new FileInputStreamSource(wavFile, this);
                cache.put(text, source);
                dataConsumer.setData(this, source, context);
            }
        }
        return true;
    }
    
    private boolean executeProcess(String commandLine, DataConsumer dataConsumer) {
        long ts = System.currentTimeMillis();
        try {
            try {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("Executing command: {}", commandLine);
                Process proc = Runtime.getRuntime().exec(commandLine);
                ProcessInfo procInfo = new ProcessInfo(proc, commandLine);
                runProcessMonitor(procInfo);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    executor.executeQuietly(new StreamLoggerTask("error", proc.getErrorStream(), false));
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    executor.executeQuietly(new StreamLoggerTask("output", proc.getInputStream(), true));
                int res = proc.waitFor();
                if (res!=0) {
                    errorsCount.incrementAndGet();
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error("The process return NON ZERO status code - ({})", res);
                    return false;
                } 
                return true;
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error executing process ({})", commandLine);
                return false;
            }
        } finally {
            addStat(ts);
        }
    }
    
    private void runProcessMonitor(final ProcessInfo process) {
        executor.executeQuietly(commandExecutionTimeout, new AbstractTask(this, "Extrenal process execution monitor") {
            @Override public void doRun() throws Exception {
                if (!process.finished) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error("Process execution timeout ({}). Killing...", process.commandLine);
                    process.process.destroy();
                }
            }
        });
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    private synchronized void addStat(long startTime) {
        executionTime += System.currentTimeMillis()-startTime;
        ++operationsCount;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
        NodeAttributeImpl textAttr = new NodeAttributeImpl(TEXT_ATTR, String.class, null, null);
        MessageComposer comp = new MessageComposer(messReg)
                .append(messReg.createMessageKeyForStringValue(
                    getClass().getCanonicalName(), TEXT_ATTR_DESC_KEY));
        textAttr.setDescriptionContainer(comp);
        textAttr.setRequired(true);
        consumerAttributes.add(textAttr);
    }
    
    private void readCache() throws Exception {
        File cacheFile = new File(cacheDir);
        if (!cacheFile.exists() || !cacheFile.isDirectory() || !cacheFile.canRead() || !cacheFile.canWrite())
            throw new Exception(String.format("Not valid path for cache (%s). It's not a directory or "
                    + "file not exists or can't read/write to this directory", cacheDir));
        File[] textFiles = cacheFile.listFiles((FilenameFilter)new SuffixFileFilter(".txt"));
        if (textFiles!=null && textFiles.length>0)
            for (File textFile: textFiles) {
                String text = FileUtils.readFileToString(textFile, textFileEncoding.name());
                String wavFileName = FilenameUtils.removeExtension(textFile.getPath())+".wav";
                File wavFile = new File(wavFileName);
                if (!(wavFile.exists())) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error("Found text file ({}) but not found WAV file ({}). "
                                + "Removing invalid cached entry", textFile.getPath(), wavFileName);
                    textFile.delete();
                } else 
                    cache.put(text, new FileInputStreamSource(wavFile, this));
            }
    }
    
    @Parameter(readOnly=true)
    public Long getTotalExecutionTimeMS() {
        return executionTime;
    }
    
    @Parameter(readOnly=true)
    public Long getAvgExecutionTimeMS() {
        return operationsCount==0? 0 : executionTime/operationsCount;
    }
    
    @Parameter(readOnly=true)
    public Long getOperationsCount() {
        return operationsCount;
    }
    
    @Parameter(readOnly=true)
    public Integer getCacheHitCount() {
        return cacheHitCount.get();
    }
    
    @Parameter(readOnly=true)
    public Long getCacheHitPercent() {
        return operationsCount==0? 0 : 100*cacheHitCount.get()/operationsCount;
    }
    
    @Parameter(readOnly=true)
    public Integer getErrorsCount() {
        return errorsCount.get();
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
        List<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        vos.add(new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, tableHeaderMessage));
        TableImpl table = new TableImpl(new String[]{textColumnMessage, wavFileColumnMessage});
        for (Map.Entry entry: cache.entrySet())
            table.addRow(new Object[]{entry.getKey(), entry.getValue().toString()});
        vos.add(new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, table));
        return vos;
    }

    public Long getCommandExecutionTimeout() {
        return commandExecutionTimeout;
    }

    public void setCommandExecutionTimeout(Long commandExecutionTimeout) {
        this.commandExecutionTimeout = commandExecutionTimeout;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
    
    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public Charset getTextFileEncoding() {
        return textFileEncoding;
    }

    public void setTextFileEncoding(Charset textFileEncoding) {
        this.textFileEncoding = textFileEncoding;
    }

    public Charset getCommandStreamsEncoding() {
        return commandStreamsEncoding;
    }

    public void setCommandStreamsEncoding(Charset commandStreamsEncoding) {
        this.commandStreamsEncoding = commandStreamsEncoding;
    }
    
    private class ProcessInfo {
        private final Process process;
        private final String commandLine;
        private volatile boolean finished = false;

        public ProcessInfo(Process process, String commandLine) {
            this.process = process;
            this.commandLine = commandLine;
        }
    }
    
    private class StreamLoggerTask implements Task {
        
        private final String streamName;
        private final InputStream stream;
        private final String statusMessage;
        private final boolean debugLevel;

        public StreamLoggerTask(String streamName, InputStream stream, boolean debugLevel) {
            this.streamName = streamName;
            this.stream = stream;
            this.debugLevel = debugLevel;
            this.statusMessage = "Logging "+streamName+" stream";
        }

        public Node getTaskNode() {
            return ExternalTextToSpeechEngineNode.this;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void run() {
            try {
                try {
                    LineIterator it = IOUtils.lineIterator(stream, commandStreamsEncoding.name());
                    int lineNum = 1;
                    while (it.hasNext())
                        if (debugLevel)
                            getLogger().debug("[{}] {}", lineNum++, it.next());
                        else
                            getLogger().error("[{}] {}", lineNum++, it.next());
                } catch (Throwable ex) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error(String.format("Error logging (%s) stream", streamName), ex);
                }
            } finally  {
                IOUtils.closeQuietly(stream);
            }
        }
    }
}
