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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataSourceMergerTest extends Assert {
    private static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    private static volatile int tasksFinished;
    private CodecManager codecManager;
    private Node owner;
    private ExecutorService executor;

    
    @Before
    public void prepare() throws IOException {
        codecManager = new CodecManagerImpl(logger);
        tasksFinished = 0;
    }
    
//    @Test
    public void addNumbersTest() {
        int x=2, y=6;
        int a, b;
        do {
            a = x & y; b = x ^ y; x = a << 1; y = b;
        } while(a>0);
        System.out.println("res: "+b);
    }
    
//    @Test
    public void addNumbers2Test() {
        int a = 254;
        int r = 0;
        byte b = (byte) (0|a);
        System.out.println("byte value: "+(b));
        System.out.println("!! RES: "+(r|b));
    }
    
//    @Test
    public void mergeOneStream() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 3);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test2.wav"));
//        merger.addDataSource(createDataSourceFromFile("src/test/wav/1sec_silence.wav"));
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_1_source.wav");
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
//    @Test
    public void mergeOneStreamLittlePackets() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 3);
        merger.addDataSource(new BufferSplitterDataSource(createDataSourceFromFile("src/test/wav/test2.wav"), 240));
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_1l_source.wav");
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
//    @Test
    public void mergeTwoStreams() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 3);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test2.wav"));
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test.wav"));
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_2_sources.wav");
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
//    @Test
    public void mergeTwoStreamsLittlePackets() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 3);
        PushBufferDataSource ds = createDataSourceFromFile("src/test/wav/test2.wav");
        merger.addDataSource(new BufferSplitterDataSource(ds, 160));
        merger.addDataSource(new BufferSplitterDataSource(createDataSourceFromFile("src/test/wav/test.wav"), 160));
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_2l_sources.wav");
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
    @Test
    public void dynamicAddStream2() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        PushBufferDataSource ds = createDataSourceFromFile("src/test/wav/test2.wav");
        TranscoderDataSource tds1 = new TranscoderDataSource(codecManager, ds
                , Codec.G729.getAudioFormat(), new LoggerHelper(owner, null));
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 10);
//        merger.addDataSource(tds1);
        merger.addDataSource(ds);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test.wav"));
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_3_1_sources.wav");
        TimeUnit.MILLISECONDS.sleep(1000);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/greeting.wav"));
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
//    @Test
    public void dynamicAddStream() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeDataSourceMerger merger = new RealTimeDataSourceMerger(codecManager, owner, null
                , executor, 0, 3);
        merger.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(merger, "target/merger_1d_sources.wav");
        TimeUnit.MILLISECONDS.sleep(1000);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/greeting.wav"));
        TimeUnit.SECONDS.sleep(4);
        merger.disconnect();
        controller.stop();
        
        verify(executor, owner);
    }
    
    private PushBufferDataSource createDataSourceFromFile(String filename) throws FileNotFoundException {
        InputStreamSource source = new TestInputStreamSource(filename);
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
        return conv;
    }
    
    private void trainMocks() throws ExecutorServiceException {
        executor = createMock("executor", ExecutorService.class);
        owner =  createMock("owner", Node.class);
        
        executor.execute(executeTask(owner));
        expectLastCall().atLeastOnce();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        expect(owner.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(owner.getName()).andReturn("owner").anyTimes();
        expect(owner.isLogLevelEnabled(anyObject(LogLevel.class))).andReturn(Boolean.TRUE).anyTimes();
    }
    
    public static Task executeTask(final Node owner) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                final Task task = (Task) argument;
                assertSame(owner, task.getTaskNode());
                new Thread(new Runnable() {
                    public void run() {
                        task.run();
                        ++tasksFinished;
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }
    
}
