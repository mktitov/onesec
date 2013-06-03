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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.media.Buffer;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.CodecManager;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.BuffersCacheEntity;
import org.onesec.raven.ivr.Codec;
import static org.onesec.raven.ivr.impl.BufferSplitterDataSourceTest.executeTask;
import org.raven.sched.Task;
import org.raven.tree.DataFile;

/**
 *
 * @author Mikhail Titov
 */
public class IvrUtilsTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger(IvrUtilsTest.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Mixer logger", null, logger);
    private CodecManager codecManager;
    private Node owner;
    private ExecutorService executor;
    private List<AudioFileWriterDataSource> fileWriters;
    private AudioFile audioFile;
    private BufferCache cache;
    private DataFile dataFile;
    private volatile static Buffer[] buffers;
    
    @Before
    public void prepare() throws Exception{
        codecManager = new CodecManagerImpl(logger);
        fileWriters = new ArrayList<AudioFileWriterDataSource>();
        trainMocks();
        replay(executor, owner, audioFile, cache, dataFile);
        
    }
    
    @After
    public void finish() throws Exception {
        verify(executor, owner, audioFile, cache, dataFile);
    }
    
    @Test
    public void test() throws Exception {
        PushBufferDataSource ds = IvrUtils.createSourceFromAudioFile(audioFile, codecManager, executor, owner, 
                cache, 160, loggerHelper);
        assertTrue(ds instanceof BufferSplitterDataSource);
        writeToFile(ds, "ivr_utils_t1.wav");
        Thread.sleep(5000);
        stopWriters();
        Thread.sleep(1000);
        ds = IvrUtils.createSourceFromAudioFile(audioFile, codecManager, executor, owner, cache, 160, 
                loggerHelper);
        assertTrue(ds instanceof ReplayBuffersDataSource);
        writeToFile(ds, "ivr_utils_t2.wav");
        Thread.sleep(5000);
        stopWriters();
    }
    
    private void stopWriters() {
        for (AudioFileWriterDataSource writer: fileWriters)
            writer.stop();
        fileWriters.clear();
    }
    
    private void trainMocks() throws Exception {
        executor = createMock("executor", ExecutorService.class);
        owner =  createMock("owner", Node.class);
        audioFile = createMock("audioFile", AudioFile.class);
        cache = createMock("bufferCache", BufferCache.class);
        dataFile = createMock("dataFile", DataFile.class);
        
        expect(audioFile.isStarted()).andReturn(Boolean.TRUE).atLeastOnce();
        expect(audioFile.getAudioFile()).andReturn(dataFile).atLeastOnce();
        expect(dataFile.getDataStream()).andReturn(new FileInputStream("src/test/wav/test.wav")).atLeastOnce();
        expect(audioFile.getPath()).andReturn("audio/file").atLeastOnce();
        expect(audioFile.getCacheChecksum()).andReturn(1l).atLeastOnce();
        expect(cache.getCachedBuffers("audio/file", 1l, Codec.LINEAR, 160)).andAnswer(new IAnswer<Buffer[]>() {
            public Buffer[] answer() throws Throwable {
                return buffers;
            }
        });
        cache.cacheBuffers(eq("audio/file"), eq(1l), eq(Codec.LINEAR), eq(160), anyObject(Collection.class));
        expectLastCall().andDelegateTo(new Cache());
        expect(cache.getCachedBuffers("audio/file", 1l, Codec.LINEAR, 160)).andAnswer(new IAnswer<Buffer[]>() {
            public Buffer[] answer() throws Throwable {
                return buffers;
            }
        });
        executor.execute(executeTask(owner));
        expectLastCall().atLeastOnce();
        expect(executor.executeQuietly(executeTask(owner))).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        expect(owner.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(owner.getName()).andReturn("owner").anyTimes();
        expect(owner.isLogLevelEnabled(anyObject(LogLevel.class))).andReturn(Boolean.TRUE).anyTimes();
    }
    
    public static Collection<Buffer> checkBuffers() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Collection<Buffer> _buffers = (Collection<Buffer>) argument;
                buffers = _buffers.toArray(new Buffer[_buffers.size()]);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        anyObject();
        return null;
    }
    
    public static Task executeTask(final Node owner) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                final Task task = (Task) argument;
                assertSame(owner, task.getTaskNode());
                new Thread(new Runnable() {
                    public void run() {
                        task.run();
//                        ++tasksFinished;
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

    private class Cache implements BufferCache {

        public Buffer getSilentBuffer(ExecutorService executor, Node requester, Codec codec, int packetSize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> _buffers) {
            System.out.println("\n!!! CACHING !!!\n");
            buffers = _buffers.toArray(new Buffer[_buffers.size()]);
            System.out.println("BUFFERS: "+buffers);
        }

        public void removeOldCaches() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void setMaxCacheIdleTime(long time) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public long getMaxCacheIdleTime() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public List<BuffersCacheEntity> getCacheEntities() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public List<String> getSilentBuffersKeys() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
}