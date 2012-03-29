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

import java.util.concurrent.ConcurrentHashMap;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class CallRecorderNode extends BaseNode implements IvrConversationsBridgeListener {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationBridgeManager;
    
    @NotNull @Parameter
    private String baseDir;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean filterExpression;
    
    @NotNull @Parameter(defaultValue="")
    private String fileMaskExpression;
    
    @Parameter
    private RecordSchemaNode recordSchema;
    
    private ConcurrentHashMap<IvrConversationsBridge, Recorder> recorders;

    @Override
    protected void initFields() {
        super.initFields();
        recorders = new ConcurrentHashMap<IvrConversationsBridge, Recorder>();
    }
    
    public void bridgeActivated(IvrConversationsBridge bridge) {
        final Recorder recorder = new Recorder(bridge);
        try {
            executor.execute(new AbstractTask(this, "Starting recording bridge conversation") {
                @Override public void doRun() throws Exception {
                    recorder.startRecording();
                }
            });
            recorders.put(bridge, recorder);
        } catch (ExecutorServiceException e) {
            
        }
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
        throw new UnsupportedOperationException("Not supported yet.");
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

    public String getFileMaskExpression() {
        return fileMaskExpression;
    }

    public void setFileMaskExpression(String fileMaskExpression) {
        this.fileMaskExpression = fileMaskExpression;
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

        public Recorder(IvrConversationsBridge bridge) {
            this.bridge = bridge;
        }
        
        public void startRecording() {
            
        }
        
        public void stopRecording() {
            
        }
        
        public void handleStreamSubstitution() {
            
        }
    }
}
