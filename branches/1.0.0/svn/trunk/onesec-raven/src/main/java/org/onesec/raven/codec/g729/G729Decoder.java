/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.codec.g729;

import javax.media.Buffer;
import static javax.media.PlugIn.BUFFER_PROCESSED_OK;
import static javax.media.PlugIn.OUTPUT_BUFFER_NOT_FILLED;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public class G729Decoder extends AbstractCodecExt {
    private static final int L_FRAME = Ld8k.L_FRAME;
    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;
    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = 2 * L_FRAME;
    private Decoder decoder;
    private short[] sp16;
    
    public G729Decoder() {
        super("G.729 Decoder", AudioFormat.class
            , new AudioFormat[] {new AudioFormat(
                    AudioFormat.LINEAR, 8000, 16,1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)
            });

        supportedInputFormats = new AudioFormat[] {
            new AudioFormat(AudioFormat.G729, 8000, 8, 1)};
    }
    
    protected void doClose() {
        sp16 = null;
        decoder = null;
    }

    protected void doOpen() {
        sp16 = new short[L_FRAME];
        decoder = new Decoder();

    }
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        short[] serial = (short[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();

        if (inputLength != SERIAL_SIZE ) {
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED;
        }

        decoder.process(serial, sp16);

        byte[] output = validateByteArraySize(
                    outputBuffer,
                    outputBuffer.getOffset() + OUTPUT_FRAME_SIZE_IN_BYTES);

        writeShorts(sp16, output, outputBuffer.getOffset());
        outputBuffer.setLength(OUTPUT_FRAME_SIZE_IN_BYTES);

        outputBuffer.setFormat(outputFormat);
        int processResult = BUFFER_PROCESSED_OK;

        return processResult;
    }

    private static void writeShorts(
        short[] input,
        byte[] output,
        int outputOffset)
    {
        for (int i=0, o=outputOffset; i<input.length; i++, o+=2)
            ArrayIOUtils.writeShort(input[i], output, o);
    }    
}
