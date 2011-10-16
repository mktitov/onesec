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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.Buffer;
import javax.media.Processor;
import javax.media.control.PacketSizeControl;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.BufferCache;
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

    private final Map<String, Buffer> silentBuffers = new ConcurrentHashMap<String, Buffer>();
    private final RTPManagerService rtpManagerService;
    private final Logger logger;

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

    private Buffer createSilentBuffer(Codec codec, int packetSize, String key) {
        try {
            Processor processor = null;
            PushBufferDataSource dataSource = null;
            IssDataSource source = null;
            try {
                ResourceInputStreamSource silenceSource=new ResourceInputStreamSource(SILENCE_RESOURCE_NAME);
                source =  new IssDataSource(silenceSource, FileTypeDescriptor.WAVE);

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
}
