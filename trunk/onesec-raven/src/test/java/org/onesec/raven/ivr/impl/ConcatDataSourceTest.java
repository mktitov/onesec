/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.Manager;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataSourceTest extends EasyMock
{
    private Logger logger = LoggerFactory.getLogger(ConcatDataSourceTest.class);
    private BufferCache bufferCache;
    private CodecManager codecManager;

    @Before
    public void prepare() throws IOException{
        Logger log = LoggerFactory.getLogger(ConcatDataSourceTest.class);
        codecManager = new CodecManagerImpl(logger);
        RTPManagerServiceImpl rtpManagerService = new RTPManagerServiceImpl(log, codecManager);
        bufferCache = new BufferCacheImpl(rtpManagerService, log, codecManager);
    }

    @Test
    public void addInputStreamSourceTest() throws Exception {
        Node owner = createMock("node", Node.class);
        ExecutorService executorService = createMock("executor", ExecutorService.class);

        executorService.execute(executeTask());
        expectLastCall().atLeastOnce();
        expect(executorService.executeQuietly(executeTask())).andReturn(true).atLeastOnce();
        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();

        replay(owner, executorService);

        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test11.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource dataSource = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executorService, codecManager, Codec.G711_MU_LAW, 240, 5
                , 5, owner, bufferCache);

        dataSource.start();
        JMFHelper.OperationController control = JMFHelper.writeToFile(dataSource, "target/iss_test.wav");
        addSourceAndWait(dataSource, source1);
        addSourceAndWait(dataSource, source2);
        TimeUnit.SECONDS.sleep(5);
        dataSource.reset();
        addSourceAndWait(dataSource, source3);
        TimeUnit.SECONDS.sleep(5);
        dataSource.close();
        control.stop();

        verify(owner, executorService);
    }

//    @Test
    public void addDataSourceTest() throws Exception
    {
        Node owner = createMock("node", Node.class);
        ExecutorService executorService = createMock("executor", ExecutorService.class);
//        Logger logger = createMock("logger", Logger.class);

//        executorService.execute(executeTask());
//        expectLastCall().atLeastOnce();
        executorService.execute(executeTask());
        expectLastCall().atLeastOnce();
        expect(executorService.executeQuietly(executeTask())).andReturn(Boolean.TRUE).times(2);
        expect(owner.isLogLevelEnabled(LogLevel.ERROR)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.isLogLevelEnabled(LogLevel.DEBUG)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();

        replay(owner, executorService);

        DataSource ids = Manager.createDataSource(new File("src/test/wav/test.wav").toURI().toURL());

        ConcatDataSource dataSource = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executorService, codecManager, Codec.G711_MU_LAW, 240, 5
                , 5, owner, bufferCache);

        dataSource.start();
        JMFHelper.OperationController control  = JMFHelper.writeToFile(
                dataSource, "target/ds_test.wav");
        dataSource.addSource(ids);
        TimeUnit.SECONDS.sleep(5);
        dataSource.close();
        control.stop();
        
        verify(owner, executorService);
    }

    private void addSourceAndWait(ConcatDataSource audioStream, InputStreamSource source)
            throws InterruptedException
    {
        audioStream.addSource("audio1", 10, source);
        while (audioStream.isPlaying()){
            TimeUnit.MILLISECONDS.sleep(10);
//            logger.debug("Waiting...");
        }
    }

    private static Task executeTask()
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(final Object argument)
            {
                new Thread(){

                    @Override
                    public void run() {
                        new Thread(){
                            @Override public void run() {
                                super.run();
                                ((Task)argument).run();
                            }
                        }.start();
                    }

                }.start();
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });
        
        return null;
    }

    private static String logMessage(final boolean errorMessage)
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(Object argument)
            {
                System.out.println(argument.toString());
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });
        return null;
    }

    private static Exception logException()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                ((Throwable)argument).printStackTrace();
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}