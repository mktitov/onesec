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
import java.util.concurrent.TimeUnit;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
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
public class AudioFileWriterDataSourceTest {
    private final static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    private final static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Logger", null, logger);
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
    public void test() throws Exception {
        trainMocks();
        replay(executor, owner);
        PushBufferDataSource ds = createDataSourceFromFile("src/test/wav/test2.wav");
        
        AudioFileWriterDataSource writer = new AudioFileWriterDataSource(
                new File("target/mux_test.wav"), ds, codecManager, FileTypeDescriptor.WAVE, loggerHelper);
        writer.start();
        TimeUnit.SECONDS.sleep(10);
        writer.stop();
                
        verify(executor, owner);
    }
    
    @Test
    public void testWithDataSourceMerger() throws Exception {
        trainMocks();
        replay(executor, owner);
        
        RealTimeMixer merger = new RealTimeMixer(codecManager, owner, "", executor, 3, 3);
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test2.wav"));
        merger.addDataSource(createDataSourceFromFile("src/test/wav/test.wav"));
        
        AudioFileWriterDataSource writer = new AudioFileWriterDataSource(
                new File("target/mux_with_merger_test.wav"), merger, codecManager, FileTypeDescriptor.WAVE
                , loggerHelper);
        writer.start();
        TimeUnit.SECONDS.sleep(4);
        writer.stop();
                
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
        expect(owner.getName()).andReturn("owner").anyTimes();
        expect(owner.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();        
        expect(owner.getLogger()).andReturn(logger).anyTimes();
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
