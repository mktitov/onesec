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
package org.onesec.raven.ivr.actions;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.script.Bindings;
import org.onesec.raven.ivr.CallRecorder;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.ivr.impl.AudioFileWriterDataSource;
import org.onesec.raven.ivr.impl.RealTimeMixer;
import org.raven.BindingNames;
import org.raven.RavenUtils;
import org.raven.cache.TemporaryFileManager;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class StartRecordingAction extends AbstractAction {
    public final static String NAME = "Start recording action";
    public final static String RECORDER_BINDING = "CallRecorder";
    public final static String CONVERSATION_BINDING = "convBindings";
    
    @Service
    private static CodecManager codecManager;
    
    private final StartRecordingActionNode actionNode;
    private final boolean saveOnCancel;

    public StartRecordingAction(StartRecordingActionNode owner, boolean saveOnCancel) {
        super(NAME);
        this.actionNode = owner;
        this.saveOnCancel = saveOnCancel;
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        final IvrEndpointConversation conversation = message.getConversation();
        ConversationScenarioState state = conversation.getConversationScenarioState();
        DataContext context = (DataContext) state.getBindings().get(BindingNames.DATA_CONTEXT_BINDING);
        stopExistingRecorder(state, conversation);
        Recorder recorder = new Recorder(context, codecManager, conversation, getLogger());
        initRecorder(recorder, conversation.getExecutorService());
        return ACTION_EXECUTED_then_EXECUTE_NEXT;
    }
    
    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }

    private void initRecorder(final Recorder recorder, ExecutorService executor) {
        executor.executeQuietly(new AbstractTask(actionNode, "Initializing recorder") {
            @Override public void doRun() throws Exception {
                recorder.init();
            }
        });
    }

    private void stopExistingRecorder(ConversationScenarioState state, IvrEndpointConversation conversation) {
        final Recorder oldRecorder = (Recorder) state.getBindings().get(RECORDER_BINDING);
        if (oldRecorder!=null) {
            if (getLogger().isWarnEnabled())
                getLogger().warn("Found existing recorder! Stopping and replacing it");
            conversation.getExecutorService().executeQuietly(new AbstractTask(actionNode, "Stopping recording") {
                @Override public void doRun() throws Exception {
                    oldRecorder.stopRecording(true);
                }
            });
        }
    }
//    @Override
//    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
//        ConversationScenarioState state = conversation.getConversationScenarioState();
//        DataContext context = (DataContext) state.getBindings().get(BindingNames.DATA_CONTEXT_BINDING);
//        stopExistingRecorder(state, conversation);
//        Recorder recorder = new Recorder(context, codecManager, conversation);
//        initRecorder(recorder, conversation.getExecutorService());
//    }

    
    class Recorder implements IncomingRtpStreamDataSourceListener, CallRecorder {
        private final DataContext context;
        private final TemporaryFileManager tempFileManager;
        private final IncomingRtpStream inRtp;
        private final RealTimeMixer merger;
        private final AudioFileWriterDataSource writer;
//        private final Node loggerNode;
        private final String key;
        private final Bindings conversationBindings;
        private final IvrEndpointConversation conversation;
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        private final LoggerHelper logger;

        public Recorder(DataContext context, CodecManager codecManager, IvrEndpointConversation conversation, LoggerHelper logger) 
                throws Exception
        {
            this.logger = new LoggerHelper(logger, "Recorder. ");
            this.context = context!=null? context : new DataContextImpl();
            this.context.putAt(CONVERSATION_BINDING, conversation.getConversationScenarioState().getBindings());
            this.tempFileManager = actionNode.getTemporaryFileManager();
            this.inRtp = conversation.getIncomingRtpStream();
//            this.loggerNode = conversation.getOwner();
            this.key = RavenUtils.generateUniqKey("recording");
            this.conversationBindings = conversation.getConversationScenarioState().getBindings();
            this.conversation = conversation;
//            String logPrefix = logMess("");
            File file = tempFileManager.createFile(actionNode, key, "audio/wav");
            merger = new RealTimeMixer(codecManager, conversation.getOwner(), logger
                    , conversation.getExecutorService()
                    , actionNode.getNoiseLevel(), actionNode.getMaxGainCoef());
            writer = new AudioFileWriterDataSource(file, merger, codecManager, FileTypeDescriptor.WAVE, logger);
        }
        
        public void init() throws RtpStreamException {
            if (logger.isDebugEnabled())
                logger.debug("Initializing recorder");
            if (!inRtp.addDataSourceListener(this, null)) {
                stopped.set(true);
                conversationBindings.remove(RECORDER_BINDING);
            } else
                conversation.getConversationScenarioState().setBinding(RECORDER_BINDING, this
                        , BindingScope.CONVERSATION);

        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
            if (logger.isDebugEnabled())
                logger.debug("Starting recorder");
            try {
                writer.start();
                merger.addDataSource((PushBufferDataSource)dataSource);
            } catch (Throwable e) {
                if (logger.isErrorEnabled())
                    logger.error("Error starting recording", e);
                stopRecording(true);
            }
        }

        @Override
        public void streamClosing(IncomingRtpStream stream) {
            stopRecording(true);
        }
        
        @Override
        public void stopRecording(boolean cancel) {
            if (!stopped.compareAndSet(false, true))
                return;
            if (logger.isDebugEnabled())
                logger.debug("Stopping recorder. Cancel flag == {}", cancel);
            try {
                writer.stop();
            } finally {
                conversationBindings.remove(RECORDER_BINDING);
            }
            if (!cancel || saveOnCancel) 
                actionNode.sendDataToConsumers(tempFileManager.getDataSource(key), context);
        }
    }

}
