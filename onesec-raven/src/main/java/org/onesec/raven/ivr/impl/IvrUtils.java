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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.media.Buffer;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.BufferCache;
import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class IvrUtils
{
    private IvrUtils()     {
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , InputStreamSource audio)
        throws InterruptedException
    {
        playAudioInAction(action, conversation, audio, null);
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , InputStreamSource audio, Cacheable cacheInfo)
        throws InterruptedException
    {
        AudioStream stream = conversation.getAudioStream();
        if (stream!=null){
            if (cacheInfo==null || !cacheInfo.isCacheable())
                stream.addSource(audio);
            else
                stream.addSource(cacheInfo.getCacheKey(), cacheInfo.getCacheChecksum(), audio);
//            Thread.sleep(200);
            while (!action.hasCancelRequest() && stream.isPlaying())
                TimeUnit.MILLISECONDS.sleep(10);
            TimeUnit.MILLISECONDS.sleep(20);
        }
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , AudioFile audio)
        throws InterruptedException
    {
        AudioFileInputStreamSource source = new AudioFileInputStreamSource(audio, conversation.getOwner());
        playAudioInAction(action, conversation, source, audio);
    }
    
    public static PushBufferDataSource createSourceFromAudioFile(AudioFile audioFile, 
            CodecManager codecManager, ExecutorService executor, Node owner, BufferCache bufferCache, 
            int packetSize, LoggerHelper logger) throws Exception
    {
        final String key = audioFile.getPath();
        final long checksum = audioFile.getCacheChecksum();
        final Codec codec = Codec.LINEAR;
        Buffer[] buffers = bufferCache.getCachedBuffers(key, checksum, codec, packetSize);
        if (buffers!=null) {
            if (logger.isDebugEnabled())
                logger.debug("Reading audio data from cache");
            return new ReplayBuffersDataSource(buffers, packetSize, executor, codec, owner, logger);
        } else {
            if (logger.isDebugEnabled())
                logger.debug("Reading audio data from audio file node");
            AudioFileInputStreamSource source = new AudioFileInputStreamSource(audioFile, owner);            
            IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
            ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
            PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
            return new BufferSplitterDataSource(conv, packetSize, codecManager, logger, 
                    new CacheListener(bufferCache, codec, key, packetSize, checksum), 
                    executor, owner);
        }
    }
    
    private static class CacheListener implements BufferCacheListener {
        private final BufferCache cache;
        private final Codec codec;
        private final String key;
        private final int packetSize;
        private final long checksum;

        public CacheListener(BufferCache cache, Codec codec, String key, int packetSize, long checksum) {
            this.cache = cache;
            this.codec = codec;
            this.key = key;
            this.packetSize = packetSize;
            this.checksum = checksum;
        }

        public void buffersCached(Buffer[] buffers) {
            cache.cacheBuffers(key, checksum, codec, packetSize, Arrays.asList(buffers));
        }
    }
}
