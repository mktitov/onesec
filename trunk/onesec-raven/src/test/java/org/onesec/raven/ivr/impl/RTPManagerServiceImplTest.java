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

import java.util.Vector;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.rtp.RTPManager;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.codec.AlawAudioFormat;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.ivr.RTPManagerService;

/**
 *
 * @author Mikhail Titov
 */
public class RTPManagerServiceImplTest extends OnesecRavenTestCase
{
    @Test
    public void codecsTest()
    {
        checkCodec(AudioFormat.ULAW, AudioFormat.ULAW_RTP, UlawPacketizer.class);
        checkCodec(AudioFormat.LINEAR, AudioFormat.ALAW, AlawEncoder.class);
        checkCodec(AudioFormat.ALAW, AlawAudioFormat.ALAW_RTP, AlawPacketizer.class);
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