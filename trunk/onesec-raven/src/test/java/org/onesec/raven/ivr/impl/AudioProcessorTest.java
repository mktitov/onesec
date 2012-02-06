/*
 * Copyright 2012 Mikhail Titov.
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

import java.util.Vector;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class AudioProcessorTest extends OnesecRavenTestCase {

    @Test
    public void getCodecChainTest() {
        checkCodec(AudioFormat.LINEAR, AudioFormat.ULAW_RTP);
    }
    
    private void checkCodec(String inFormat, String outFormat)
    {
        Vector codecs = PlugInManager.getPlugInList(
                new AudioFormat(inFormat), new AudioFormat(outFormat), PlugInManager.CODEC);
        assertNotNull(codecs);
        for (Object codec: codecs)
            System.out.println("!!! Codec: "+codec);
//        assertEquals(1, codecs.size());
    }
    
}
