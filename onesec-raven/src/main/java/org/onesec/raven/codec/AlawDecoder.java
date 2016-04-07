/*
 * Copyright 2016 Mikhail Titov.
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

import com.ibm.media.codec.audio.AudioCodec;
import com.sun.media.controls.SilenceSuppressionAdapter;
import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public class AlawDecoder extends AudioCodec {

    private static final byte[] lutTableL = new byte[256];
    private static final byte[] lutTableH = new byte[256];
    
    static {
        for (int i = 0; i < 256; ++i) {
            int input = i ^ 0x55;
            int mantissa = (input & 0xF) << 4;
            int segment = (input & 0x70) >> 4;
            int value = mantissa + 8;

            if (segment >= 1) {
                value += 256;
            }
            if (segment > 1) {
                value <<= segment - 1;
            }
            if ((input & 0x80) == 0) {
                value = -value;
            }
            lutTableL[i] = (byte) value;
            lutTableH[i] = (byte) (value >> 8);
        }        
    }

    public AlawDecoder() {
        this.supportedInputFormats = new AudioFormat[]{new AudioFormat(AudioFormat.ALAW)};
        this.defaultOutputFormats = new AudioFormat[]{new AudioFormat(AudioFormat.LINEAR)};
        this.PLUGIN_NAME = "A-Law Decoder";
    }

    @Override
    protected Format[] getMatchingOutputFormats(Format in) {
        AudioFormat af = (AudioFormat) in;

        this.supportedOutputFormats = new AudioFormat[]{new AudioFormat(AudioFormat.LINEAR, af.getSampleRate(), 16, af.getChannels(), 0, 1)};

        return this.supportedOutputFormats;
    }

    @Override
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (!super.checkInputBuffer(inputBuffer)) {
            return 1;
        }

        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return 0;
        }

//        int channels = this.outputFormat.getChannels();
        byte[] inData = (byte[]) inputBuffer.getData();
        byte[] outData = super.validateByteArraySize(outputBuffer, inData.length * 2);

        int inpLength = inputBuffer.getLength();
        int outLength = 2 * inpLength;

        int inOffset = inputBuffer.getOffset();
        int outOffset = outputBuffer.getOffset();
        for (int i = 0; i < inpLength; ++i) {
            int temp = inData[(inOffset++)] & 0xFF;
            outData[(outOffset++)] = lutTableL[temp];
            outData[(outOffset++)] = lutTableH[temp];
        }

        updateOutput(outputBuffer, this.outputFormat, outLength, 0);

        return 0;
    }

    @Override
    public Object[] getControls() {
        if (this.controls == null) {
            this.controls = new Control[1];
            this.controls[0] = new SilenceSuppressionAdapter(this, false, false);
        }
        return (Object[]) this.controls;
    }
}
