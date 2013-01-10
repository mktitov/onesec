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
package org.onesec.raven.codec;

import com.sun.media.BasicPlugIn;
import com.sun.media.codec.audio.ulaw.DePacketizer;
import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public class AlawDepacketizer extends DePacketizer {
    private final static String PLUGIN_NAME = "A-Law DePacketizer";

    public AlawDepacketizer() {
        this.inputFormats = new Format[] { new AudioFormat("ALAW/rtp") };
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }
    
    @Override
    public Format[] getSupportedOutputFormats(Format in) {
        if (in == null) 
            return new Format[]{new AudioFormat("ULAW")};
        if (BasicPlugIn.matches(in, this.inputFormats) == null) 
            return new Format[1];
        if (!(in instanceof AudioFormat)) 
            return new Format[]{new AudioFormat("ULAW")};
        AudioFormat af = (AudioFormat) in;
        return new Format[]{new AudioFormat("ALAW", af.getSampleRate(), af.getSampleSizeInBits(), 
                af.getChannels())};
    }
}
