/*
 *  Copyright 2011 Mikhail Titov.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.media.Buffer;
import javax.media.Processor;
import javax.media.control.PacketSizeControl;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.BuffersCacheEntity;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.RTPManagerService;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class BufferCacheImpl implements BufferCache
{
    public final static String SILENCE_RESOURCE_NAME = "/org/onesec/raven/ivr/silence.wav";
    public final static int WAIT_STATE_TIMEOUT = 2000;
    public final static long DEFAULT_MAX_CACHE_IDLE_TIME = 3600l;

    private final Map<String, Buffer> silentBuffers = new ConcurrentHashMap<String, Buffer>();
    private final Map<String, BuffersCacheEntity>  buffersCache = new ConcurrentHashMap<String, BuffersCacheEntity>();
    private final RTPManagerService rtpManagerService;
    private final Logger logger;

    private AtomicLong maxCacheIdleTime = new AtomicLong(DEFAULT_MAX_CACHE_IDLE_TIME);

    public BufferCacheImpl(RTPManagerService rtpManagerService, Logger logger) {
        this.rtpManagerService = rtpManagerService;
        this.logger = logger;
    }

    public Buffer getSilentBuffer(Codec codec, int packetSize) {
        String key = codec.toString()+"_"+packetSize;
        Buffer res = silentBuffers.get(key);
        if (res==null) synchronized (silentBuffers) {
            res = silentBuffers.get(key);
            if (res==null)
                res = createSilentBuffer(codec, packetSize, key);
        }
        return res;
    }

    public List<String> getSilentBuffersKeys() {
        return new ArrayList<String>(silentBuffers.keySet());
    }

    public List<BuffersCacheEntity> getCacheEntities() {
        return new ArrayList<BuffersCacheEntity>(buffersCache.values());
    }

    public long getMaxCacheIdleTime() {
        return maxCacheIdleTime.get();
    }

    public void removeOldCaches() {
        Iterator<Map.Entry<String, BuffersCacheEntity>> it = buffersCache.entrySet().iterator();
        while (it.hasNext())
            if (it.next().getValue().isInvalid())
                it.remove();
    }

    public void setMaxCacheIdleTime(long time) {
        maxCacheIdleTime.set(time);
    }

    public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> buffers) {
        buffersCache.put(
                formCacheKey(key, codec, packetSize)
                , new CacheEntity(key, codec, packetSize, checksum, buffers));
    }

    public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize) {
        BuffersCacheEntity entity = buffersCache.get(formCacheKey(key, codec, packetSize));
        return entity!=null && entity.getChecksum()==checksum? entity.getBuffers() : null;
    }

    private String formCacheKey(String key, Codec codec, int packetSize){
        return key+"_"+codec+"_"+packetSize;
    }

    private Buffer createSilentBuffer(Codec codec, int packetSize, String key) {
        try {
            Processor processor = null;
            PushBufferDataSource dataSource = null;
            IssDataSource source = null;
            try {
                ResourceInputStreamSource silenceSource=new ResourceInputStreamSource(SILENCE_RESOURCE_NAME);
                source = new IssDataSource(silenceSource, FileTypeDescriptor.WAVE);

                processor = ControllerStateWaiter.createRealizedProcessor(
                        source, codec.getAudioFormat(), WAIT_STATE_TIMEOUT);

                PacketSizeControl packetSizeControl =
                        (PacketSizeControl) processor.getControl(PacketSizeControl.class.getName());
                if (packetSizeControl!=null)
                    packetSizeControl.setPacketSize(packetSize);

                dataSource = (PushBufferDataSource)processor.getDataOutput();
                PushBufferStream stream = dataSource.getStreams()[0];
                dataSource.start();
                processor.start();
                Buffer silentBuffer = new Buffer();
                stream.read(silentBuffer);
                silentBuffers.put(key, silentBuffer);
                return silentBuffer;
            } finally {
                source.stop();
                if (processor!=null) processor.stop();
                if (dataSource!=null) dataSource.stop();
                if (processor!=null) processor.close();
            }
        } catch (Exception ex) {
            if (logger.isErrorEnabled())
                logger.error(String.format(
                        "Error initializing silent buffer for codec (%s) and rtp packet size (%s)"
                        , codec, packetSize)
                        , ex);
            return null;
        }
    }

    private class CacheEntity implements BuffersCacheEntity {
        private final String key;
        private final Codec codec;
        private final int packetSize;
        private final long checksum;
        private final Buffer[] buffers;
        private final AtomicLong ts = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong usageCount = new AtomicLong(0);

        public CacheEntity(String key, Codec codec, int packetSize, long checksum, Collection<Buffer> buffers)
        {
            this.key = key;
            this.codec = codec;
            this.packetSize = packetSize;
            this.checksum = checksum;
            this.buffers = new Buffer[buffers.size()];
            buffers.toArray(this.buffers);
        }

        public int getBuffersCount() {
            return buffers.length;
        }

        public long getChecksum() {
            return checksum;
        }

        public Codec getCodec() {
            return codec;
        }

        public long getIdleTime() {
            return (System.currentTimeMillis()-ts.get())/1000;
        }

        public String getKey() {
            return key;
        }

        public int getPacketSize() {
            return packetSize;
        }

        public boolean isInvalid(){
            return (System.currentTimeMillis()-ts.get())/1000>maxCacheIdleTime.get();
        }

        public Buffer[] getBuffers(){
            ts.set(System.currentTimeMillis());
            usageCount.incrementAndGet();
            return buffers;
        }

        public long getUsageCount(){
            return usageCount.get();
        }
    }
}
