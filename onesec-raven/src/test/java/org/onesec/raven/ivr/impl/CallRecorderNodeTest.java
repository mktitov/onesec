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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.apache.commons.io.FileUtils;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.*;
import org.raven.ds.Record;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import static org.onesec.raven.ivr.impl.CallRecordingRecordSchemaNode.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallRecorderNodeTest extends OnesecRavenTestCase {
    
    private ExecutorServiceNode executor;
    private CodecManager codecManager;
    private BaseNode owner;
    private DummyConversationsBridgeManager bridgeManager;
    
    @Before
    public void prepare() {
        try {
            FileUtils.forceDelete(new File("target/recs"));
        } catch (IOException ex) {
        }
        codecManager = registry.getService(CodecManager.class);
        
        owner = new BaseNode("owner");
        tree.getRootNode().addAndSaveChildren(owner);
        assertTrue(owner.start());
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(20);
        executor.setMaximumQueueSize(20);
        assertTrue(executor.start());
        
        bridgeManager = new DummyConversationsBridgeManager();
        bridgeManager.setName("bridge manager");
        tree.getRootNode().addAndSaveChildren(bridgeManager);
        assertTrue(bridgeManager.start());
    }
    
//    @Test
    public void filterTest() {
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        IvrEndpointConversation conv1 = createMock("conv1", IvrEndpointConversation.class);
        IvrEndpointConversation conv2 = createMock("conv2", IvrEndpointConversation.class);
        
        expect(bridge.getConversation1()).andReturn(conv1).atLeastOnce();
        expect(bridge.getConversation2()).andReturn(conv2).atLeastOnce();
        expect(conv1.getCallingNumber()).andReturn("c1n1").anyTimes();
        expect(conv1.getCalledNumber()).andReturn("c1n2").anyTimes();
        expect(conv2.getCallingNumber()).andReturn("c2n1").anyTimes();
        expect(conv2.getCalledNumber()).andReturn("c2n2").anyTimes();
        replay(bridge, conv1, conv2);
        
        CallRecorderNode recorder = new CallRecorderNode();
        recorder.setName("recorder");
        tree.getRootNode().addAndSaveChildren(recorder);
        recorder.setConversationBridgeManager(bridgeManager);
        recorder.setExecutor(executor);
        recorder.setBaseDir("target/recs");
        recorder.setLogLevel(LogLevel.TRACE);
        recorder.setFilterExpression(false);
        assertTrue(recorder.start());
        
        recorder.bridgeActivated(bridge);
        
        verify(bridge, conv1, conv2);
    }
    
//    @Test
    public void simpleTest() throws Exception {
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        IvrEndpointConversation conv1 = createMock("conv1", IvrEndpointConversation.class);
        IvrEndpointConversation conv2 = createMock("conv2", IvrEndpointConversation.class);
        IncomingRtpStream inRtp1 = createMock("inRtp1", IncomingRtpStream.class);
        IncomingRtpStream inRtp2 = createMock("inRtp2", IncomingRtpStream.class);
        
        expect(bridge.getConversation1()).andReturn(conv1).atLeastOnce();
        expect(bridge.getConversation2()).andReturn(conv2).atLeastOnce();
        expect(conv1.getCallingNumber()).andReturn("c1n1").anyTimes();
        expect(conv1.getCalledNumber()).andReturn("c1n2").anyTimes();
        expect(conv1.getIncomingRtpStream()).andReturn(inRtp1);
        expect(conv2.getCallingNumber()).andReturn("c2n1").anyTimes();
        expect(conv2.getCalledNumber()).andReturn("c2n2").anyTimes();
        expect(conv2.getIncomingRtpStream()).andReturn(inRtp2);
        PushBufferDataSource ds1 = createDataSourceFromFile("src/test/wav/test.wav");
        expect(inRtp1.addDataSourceListener(addDataSourceListener(ds1), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        PushBufferDataSource ds2 = createDataSourceFromFile("src/test/wav/test2.wav");
        expect(inRtp2.addDataSourceListener(addDataSourceListener(ds2), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        
        replay(bridge, conv1, conv2, inRtp1, inRtp2);
        CallRecorderNode recorder = new CallRecorderNode();
        recorder.setName("recorder");
        tree.getRootNode().addAndSaveChildren(recorder);
        recorder.setConversationBridgeManager(bridgeManager);
        recorder.setExecutor(executor);
        recorder.setBaseDir("target/recs");
        recorder.setLogLevel(LogLevel.TRACE);
        NodeAttribute attr = recorder.getNodeAttribute("filterExpression");
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("c1NumA=='c1n1' && c1NumB=='c1n2' && c2NumA=='c2n1' && c2NumB=='c2n2'");
        assertTrue(recorder.start());
        
        recorder.bridgeActivated(bridge);
        TimeUnit.SECONDS.sleep(4);
        recorder.bridgeDeactivated(bridge);
        TimeUnit.SECONDS.sleep(1);
        
        verify(bridge, conv1, conv2, inRtp1, inRtp2);
        
    }
    
//    @Test
    public void streamsReassingTest() throws Exception {
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        IvrEndpointConversation conv1 = createMock("conv1", IvrEndpointConversation.class);
        IvrEndpointConversation conv2 = createMock("conv2", IvrEndpointConversation.class);
        IncomingRtpStream inRtp1 = createMock("inRtp1", IncomingRtpStream.class);
        IncomingRtpStream inRtp11 = createMock("inRtp11", IncomingRtpStream.class);
        IncomingRtpStream inRtp2 = createMock("inRtp2", IncomingRtpStream.class);
        
        expect(bridge.getConversation1()).andReturn(conv1).atLeastOnce();
        expect(bridge.getConversation2()).andReturn(conv2).atLeastOnce();
        expect(conv1.getCallingNumber()).andReturn("c1n1").anyTimes();
        expect(conv1.getCalledNumber()).andReturn("c1n2").anyTimes();
        expect(conv1.getIncomingRtpStream()).andReturn(inRtp1);
        expect(conv2.getCallingNumber()).andReturn("c2n1").anyTimes();
        expect(conv2.getCalledNumber()).andReturn("c2n2").anyTimes();
        expect(conv2.getIncomingRtpStream()).andReturn(inRtp2);
        PushBufferDataSource ds1 = createDataSourceFromFile("src/test/wav/test.wav");
        expect(inRtp1.addDataSourceListener(addDataSourceListener(ds1), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        PushBufferDataSource ds2 = createDataSourceFromFile("src/test/wav/test2.wav");
        expect(inRtp2.addDataSourceListener(addDataSourceListener(ds2), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        //reasing
        expect(conv2.getIncomingRtpStream()).andReturn(inRtp2);
        PushBufferDataSource ds3 = createDataSourceFromFile("src/test/wav/greeting.wav");
        expect(conv1.getIncomingRtpStream()).andReturn(inRtp11).atLeastOnce();
        expect(inRtp11.addDataSourceListener(addDataSourceListener(ds3), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        
        replay(bridge, conv1, conv2, inRtp1, inRtp2, inRtp11);
        CallRecorderNode recorder = new CallRecorderNode();
        recorder.setName("recorder");
        tree.getRootNode().addAndSaveChildren(recorder);
        recorder.setConversationBridgeManager(bridgeManager);
        recorder.setExecutor(executor);
        recorder.setBaseDir("target/recs");
        recorder.setLogLevel(LogLevel.TRACE);
        recorder.setFileNameExpression("'stream_reassing_test.wav'");
        assertTrue(recorder.start());
        
        recorder.bridgeActivated(bridge);
        TimeUnit.SECONDS.sleep(1);
        recorder.bridgeReactivated(bridge);
        TimeUnit.SECONDS.sleep(4);
        recorder.bridgeDeactivated(bridge);
        TimeUnit.SECONDS.sleep(1);
        
        verify(bridge, conv1, conv2, inRtp1, inRtp2, inRtp11);
    }
 
//    @Test
    public void deleteOldRecordsTest() {
        CallRecorderNode recorder = new CallRecorderNode();
        recorder.setName("recorder");
        tree.getRootNode().addAndSaveChildren(recorder);
        recorder.setConversationBridgeManager(bridgeManager);
        recorder.setExecutor(executor);
        recorder.setBaseDir("target/recs");
        recorder.setLogLevel(LogLevel.TRACE);
        recorder.setKeepRecordsForDays(1);
        assertTrue(recorder.start());
        
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy.MM/dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);

        File liveDir = new File("target/recs/"+fmt.format(c.getTime()));
        liveDir.mkdirs();
        c.add(Calendar.DATE, -1);
        File del1Dir = new File("target/recs/"+fmt.format(c.getTime()));
        del1Dir.mkdirs();
        c.add(Calendar.MONTH, -1);
        File del2Dir = new File("target/recs/"+fmt.format(c.getTime()));
        del2Dir.mkdirs();
        
        assertTrue(liveDir.exists());
        assertTrue(del1Dir.exists());
        assertTrue(del2Dir.exists());
        
        recorder.executeScheduledJob(null);
        assertTrue(liveDir.exists());
        assertFalse(del1Dir.exists());
        assertFalse(del2Dir.exists());
    }
    
    @Test
    public void recordTest() throws Exception {
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        IvrEndpointConversation conv1 = createMock("conv1", IvrEndpointConversation.class);
        IvrEndpointConversation conv2 = createMock("conv2", IvrEndpointConversation.class);
        IncomingRtpStream inRtp1 = createMock("inRtp1", IncomingRtpStream.class);
        IncomingRtpStream inRtp2 = createMock("inRtp2", IncomingRtpStream.class);
        
        expect(bridge.getConversation1()).andReturn(conv1).atLeastOnce();
        expect(bridge.getConversation2()).andReturn(conv2).atLeastOnce();
        expect(conv1.getCallingNumber()).andReturn("c1n1").anyTimes();
        expect(conv1.getCalledNumber()).andReturn("c1n2").anyTimes();
        expect(conv1.getIncomingRtpStream()).andReturn(inRtp1);
        expect(conv2.getCallingNumber()).andReturn("c2n1").anyTimes();
        expect(conv2.getCalledNumber()).andReturn("c2n2").anyTimes();
        expect(conv2.getIncomingRtpStream()).andReturn(inRtp2);
        PushBufferDataSource ds1 = createDataSourceFromFile("src/test/wav/test.wav");
        expect(inRtp1.addDataSourceListener(addDataSourceListener(ds1), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        PushBufferDataSource ds2 = createDataSourceFromFile("src/test/wav/test2.wav");
        expect(inRtp2.addDataSourceListener(addDataSourceListener(ds2), isNull(AudioFormat.class)))
                .andReturn(Boolean.TRUE);
        
        replay(bridge, conv1, conv2, inRtp1, inRtp2);
        
        CallRecordingRecordSchemaNode schema = new CallRecordingRecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());
        
        CallRecorderNode recorder = new CallRecorderNode();
        recorder.setName("recorder");
        tree.getRootNode().addAndSaveChildren(recorder);
        recorder.setConversationBridgeManager(bridgeManager);
        recorder.setExecutor(executor);
        recorder.setBaseDir("target/recs");
        recorder.setFileNameExpression("'test.wav'");
        recorder.setLogLevel(LogLevel.TRACE);
        recorder.setRecordSchema(schema);
        assertTrue(recorder.start());
        
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(recorder);
        assertTrue(collector.start());
        
        long startTime = System.currentTimeMillis();
        recorder.bridgeActivated(bridge);
        TimeUnit.SECONDS.sleep(4);
        recorder.bridgeDeactivated(bridge);
        TimeUnit.SECONDS.sleep(1);
        
        assertEquals(1, collector.getDataListSize());
        Object obj = collector.getDataList().get(0);
        assertNotNull(obj);
        assertTrue(obj instanceof Record);
        Record rec = (Record) obj;
        assertNull(rec.getValue(ID));
        assertEquals("c1n1", rec.getValue(CONV1_NUMA));
        assertEquals("c1n2", rec.getValue(CONV1_NUMB));
        assertEquals("c2n1", rec.getValue(CONV2_NUMA));
        assertEquals("c2n2", rec.getValue(CONV2_NUMB));
        Date recTime = (Date) rec.getValue(RECORDING_TIME);
        assertNotNull(recTime);
        assertTrue(recTime.getTime()>startTime);
        Integer dur = (Integer) rec.getValue(RECORDING_DURATION);
        assertTrue(dur>0);
        File file = new File("target/recs/"+new SimpleDateFormat("yyyy.MM/dd/").format(new Date())+"test.wav");
        assertTrue(file.exists());
        assertEquals(file.getPath(), rec.getValue(FILE));
        verify(bridge, conv1, conv2, inRtp1, inRtp2);
        
    }
    
    private PushBufferDataSource createDataSourceFromFile(String filename) throws FileNotFoundException {
        InputStreamSource source = new TestInputStreamSource(filename);
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
        return conv;
    }
    
    
    public static IncomingRtpStreamDataSourceListener addDataSourceListener(
            final PushBufferDataSource dataSource) 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                IncomingRtpStreamDataSourceListener listener = (IncomingRtpStreamDataSourceListener) o;
                listener.dataSourceCreated(null, dataSource);
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }
    
}
