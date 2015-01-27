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
import static javax.media.PlugIn.INPUT_BUFFER_NOT_CONSUMED;
import static javax.media.PlugIn.OUTPUT_BUFFER_NOT_FILLED;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 * Breaking class G729Decoder written by Lubomir Marinov on two parts: Decoder, Depacketizer
 */
public class G729Depacketizer extends AbstractCodecExt {
    private static final short BIT_0 = Ld8k.BIT_0;
    private static final short BIT_1 = Ld8k.BIT_1;
    private static final int L_FRAME = Ld8k.L_FRAME;
    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;
    private static final short SIZE_WORD = Ld8k.SIZE_WORD;
    private static final short SYNC_WORD = Ld8k.SYNC_WORD;
    private static final int INPUT_FRAME_SIZE_IN_BYTES = L_FRAME / 8;
    
    private short[] serial;   
    
    public G729Depacketizer() {
        super("G.729 Depacketizer", AudioFormat.class
            , new AudioFormat[] {new AudioFormat(AudioFormat.G729, 8000, 8, 1)
            });
        supportedInputFormats = new AudioFormat[] {new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1)};
    }
    
    protected void doOpen() {
        serial = new short[SERIAL_SIZE];
    }
    
    protected void doClose() {
        serial = null;
    }
    
    private void depacketize(byte[] inputFrame, int inputFrameOffset, short[] serial) {
        serial[0] = SYNC_WORD;
        serial[1] = SIZE_WORD;
        for (int s = 0; s < L_FRAME; s++) {
            int input = inputFrame[inputFrameOffset + s / 8];
            input &= 1 << (7 - (s % 8));
            serial[2 + s] = (0 != input) ? BIT_1 : BIT_0;
        }
    }    
    
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer) {
        byte[] input = (byte[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();

        if (inputLength < INPUT_FRAME_SIZE_IN_BYTES) {
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED;
        }

        int inputOffset = inputBuffer.getOffset();
        depacketize(input, inputOffset, serial);
        
        inputLength -= INPUT_FRAME_SIZE_IN_BYTES;
        inputBuffer.setLength(inputLength);
        inputOffset += INPUT_FRAME_SIZE_IN_BYTES;
        inputBuffer.setOffset(inputOffset);
        
        outputBuffer.setLength(SERIAL_SIZE);
        outputBuffer.setOffset(0);
        outputBuffer.setData(serial);
        outputBuffer.setFormat(outputFormat);
        
        int processResult = BUFFER_PROCESSED_OK;

        if (inputLength > 0)
            processResult |= INPUT_BUFFER_NOT_CONSUMED;
        return processResult;
    }    
}
