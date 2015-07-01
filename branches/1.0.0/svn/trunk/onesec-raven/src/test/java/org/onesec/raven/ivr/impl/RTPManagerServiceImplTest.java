/*
 *  Copyright 2010 Mikhail Titov.
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

import com.sun.media.codec.audio.mp3.JavaDecoder;
import java.util.Collection;
import java.util.Vector;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.rtp.RTPManager;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.codec.AlawAudioFormat;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.codec.g729.G729FullDecoder;
import org.onesec.raven.codec.g729.G729FullEncoder;
import org.onesec.raven.ivr.RTPManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RTPManagerServiceImplTest extends OnesecRavenTestCase
{
    private static final Logger logger = LoggerFactory.getLogger(RTPManagerServiceImplTest.class);

    @Test
    public void printFormats() throws Exception
    {
//        AudioFormat inFormat = new AudioFormat(
//                AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
        Collection plugins = PlugInManager.getPlugInList(null, null, PlugInManager.DEMULTIPLEXER);
        assertNotNull(plugins);
        for (Object plugin: plugins) {
            logger.info("PLUGIN: {}", plugin.toString());
            Demultiplexer demux = (Demultiplexer) Class.forName(plugin.toString()).newInstance();
            ContentDescriptor[] descriptors = demux.getSupportedInputContentDescriptors();
            for (ContentDescriptor desc: descriptors)
                logger.info("    SUPPORTED CONTENT DESCRIPTOR: "+desc.toString());
//            Codec codec = (Codec) Class.forName(plugin.toString()).newInstance();
//            Format[] formats = codec.getSupportedInputFormats();
//            for (Format format: formats)
//                logger.info("  Decoder. Supported format: "+format.toString());
        }

    }

//    @Test
    public void printPluginSupportedFormats()
    {
        JavaDecoder decoder = new JavaDecoder();
        Format[] formats = decoder.getSupportedInputFormats();
        for (Format format: formats)
            logger.info("M3 Decoder. Supported format: "+format.toString());
    }

//    @Test
    public void codecsTest()
    {
        checkCodec(AudioFormat.ULAW, AudioFormat.ULAW_RTP, UlawPacketizer.class);
        checkCodec(AudioFormat.LINEAR, AudioFormat.ALAW, AlawEncoder.class);
        checkCodec(AudioFormat.ALAW, AlawAudioFormat.ALAW_RTP, AlawPacketizer.class);
        checkCodec(AudioFormat.LINEAR, AudioFormat.G729_RTP, G729FullEncoder.class);
        checkCodec(AudioFormat.G729_RTP, AudioFormat.LINEAR, G729FullDecoder.class);
    }

    @Test
    public void serviceTest()
    {
        RTPManagerService service = registry.getService(RTPManagerService.class);
        assertNotNull(service);
        RTPManager manager = service.createRtpManager();
        assertNotNull(manager);
    }

    private void checkCodec(String inFormat, String outFormat, Class codecClass)
    {
        Vector codecs = PlugInManager.getPlugInList(
                new AudioFormat(inFormat), new AudioFormat(outFormat), PlugInManager.CODEC);
        assertNotNull(codecs);
        assertEquals(1, codecs.size());
        assertEquals(codecClass.getName(), codecs.get(0));
    }
}