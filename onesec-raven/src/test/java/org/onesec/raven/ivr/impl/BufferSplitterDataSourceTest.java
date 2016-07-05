/*
 * Copyright 2013 Mikhail Titov.
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
import java.util.ArrayList;
import java.util.List;
import javax.media.Buffer;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.raven.sched.Task;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class BufferSplitterDataSourceTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger(BufferSplitterDataSourceTest.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Mixer logger", null, logger);
    private CodecManager codecManager;
    private Node owner;
    private ExecutorService executor;
    private List<AudioFileWriterDataSource> fileWriters;
    private static volatile int tasksFinished;

    @Before
    public void prepare() throws Exception {
        codecManager = new CodecManagerImpl(logger);
        fileWriters = new ArrayList<AudioFileWriterDataSource>();
        tasksFinished = 0;
        trainMocks();
        replay(executor, owner);
        
    }
    
    @After
    public void finish() throws Exception {
        for (AudioFileWriterDataSource writer: fileWriters)
            writer.stop();
        verify(executor, owner);
    }
    
    @Test()
    public void test() throws Exception {
        PullToPushConverterDataSource fileSource = createSourceFromFile();
        BufferSplitterDataSource splitterSource = new BufferSplitterDataSource(
                fileSource, 480, codecManager, loggerHelper);
        writeToFile(splitterSource, "splitter_res.wav");
        Thread.sleep(5000);
    }
    
    @Test
    public void cacheBuffersTest() throws Exception {
        IMocksControl mocks = createControl();
        BufferCacheListener listener = mocks.createMock(BufferCacheListener.class);
        listener.buffersCached(checkBuffers());
        mocks.replay();
        
        PullToPushConverterDataSource fileSource = createSourceFromFile();
        BufferSplitterDataSource splitterSource = new BufferSplitterDataSource(
                fileSource, 160, codecManager, loggerHelper, listener, executor, owner);
        writeToFile(splitterSource, "splitter_res_2.wav");
        Thread.sleep(5000);        
        
        mocks.verify();
    }
    
    public static Buffer[] checkBuffers() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Buffer[] buffers = (Buffer[]) argument;
                return buffers!=null && buffers.length>0;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    private void trainMocks() throws ExecutorServiceException {
        executor = createMock("executor", ExecutorService.class);
        owner =  createMock("owner", Node.class);
        
        executor.execute(executeTask(owner));
        expectLastCall().atLeastOnce();
        expect(executor.executeQuietly(executeTask(owner))).andReturn(Boolean.TRUE).anyTimes();
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
    
    private void writeToFile(PushBufferDataSource ds, String filename) throws Exception {
        File file = new File("target/"+filename);
        if (file.exists()) file.delete();
        AudioFileWriterDataSource writer = new AudioFileWriterDataSource(
                file, ds, codecManager, FileTypeDescriptor.WAVE, loggerHelper);
        writer.start();
        fileWriters.add(writer);
//        fileWriteControllers.add(JMFHelper.writeToFile(ds, "target/"+filename));
    }

    private PullToPushConverterDataSource createSourceFromFile() throws FileNotFoundException {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test11_8k.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
        return conv;
    }
}
