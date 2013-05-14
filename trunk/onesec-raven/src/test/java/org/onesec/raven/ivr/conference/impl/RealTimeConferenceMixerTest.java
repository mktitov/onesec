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
package org.onesec.raven.ivr.conference.impl;

import org.onesec.raven.ivr.conference.impl.RealTimeConferenceMixer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.junit.Before;
import static org.junit.Assert.*;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.junit.After;
import org.junit.Test;
import org.onesec.raven.ivr.ConferenceMixerSession;
import org.onesec.raven.ivr.impl.AudioFileWriterDataSource;
import org.onesec.raven.ivr.impl.BufferSplitterDataSource;
import org.onesec.raven.ivr.impl.CodecManagerImpl;
import org.onesec.raven.ivr.impl.ContainerParserDataSource;
import org.onesec.raven.ivr.impl.IssDataSource;
import org.onesec.raven.ivr.impl.PullToPushConverterDataSource;
import org.onesec.raven.ivr.impl.TestInputStreamSource;
import org.raven.sched.Task;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeConferenceMixerTest {
    private final static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Mixer logger", null, logger);
    private static volatile int tasksFinished;
    private CodecManager codecManager;
    private Node owner;
    private ExecutorService executor;
//    private List<JMFHelper.OperationController> fileWriteControllers;
    private List<AudioFileWriterDataSource> fileWriters;
    private RealTimeConferenceMixer conf;
    
    @Before
    public void prepare() throws Exception {
        codecManager = new CodecManagerImpl(logger);
        tasksFinished = 0;
        fileWriters = new ArrayList<AudioFileWriterDataSource>();
        trainMocks();
        replay(executor, owner);
        conf = new RealTimeConferenceMixer(codecManager, owner, loggerHelper, executor, 0, 1);
    }
    
    @After
    public void finish() throws Exception {
        for (AudioFileWriterDataSource writer: fileWriters)
            writer.stop();
        verify(executor, owner);
    }

//    @Test
    public void test() throws Exception {
        conf.connect();
        conf.start();
        writeToFile(conf, "conf_all.wav");
        writeToFile(conf.addParticipant("P1", createDataSourceFromFile("part_1.wav")), "conf_p1.wav");
        Thread.sleep(6000);
    }
    
//    @Test
    public void threeParticipantsTest() throws Exception {
        conf.connect();
        conf.start();
        writeToFile(conf, "conf_all.wav");
        writeToFile(conf.addParticipant("P1", createDataSourceFromFile("part_1.wav")), "conf_p1.wav");
        Thread.sleep(1000);
        writeToFile(conf.addParticipant("P2", createDataSourceFromFile("part_2.wav")), "conf_p2.wav");
        Thread.sleep(1000);
        writeToFile(conf.addParticipant("P3", createDataSourceFromFile("part_3.wav")), "conf_p3.wav");        
        Thread.sleep(7000);
        conf.disconnect();
    }
    
    @Test
    public void muteUnmuteTest() throws Exception {
        conf.connect();
        conf.start();
        writeToFile(conf, "conf_all_1.wav");
        ConferenceMixerSession sess = writeToFile(conf.addParticipant("P1", createDataSourceFromFile("part_1.wav")), 
            "conf_p1_1.wav");
        Thread.sleep(1000);
        writeToFile(conf.addParticipant("P3", createDataSourceFromFile("part_3.wav")), "conf_p3_1.wav");
        writeToFile(conf.addParticipant("P2", createDataSourceFromFile("part_2.wav")), "conf_p2_1.wav");
        Thread.sleep(1000);
        sess.stopParticipantAudio();
        Thread.sleep(3000);
        sess.replaceParticipantAudio(createDataSourceFromFile("part_1.wav"));
        Thread.sleep(7000);
    }
    
    private PushBufferDataSource createDataSourceFromFile(String filename) throws Exception {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/"+filename);
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
        return new BufferSplitterDataSource(conv, 160, codecManager, loggerHelper);
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
    
    private ConferenceMixerSession writeToFile(ConferenceMixerSession sess, String filename) throws Exception {
        writeToFile(sess.getConferenceAudioSource(), filename);
        return sess;
    }
    
    private void startFileWriters() throws Exception {
        for (AudioFileWriterDataSource writer: fileWriters)
            writer.start();
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
