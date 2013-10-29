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

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.media.Buffer;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onesec.raven.RtpManagerTestCase;
import org.onesec.raven.TestSchedulerNode;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class RtpStressTest extends RtpManagerTestCase {
    private final int STREAMS_COUNT = 150;
    private final Codec CODEC = Codec.G711_MU_LAW;
    private final int PACKET_SIZE = 240;
    private final long WAIT_INTERVAL = 20000;
    
    private final AtomicInteger receivedPackets = new AtomicInteger();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicInteger initiatedStreams = new AtomicInteger();
    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean process = ManagementFactory.getOperatingSystemMXBean();
    private AudioFileNode audioNode;
    private TestSchedulerNode scheduler;
    private BufferCache bufferCache;
    private int packetsCount = 0;
    private List<StreamsPair> streams;
    private BaseNode streamsLogger;
    

    @Override
    public void initNodes() throws Exception {
        manager.stop();
        manager.setMaxStreamCount(STREAMS_COUNT * 2);
        manager.setLogLevel(LogLevel.WARN);
        assertTrue(manager.start());
        
        executor.stop();
        executor.setCorePoolSize((int)(10+STREAMS_COUNT*1.2));
        executor.setMaximumPoolSize(executor.getCorePoolSize());
        assertTrue(executor.start());
        
        streamsLogger = new BaseNode("streams logger");
        testsNode.addAndSaveChildren(streamsLogger);
//        streamsLogger.setLogLevel(LogLevel.TRACE);
        streamsLogger.setLogLevel(LogLevel.WARN);
        assertTrue(streamsLogger.start());
        
        scheduler = new TestSchedulerNode();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        BufferCacheNode cache = (BufferCacheNode) tree.getNode("/System/Services/"+BufferCacheNode.NAME);
        assertNotNull(cache);
        cache.setMaxCacheIdleTime(600l);
        cache.setScheduler(scheduler);
        cache.setLogLevel(LogLevel.TRACE);
        assertTrue(cache.start());
        
        bufferCache = registry.getService(BufferCache.class);
        
        createAudioNode();
        cacheAudio();
    }
    
    private void createAudioNode() throws Exception {
        audioNode = new AudioFileNode();
        audioNode.setName("audio");
        testsNode.addAndSaveChildren(audioNode);
        audioNode.setLogLevel(LogLevel.TRACE);
        FileInputStream stream = new FileInputStream("src/test/wav/long_mess.wav");
        try {
            audioNode.getAudioFile().setDataStream(stream);
            assertTrue(audioNode.start());
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
    
    private void cacheAudio() throws Exception {
        final ConcatDataSource audioSource = createAudioSource(LogLevel.TRACE, "Audio cacher. ");
        try {
            audioSource.connect();
            audioSource.start();
            audioSource.addSource(audioNode.getCacheKey(), audioNode.getCacheChecksum()
                    , new AudioFileInputStreamSource(audioNode, audioNode));
            while (bufferCache.getCacheEntities().isEmpty())
                Thread.sleep(100);
            packetsCount = bufferCache.getCacheEntities().iterator().next().getBuffersCount();
            logger.debug("!!!CACHED PACKETS COUNT: "+packetsCount);
        } finally {
            audioSource.stop();
        }
    }
    
    private void addCachedAudioToSource(ConcatDataSource source) {
        source.addSource(audioNode.getCacheKey(), audioNode.getCacheChecksum(), (DataSource)null);
        
    }
    
    private ConcatDataSource createAudioSource(LogLevel logLevel, String prefix) {
        return new ConcatDataSource(FileTypeDescriptor.WAVE, executor, codecManager, CODEC, 
                PACKET_SIZE, 5, 5, manager, bufferCache , 
                new LoggerHelper(logLevel, "AS", prefix, logger));
        
    }
    
    @Test
    public void standartRtpConnectorTest() throws Exception{
        createStreams();
    }
    
//    @Test
    public void nettyRtpConnectorTest() throws Exception {
        manager.stop();
        switchToNettyRtpManagerConfigurator();
        assertTrue(manager.start());
        createStreams();
    }
    
    private void createStreams() throws Exception {
        System.out.println("Creating "+STREAMS_COUNT+" streams...");
        long threadsCount = threads.getThreadCount();
        streams = new ArrayList<StreamsPair>(STREAMS_COUNT);
        for (int i=0; i<STREAMS_COUNT; ++i) {
            OutgoingRtpStream outStream = manager.getOutgoingRtpStream(streamsLogger);
            IncomingRtpStream inStream = manager.getIncomingRtpStream(streamsLogger);
            ConcatDataSource audioSource = createAudioSource(LogLevel.ERROR, "AS ["+i+"]. ");
            
            InboundStreamListener listener = new InboundStreamListener();
            inStream.addDataSourceListener(listener, null);
            inStream.open(outStream.getAddress().getHostName(), outStream.getPort());
            audioSource.start();
            outStream.open(inStream.getAddress().getHostName(), inStream.getPort(), audioSource);
            outStream.start();
            streams.add(new StreamsPair(outStream, audioSource, inStream, listener));
        }
        System.out.println("\nWAITING WHILE ALL STREAM OPENED");
        long ts = System.currentTimeMillis();
        while (initiatedStreams.get()<STREAMS_COUNT) {
            Thread.sleep(100);
            if (System.currentTimeMillis()-20000>ts) {
                logger.error("\nTimeout stream initialization!!!");
                throw new Exception("Timeout wating streams initialization. "
                        + "Initiazed "+initiatedStreams.get()+", but need "+STREAMS_COUNT);
            }
        }
        System.out.println("\nAll stream initialized");
        logger.info("Starting transmission");
        for (StreamsPair pair: streams) 
            pair.inListener.start();
        long rtpThreads = threads.getThreadCount() - threadsCount;
        long cpuTime = getCpuUsage();
        Thread.sleep(WAIT_INTERVAL);
        System.out.println("CPU usage time: "+(getCpuUsage()-cpuTime));
        System.out.println("\nReceived packets: "+receivedPackets.get());
        long expected = WAIT_INTERVAL / (PACKET_SIZE/8) * STREAMS_COUNT;
        System.out.println("Expected packets: "+expected);
        System.out.println("Lost (%): "+(expected-receivedPackets.get())*100/expected);
        System.out.println("THREADS: "+rtpThreads);
        for (StreamsPair pair: streams) {
            pair.audioStream.stop();
            pair.outStream.release();
            pair.inStream.release();
        }
    }
    
    private long getCpuUsage() {
        long cpuTime = 0;
        for (long id: threads.getAllThreadIds())
            cpuTime += threads.getThreadCpuTime(id);
        return cpuTime;
    }
    
    private class InboundStreamListener implements IncomingRtpStreamDataSourceListener, BufferTransferHandler
    {
        private final Buffer buf;
        private volatile boolean started = false;

        public InboundStreamListener() {
            this.buf = new Buffer();
            buf.setData(new byte[512]);
        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
            logger.debug("Received dataSourceCreated event.");
            if (dataSource!=null) {
                initiatedStreams.incrementAndGet();
                ((PushBufferDataSource)dataSource).getStreams()[0].setTransferHandler(this);
            }
        }

        public void streamClosing(IncomingRtpStream stream) {
//            if (writeControl!=null)
//                writeControl.stop();
        }
        
        public void start() {
            started = true;
        }

        public void transferData(PushBufferStream stream) {
            if (!started) return;
            try {
                try {
                    stream.read(buf);
//                    logger.debug("!!! Bytes readed from stream: "+buf.getLength());
                    receivedPackets.incrementAndGet();
                    receivedBytes.addAndGet(buf.getLength());
                } finally {
                    buf.setOffset(0);
                    buf.setLength(0);
                }
            } catch (IOException ex) {
                logger.error("Error reading from RTP stream", ex);
            }
        }
    }
    
    private class StreamsPair {
        private final OutgoingRtpStream outStream;
        private final ConcatDataSource audioStream;
        private final IncomingRtpStream inStream;
        private final InboundStreamListener inListener;

        public StreamsPair(OutgoingRtpStream outStream, ConcatDataSource audioStream, IncomingRtpStream inStream, InboundStreamListener inListener) {
            this.outStream = outStream;
            this.audioStream = audioStream;
            this.inStream = inStream;
            this.inListener = inListener;
        }

    }
}
