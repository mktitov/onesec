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

package org.onesec.raven.codec;

import javax.media.Format;
import javax.media.format.AudioFormat;
import org.onesec.raven.ivr.Codec;

/**
 *
 * @author Mikhail Titov
 */
public class AlawPacketizer extends UlawPacketizer
{
    public AlawPacketizer()
    {
        this.supportedInputFormats = new AudioFormat[] { 
            new AlawAudioFormat(
                AlawAudioFormat.ALAW,
                new AudioFormat(AudioFormat.ALAW, -1.0D, 8, 1, -1, -1, 8, -1.0D, Format.byteArray)) 
        };

        this.defaultOutputFormats = new AudioFormat[] { 
            new AlawAudioFormat(
                    AlawAudioFormat.ALAW_RTP,
                    new AudioFormat(AlawAudioFormat.ALAW_RTP, -1.0D, 8, 1, -1, -1, 8, -1.0D, Format.byteArray))
        };

        this.PLUGIN_NAME = "A-Law Packetizer";
    }

    @Override
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat)in;

        this.supportedOutputFormats = new AudioFormat[] {
            new AlawAudioFormat(
                    AlawAudioFormat.ALAW_RTP,
                    new AudioFormat(AlawAudioFormat.ALAW_RTP, af.getSampleRate(), 8, 1, -1, -1, 8, -1.0D, Format.byteArray)) 
        };

        return this.supportedOutputFormats;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
        if (format instanceof AudioFormat && AlawAudioFormat.ALAW_RTP.equals(format.getEncoding()))
            format = new AlawAudioFormat(AlawAudioFormat.ALAW_RTP, format);
        return super.setOutputFormat(format);
    }
}
