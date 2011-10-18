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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.PacketSizeControl;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataSource extends PushBufferDataSource implements AudioStream
{
    public static final int SOURCE_WAIT_TIMEOUT = 100;
    public static final int WAIT_STATE_TIMEOUT = 2000;

    private final String contentType;
    private final ExecutorService executorService;
    private final ConcatDataStream[] streams;
    private final Queue<Buffer> buffers = new ConcurrentLinkedQueue<Buffer>();
    private final AtomicReference<SourceProcessor> sourceProcessorRef = new AtomicReference<SourceProcessor>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Node owner;
    private final AtomicBoolean streamThreadRunning = new AtomicBoolean(false);
    private final Format format;
    private final int rtpPacketSize;
    private final BufferCache bufferCache;
    private final Codec codec;
    private String logPrefix;
    private int bufferCount;

    public ConcatDataSource(String contentType
            , ExecutorService executorService
            , Codec codec
            , int rtpPacketSize
            , int rtpInitialBufferSize
            , int rtpMaxSendAheadPacketsCount
            , Node owner
            , BufferCache bufferCache)
    {
        this.contentType = contentType;
        this.executorService = executorService;
        this.owner = owner;
        this.rtpPacketSize = rtpPacketSize;
        this.format = codec.getAudioFormat();
        this.bufferCache = bufferCache;
        this.codec = codec;
        
        bufferCount = 0;
        Buffer silentBuffer = bufferCache.getSilentBuffer(codec, rtpPacketSize);
        streams = new ConcatDataStream[]{new ConcatDataStream(
                buffers, this, owner, rtpPacketSize, rtpMaxSendAheadPacketsCount, silentBuffer)};
        streams[0].setLogPrefix(logPrefix);
    }

    public void addSource(DataSource source) {
        replaceSourceProcessor(new SourceProcessor(source));
    }

    public void addSource(String key, long checksum, DataSource source) {
        replaceSourceProcessor(new SourceProcessor(source, key, checksum));
    }

    public void addSource(InputStreamSource source) {
        if (source!=null)
            addSource(new IssDataSource(source, contentType));
    }

    public void addSource(String key, long checksum, InputStreamSource source) {
        if (source!=null)
            addSource(key, checksum, new IssDataSource(source, contentType));
    }

    private SourceProcessor replaceSourceProcessor(final SourceProcessor newSourceProcessor)
    {
        final SourceProcessor oldSp = sourceProcessorRef.getAndSet(newSourceProcessor);
        if (oldSp!=null) {
            oldSp.stop();
            executorService.executeQuietly(new AbstractTask(owner, "Stopping source processing") {
                @Override public void doRun() throws Exception {
                    oldSp.close();
                }
            });
        }
        buffers.clear();
        if (newSourceProcessor!=null)
            executorService.executeQuietly(new AbstractTask(owner, logMess("Starting processing new source")){
                @Override public void doRun() throws Exception {
                    newSourceProcessor.start();
                }
            });
        return newSourceProcessor;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public DataSource getDataSource() {
        return this;
    }

    public Format getFormat() {
        return format;
    }

    public boolean isPlaying() {
        SourceProcessor sp = sourceProcessorRef.get();
        return (sp!=null && sp.isProcessing()) || !buffers.isEmpty();
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void start() throws IOException {
        if (started.compareAndSet(false, true))
            try {
                executorService.execute(streams[0]);
            } catch (ExecutorServiceException ex) {
                throw new IOException(ex);
            }
    }

    @Override
    public void stop() throws IOException {
    }

    public void close()  {
        if (!stopped.compareAndSet(false, true))
            return;
        replaceSourceProcessor(null);
        buffers.clear();
        try {
            while (streamThreadRunning.get())
                TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("ConcatDataSource close operation was interrupted"), e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isClosed() {
        return stopped.get();
    }

    public void reset() {
        replaceSourceProcessor(null);
    }

    void setStreamThreadRunning(boolean streamThreadRunning) {
        this.streamThreadRunning.set(streamThreadRunning);
    }

    @Override
    public Object getControl(String controlClass) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Time getDuration() {
        return DURATION_UNKNOWN;
    }

    public Node getTaskNode() {
        return owner;
    }

    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"AudioStream. "+String.format(mess, args);
    }

    private class SourceProcessor implements BufferTransferHandler {
        private final DataSource source;
        private final AtomicBoolean stopProcessing = new AtomicBoolean(Boolean.FALSE);
        private final Lock lock = new ReentrantLock();
        private final String sourceKey;
        private final long sourceChecksum;
        private Collection<Buffer> cache;

        private PushBufferDataSource dataSource;
        private Processor processor;
        private long startTs;

        public SourceProcessor(DataSource source) {
            this.source = source;
            this.sourceChecksum = 0l;
            this.sourceKey = null;
        }

        public SourceProcessor(DataSource source, String sourceKey, long sourceChecksum) {
            this.source = source;
            this.sourceKey = sourceKey;
            this.sourceChecksum = sourceChecksum;
        }

        public boolean isProcessing(){
            return !stopProcessing.get();
        }

        public void start(){
            if (lock.tryLock()) try {
                if (!stopProcessing.get()) try {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(logMess("Processing new source..."));
                    startTs = System.currentTimeMillis();
                    if (!applyBuffersFromCache())
                        readBuffersFromSource();
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(logMess("Source initialization time (ms) - "
                                + (System.currentTimeMillis() - startTs)));
                }catch(Throwable e){
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(logMess("Error processing source"), e);
                }
            } finally {
                lock.unlock();
            }
        }
        
        private boolean applyBuffersFromCache() {
            if (sourceKey==null)
                return false;
            Buffer[] cachedBuffers = bufferCache.getCachedBuffers(sourceKey, sourceChecksum, codec, rtpPacketSize);
            if (cachedBuffers==null)
                return false;
            buffers.addAll(Arrays.asList(cachedBuffers));
            stopProcessing.set(true);
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess(
                        "Buffers applied from the cache (number of buffers - %s)", cachedBuffers.length));
            return true;
        }

        private void readBuffersFromSource() throws Exception {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Reading buffers from source"));
            processor = ControllerStateWaiter.createRealizedProcessor(source, format, WAIT_STATE_TIMEOUT);
            PacketSizeControl packetSizeControl =
                    (PacketSizeControl) processor.getControl(PacketSizeControl.class.getName());
            if (packetSizeControl != null) {
                packetSizeControl.setPacketSize(rtpPacketSize);
            }
            dataSource = (PushBufferDataSource) processor.getDataOutput();
            PushBufferStream s = dataSource.getStreams()[0];
            s.setTransferHandler(this);
            dataSource.start();
            long ts2 = System.currentTimeMillis();
            processor.prefetch();
            ControllerStateWaiter.waitForState(processor, Processor.Prefetched, WAIT_STATE_TIMEOUT);
            if (owner.isLogLevelEnabled(LogLevel.DEBUG)) {
                owner.getLogger().debug(logMess("Prefetching time is %s ms", System.currentTimeMillis() - ts2));
            }
            processor.start();
        }

        public void stop() {
            stopProcessing.set(true);
        }

        public void close(){
            stopProcessing.set(true);
            try {
                if (lock.tryLock(2000, TimeUnit.MILLISECONDS)) try {
                    try {
                        source.stop();
                    } finally {
                        try {
                            if (processor != null) processor.stop();
                        } finally {
                            try {
                                if (dataSource != null) dataSource.stop();
                            } finally {
                                if (processor != null) processor.close();
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error stopping source processor"));
            }
        }

        public void transferData(PushBufferStream stream) {
            try{
                if (stopProcessing.get())
                    return;
                Buffer buffer = new Buffer();
                stream.read(buffer);
                if (buffer.isDiscard())
                    return;
                boolean theEnd = false;
                if (buffer.isEOM()) {
                    buffer.setEOM(false);
                    close();
                    theEnd = true;
                }
                buffers.add(buffer);
                if (sourceKey!=null){
                    if (cache==null)
                        cache = new LinkedList<Buffer>();
                    cache.add(buffer);
                    if (theEnd) {
                        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                            owner.getLogger().debug(logMess(
                                    "Caching buffers: key - %s, codec - %s, packetSize - %s"
                                    , sourceKey, codec, rtpPacketSize));
                        bufferCache.cacheBuffers(sourceKey, sourceChecksum, codec, rtpPacketSize, cache);
                    }
                }
                ++bufferCount;
                if (theEnd && owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(String.format(
                            "Source processing time is %s ms", System.currentTimeMillis()-startTs));
            }catch (Exception e){
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().debug(logMess("Error reading buffer from source"), e);
                close();
            }
        }

    }
}
