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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.media.Manager;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataSourceTest extends OnesecRavenTestCase
{
    private final Logger logger = LoggerFactory.getLogger(ConcatDataSourceTest.class);
    private BufferCache bufferCache;
    private CodecManager codecManager;
    private IMocksControl mocks;
    private ExecutorService executorService;
    private Node owner;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
    }

    @Before
    public void prepare() throws IOException{
        mocks = createControl();
        Logger log = LoggerFactory.getLogger(ConcatDataSourceTest.class);
        codecManager = new CodecManagerImpl(logger);
        RTPManagerServiceImpl rtpManagerService = new RTPManagerServiceImpl(log, codecManager);
        bufferCache = new BufferCacheImpl(rtpManagerService, log, codecManager);
    }

//    @Test
    public void addInputStreamSourceTest() throws Exception {
        trainOwnerAndExecutor();
        mocks.replay();

        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test11.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource dataSource = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executorService, codecManager, Codec.G711_MU_LAW, 240, 5
                , 5, owner, bufferCache, new LoggerHelper(owner, null));

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

        mocks.verify();
    }
    
    @Test
    public void playContinuouslyTest() throws Exception {
        trainOwnerAndExecutor();
        mocks.replay();
        
        List<AudioFile> files = Arrays.asList(
                newAudio("900"), newAudio("50"), newAudio("4"), newAudio("рубля"), newAudio("30"), 
                newAudio("3"), newAudio("копейки"));
        ConcatDataSource audioStream = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executorService, codecManager, Codec.G729, 240, 5
                , 5, owner, bufferCache, new LoggerHelper(owner, null));

        audioStream.start();
        JMFHelper.OperationController control = JMFHelper.writeToFile(audioStream, "target/cont_play_test.wav");
        audioStream.playContinuously(files, 90);
        while (audioStream.isPlaying())
            Thread.sleep(100);
        control.stop();
        
        control = JMFHelper.writeToFile(audioStream, "target/cont_play_test2.wav");
        audioStream.playContinuously(files, 90);
        while (audioStream.isPlaying())
            Thread.sleep(100);
        
        control.stop();
        audioStream.close();
        
        mocks.verify();
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
                , 5, owner, bufferCache, new LoggerHelper(owner, null));

        dataSource.start();
        JMFHelper.OperationController control  = JMFHelper.writeToFile(
                dataSource, "target/ds_test.wav");
        dataSource.addSource(ids);
        TimeUnit.SECONDS.sleep(5);
        dataSource.close();
        control.stop();
        
        verify(owner, executorService);
    }
    
    private AudioFile newAudio(String name) throws Exception {
        AudioFileNode file = new AudioFileNode();
        file.setName(name);
        testsNode.addAndSaveChildren(file);
        DataFile data = file.getAudioFile();
        data.setFilename(name);
        data.setDataStream(new FileInputStream(new File("src/test/wav/numbers/"+name+".wav")));
        assertTrue(file.start());
        return file;
    }
    
    private void trainOwnerAndExecutor() throws Exception {
        owner = mocks.createMock("node", Node.class);
        executorService = mocks.createMock("executor", ExecutorService.class);

        executorService.execute(executeTask());
        expectLastCall().atLeastOnce();
        expect(executorService.executeQuietly(executeTask())).andReturn(true).atLeastOnce();
        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        expect(owner.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(owner.getName()).andReturn("node").anyTimes();
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