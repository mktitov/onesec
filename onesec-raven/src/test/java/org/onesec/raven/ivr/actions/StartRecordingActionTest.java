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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import javax.activation.DataSource;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.TestSchedulerNode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.cache.TemporaryFileManagerNode;
import static org.easymock.EasyMock.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.actions.StartRecordingAction.Recorder;
import org.onesec.raven.ivr.impl.BufferCacheImpl;
import org.onesec.raven.ivr.impl.ConcatDataSource;
import org.onesec.raven.ivr.impl.RTPManagerServiceImpl;
import org.onesec.raven.ivr.impl.TestInputStreamSource;
import org.onesec.raven.ivr.impl.TickingDataSource;
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.log.LogLevel;
import org.raven.sched.impl.AbstractTask;
import org.raven.test.DataCollector;
import org.raven.test.TestDataProcessorFacade;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class StartRecordingActionTest extends ActionTestCase {

    private StartRecordingActionNode actionNode;
    private TemporaryFileManagerNode tempFileManager;
    private TestSchedulerNode scheduler;
    private CodecManager codecManager;
    private BufferCache bufferCache;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        codecManager = registry.getService(CodecManager.class);
        RTPManagerServiceImpl rtpManagerService = new RTPManagerServiceImpl(logger, codecManager);
        bufferCache = new BufferCacheImpl(rtpManagerService, logger, codecManager);
        
        scheduler = new TestSchedulerNode();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        tempFileManager = new TemporaryFileManagerNode();
        tempFileManager.setName("Temporary file manager");
        tree.getRootNode().addAndSaveChildren(tempFileManager);
        tempFileManager.setDirectory("target/raven_temp");
        tempFileManager.setForceCreateDirectory(Boolean.TRUE);
        tempFileManager.setScheduler(scheduler);
        assertTrue(tempFileManager.start());
        
        actionNode = new StartRecordingActionNode();
        actionNode.setName("start recording action node");
        tree.getRootNode().addAndSaveChildren(actionNode);
        actionNode.setTemporaryFileManager(tempFileManager);
        actionNode.setLogLevel(LogLevel.TRACE);
        assertTrue(actionNode.start());
        
        collector = new DataCollector();
        collector.setName("data collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(actionNode);
        assertTrue(collector.start());
    }
    
    @Test
    public void test(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final IncomingRtpStream inRtp,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        final AtomicReference<Recorder> recorder = new AtomicReference<>();
        new Expectations() {{
            conv.getExecutorService(); result = executor;
            conv.getIncomingRtpStream(); result = inRtp;
            bindings.get(BindingNames.DATA_CONTEXT_BINDING); result = null;
            bindings.get(StartRecordingAction.RECORDER_BINDING); result = null;
            state.setBinding(StartRecordingAction.RECORDER_BINDING, withInstanceOf(Recorder.class), BindingScope.CONVERSATION);
            inRtp.addDataSourceListener((IncomingRtpStreamDataSourceListener) anyObject(), (AudioFormat) withNull()); result = new Delegate() {
                boolean addDataSourceListener(final IncomingRtpStreamDataSourceListener listener, AudioFormat format) {
                    executor.executeQuietly(new AbstractTask(actionNode, "datasource executor") {
                        @Override public void doRun() throws Exception {
                            try {
                                recorder.set((Recorder) listener);
                                TestInputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
                                ConcatDataSource dataSource = new ConcatDataSource(
                                        FileTypeDescriptor.WAVE, executor, codecManager, Codec.G711_MU_LAW, 240, 5
                                        , 5, actionNode, bufferCache, new LoggerHelper(actionNode, null));
                                TickingDataSource tickingSource = new TickingDataSource(dataSource, 30, new LoggerHelper(logger, null));
                                
                                tickingSource.start();
                                dataSource.addSource(source);
                                listener.dataSourceCreated(null, tickingSource);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    return true;
                };
            };
            bindings.remove(StartRecordingAction.RECORDER_BINDING); result = null;
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        actionExecutor.sendTo(action, execMess);
        actionExecutor.waitForMessage(100);
        Thread.sleep(1000);
        assertNotNull(recorder.get());
        recorder.get().stopRecording(false);
        
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);
        DataSource ds = (DataSource) collector.getDataList().get(0);
        FileOutputStream out = new FileOutputStream("target/test.wav");
        IOUtils.copy(ds.getInputStream(), out);
        out.close();
        
        new Verifications() {{
            state.setBinding(StartRecordingAction.RECORDER_BINDING, withInstanceOf(Recorder.class), BindingScope.CONVERSATION);
        }};
    }
}
