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

package org.onesec.raven.ivr;

import com.cisco.jtapi.extensions.CiscoMediaCapability;
import javax.media.format.AudioFormat;
import org.onesec.raven.codec.AlawAudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public enum Codec
{
    AUTO(-1), G711_MU_LAW(0), G711_A_LAW(8), G729(11), LINEAR(100);

    private final int payload;
    private final AudioFormat audioFormat;
    private final CiscoMediaCapability[] ciscoMediaCapabilities;

    private Codec(int payload)
    {
        this.payload = payload;
        switch (payload){
            case 11 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(11, 60)};
                audioFormat = new AudioFormat(AudioFormat.G729_RTP, 8000d, 8, 1);
                break;
            case 8 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(2, 60)};
                audioFormat = new AudioFormat(AlawAudioFormat.ALAW_RTP, 8000d, 8, 1);
                break;
            case 100:
                ciscoMediaCapabilities = null;
                audioFormat = new AudioFormat(AudioFormat.LINEAR, 8000d, 8, 1);
                break;
            case -1 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(2, 60),
                    new CiscoMediaCapability(4, 60), new CiscoMediaCapability(11, 60)};
                audioFormat = null;
                break;
            default : 
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(4, 60)};
                audioFormat = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
                break;
        }
    }

    public int getPayload() {
        return payload;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public CiscoMediaCapability[] getCiscoMediaCapabilities() {
        return ciscoMediaCapabilities;
    }

    public static Codec getCodecByCiscoPayload(int ciscoPayload)
    {
        for (Codec codec: values())
            if (AUTO!=codec && codec.getCiscoMediaCapabilities()[0].getPayloadType()==ciscoPayload)
                return codec;
        return null;
    }
}
