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
import javax.activation.DataSource;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.script.Bindings;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.TestSchedulerNode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.cache.TemporaryFileManagerNode;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.internal.MocksControl;
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
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class StartRecordingActionTest extends OnesecRavenTestCase {
    private final static Logger logger = LoggerFactory.getLogger(StartRecordingActionTest.class);
    
    private ExecutorServiceNode executor;
    private StartRecordingActionNode actionNode;
    private TemporaryFileManagerNode tempFileManager;
    private TestSchedulerNode scheduler;
    private CodecManager codecManager;
    private BufferCache bufferCache;
    private DataCollector collector;
    private static StartRecordingAction.Recorder recorder;
    
    @Before
    public void prepare() {
        codecManager = registry.getService(CodecManager.class);
        RTPManagerServiceImpl rtpManagerService = new RTPManagerServiceImpl(logger, codecManager);
        bufferCache = new BufferCacheImpl(rtpManagerService, logger, codecManager);
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(10);
        executor.setMaximumPoolSize(10);
        assertTrue(executor.start());
        
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
    public void test() throws Exception {
        MocksControl control = new MocksControl(MocksControl.MockType.NICE);
        IvrEndpointConversation conv = control.createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings bindings = control.createMock(Bindings.class);
        IncomingRtpStream inRtp = control.createMock(IncomingRtpStream.class);
        
        expect(conv.getConversationScenarioState()).andReturn(state).atLeastOnce();
        expect(state.getBindings()).andReturn(bindings).atLeastOnce();
        expect(bindings.get(BindingNames.DATA_CONTEXT_BINDING)).andReturn(null);
        expect(bindings.get(StartRecordingAction.RECORDER_BINDING)).andReturn(null);
        expect(conv.getOwner()).andReturn(actionNode);
        expect(conv.getExecutorService()).andReturn(executor).atLeastOnce();
        expect(conv.getIncomingRtpStream()).andReturn(inRtp);
        expect(inRtp.addDataSourceListener(checkRtpListener(codecManager, executor, actionNode, bufferCache)
                , isNull(AudioFormat.class))).andReturn(true);
        state.setBinding(eq(StartRecordingAction.RECORDER_BINDING), isA(Recorder.class), eq(BindingScope.CONVERSATION));
        expect(bindings.remove(StartRecordingAction.RECORDER_BINDING)).andReturn(null);
        
        control.replay();
        
        StartRecordingAction action = (StartRecordingAction) actionNode.createAction();
        assertNotNull(action);
        action.doExecute(conv);
        Thread.sleep(1000);
        assertNotNull(recorder);
        recorder.stopRecording(false);
        
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);
        DataSource ds = (DataSource) collector.getDataList().get(0);
        FileOutputStream out = new FileOutputStream("target/test.wav");
        IOUtils.copy(ds.getInputStream(), out);
        out.close();
//        while (action.)
        control.verify();
    }
    
    public static IncomingRtpStreamDataSourceListener checkRtpListener(final CodecManager codecManager
            , final ExecutorService executor, final Node actionNode, final BufferCache bufferCache) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(final Object arg) {
                executor.executeQuietly(new AbstractTask(actionNode, "datasource executor") {
                    @Override public void doRun() throws Exception {
                        try {
                            recorder = (Recorder) arg;
                            TestInputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
                            ConcatDataSource dataSource = new ConcatDataSource(
                                    FileTypeDescriptor.WAVE, executor, codecManager, Codec.G711_MU_LAW, 240, 5
                                    , 5, actionNode, bufferCache, new LoggerHelper(actionNode, null));
                            IncomingRtpStreamDataSourceListener listener = (IncomingRtpStreamDataSourceListener) arg;
                            dataSource.start();
                            dataSource.addSource(source);
                            listener.dataSourceCreated(null, dataSource);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}
