/*
 * Copyright 2015 Mikhail Titov.
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

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Set;
import javax.media.protocol.FileTypeDescriptor;
import mockit.integration.junit4.JMockit;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.PlayAudioDP;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.BufferCacheImpl;
import org.onesec.raven.ivr.impl.CodecManagerImpl;
import org.onesec.raven.ivr.impl.ConcatDataSource;
import org.onesec.raven.ivr.impl.RTPManagerServiceImpl;
import org.onesec.raven.ivr.impl.TestInputStreamSource;
import org.onesec.raven.ivr.impl.TickingDataSource;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class PlayAudioDPTest extends OnesecRavenTestCase {
    private final Logger logger = LoggerFactory.getLogger(PlayAudioDPTest.class);

    
    private ExecutorServiceNode executor;
    private ConcatDataSource audioStream;
    private CodecManager codecManager;
    private TickingDataSource tickingSource;
    private JMFHelper.OperationController writeControl;
    private DataProcessorFacadeConfig dpConfig;
    
    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder); 
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        
    }
    
    @Before
    public void prepare() throws Exception {
        testsNode.setLogLevel(LogLevel.TRACE);
        codecManager = new CodecManagerImpl(logger);
        RTPManagerServiceImpl rtpManagerService = new RTPManagerServiceImpl(logger, codecManager);
        BufferCacheImpl bufferCache = new BufferCacheImpl(rtpManagerService, logger, codecManager);
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(8);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        audioStream = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executor, codecManager, Codec.G711_MU_LAW, 240, 5
                , 5, testsNode, bufferCache, new LoggerHelper(testsNode, null));
        tickingSource = new TickingDataSource(audioStream, 30, new LoggerHelper(testsNode, null));        
        
        dpConfig = new DataProcessorFacadeConfig(
                "Player", testsNode, new PlayAudioDP(audioStream), executor, 
                new LoggerHelper(testsNode, null)
            ).withDefaultAskTimeout(30000);
    }
    
    @Test(timeout = 30000)
    public void playInputStreamSourceTest() throws Exception {
        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test11.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");
        
        startWriteToFile("target/playAudioDP_is.wav");
        
        DataProcessorFacade dp = dpConfig.build();
        assertEquals(dp.ask(new PlayAudioDP.PlayInputStreamSource(source1, null)).get(), PlayAudioDP.PLAYED);
        assertEquals(dp.ask(new PlayAudioDP.PlayInputStreamSource(source2, null)).get(), PlayAudioDP.PLAYED);
        assertEquals(dp.ask(new PlayAudioDP.PlayInputStreamSource(source3, null)).get(), PlayAudioDP.PLAYED);
        
        stopWriteToFile();
    }
    
    @Test(timeout = 30_000)
    public void playAudioFileTest() throws Exception {
        AudioFile audioFile = createAudioFileNode("audio1", "src/test/wav/test11.wav");
        startWriteToFile("target/playAudioDP_audioFile.wav");
        
        DataProcessorFacade dp = dpConfig.build();
        assertEquals(PlayAudioDP.PLAYED, dp.ask(new PlayAudioDP.PlayAudioFile(audioFile)).get());
        
        stopWriteToFile();
    }

    @Test(timeout = 30_000)
    public void playAudioFilesTest_zeroPause() throws Exception {
        AudioFile audioFile1 = createAudioFileNode("audio1", "src/test/wav/test11.wav");
        AudioFile audioFile2 = createAudioFileNode("audio2", "src/test/wav/test2.wav");
        startWriteToFile("target/playAudioDP_audioFiles.wav");
        
        DataProcessorFacade dp = dpConfig.build();
        assertEquals(PlayAudioDP.PLAYED, dp.ask(
                new PlayAudioDP.PlayAudioFiles(Arrays.asList(audioFile1, audioFile2), 0)
            ).get());
        
        stopWriteToFile();
    }

    @Test(timeout = 30_000)
    public void playAudioFilesTest_2000msPause() throws Exception {
        AudioFile audioFile1 = createAudioFileNode("audio1", "src/test/wav/test11.wav");
        AudioFile audioFile2 = createAudioFileNode("audio2", "src/test/wav/test2.wav");
        startWriteToFile("target/playAudioDP_audioFiles_2000.wav");
        
        DataProcessorFacade dp = dpConfig.build();
        assertEquals(PlayAudioDP.PLAYED, dp.ask(
                new PlayAudioDP.PlayAudioFiles(Arrays.asList(audioFile1, audioFile2), 2000)
            ).get());
        
        stopWriteToFile();
    }

    @Test(timeout = 30_000)
    public void playAudioFilesTest_minus500msPause() throws Exception {
        AudioFile audioFile1 = createAudioFileNode("audio1", "src/test/wav/test11.wav");
        AudioFile audioFile2 = createAudioFileNode("audio2", "src/test/wav/test2.wav");
        startWriteToFile("target/playAudioDP_audioFiles_-500.wav");
        
        DataProcessorFacade dp = dpConfig.build();
        assertEquals(PlayAudioDP.PLAYED, dp.ask(
                new PlayAudioDP.PlayAudioFiles(Arrays.asList(audioFile1, audioFile2), -500)
            ).get());
        
        stopWriteToFile();
    }

    private void startWriteToFile(String fileName) throws Exception {
        writeControl = JMFHelper.writeToFile(tickingSource, fileName);
    }
    
    private void stopWriteToFile() {
        writeControl.stop();
    }
    
    private AudioFileNode createAudioFileNode(String nodeName, String filename) throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName(nodeName);
        testsNode.addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream(filename);
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());
        return audioFileNode;
    }
}
