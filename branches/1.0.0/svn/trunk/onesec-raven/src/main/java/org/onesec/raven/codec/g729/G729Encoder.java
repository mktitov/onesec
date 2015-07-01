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

import java.awt.Component;
import javax.media.Buffer;
import javax.media.Format;
import static javax.media.PlugIn.BUFFER_PROCESSED_OK;
import static javax.media.PlugIn.INPUT_BUFFER_NOT_CONSUMED;
import static javax.media.PlugIn.OUTPUT_BUFFER_NOT_FILLED;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 * Breaking class G729Encoder written by Lubomir Marinov on two parts: Encoder, Packetizer
 */
public class G729Encoder  extends AbstractCodecExt {
    private static final int L_FRAME = Ld8k.L_FRAME;
    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;
    private static final int INPUT_FRAME_SIZE_IN_BYTES = 2 * L_FRAME;

    private Coder coder;


    /**
     * The previous input if it was less than the input frame size and which is
     * to be prepended to the next input in order to form a complete input
     * frame.
     */
    private byte[] prevInput;

    /**
     * The length of the previous input if it was less than the input frame size
     * and which is to be prepended to the next input in order to form a
     * complete input frame.
     */
    private int prevInputLength;

    private short[] serial;

    private short[] sp16;

    /**
     * Initializes a new <code>G729Encoder</code> instance.
     */
    public G729Encoder() {
        super("G.729 Encoder", AudioFormat.class
            , new AudioFormat[]{new G729AudioFormat(
                    AudioFormat.G729, new AudioFormat(AudioFormat.G729, 8000, 8, 1))});

        supportedInputFormats = new AudioFormat[]{
            new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)
        };

        this.controls = new Object[]{this};
    }

    @Override
    public Format setOutputFormat(Format format) {
        if (format instanceof AudioFormat && AudioFormat.G729.equals(format.getEncoding()))
            format = new G729AudioFormat(AudioFormat.G729, format);
        return super.setOutputFormat(format);
    }

    /*
     * Implements AbstractCodecExt#doClose().
     */
    protected void doClose() {
        prevInput = null;
        prevInputLength = 0;

        sp16 = null;
        serial = null;
        coder = null;
    }

    /*
     * Implements AbstractCodecExt#doOpen().
     */
    protected void doOpen() throws ResourceUnavailableException {
        prevInput = new byte[INPUT_FRAME_SIZE_IN_BYTES];
        prevInputLength = 0;

        sp16 = new short[L_FRAME];
        serial = new short[SERIAL_SIZE];
        coder = new Coder();
    }

    /*
     * Implements AbstractCodecExt#doProcess(Buffer, Buffer).
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer) {
        byte[] input = (byte[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();
        int inputOffset = inputBuffer.getOffset();

        if ((prevInputLength + inputLength) < INPUT_FRAME_SIZE_IN_BYTES)
        {
            System.arraycopy(
                input,
                inputOffset,
                prevInput,
                prevInputLength,
                inputLength);
            prevInputLength += inputLength;
            return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED;
        }

        int readShorts = 0;

        if (prevInputLength > 0)
        {
            readShorts += readShorts(prevInput, 0, sp16, 0, prevInputLength / 2);
            prevInputLength = 0;
        }
        readShorts = readShorts(input, inputOffset, sp16, readShorts, sp16.length - readShorts);

        int readBytes = 2 * readShorts;

        inputLength -= readBytes;
        inputBuffer.setLength(inputLength);
        inputOffset += readBytes;
        inputBuffer.setOffset(inputOffset);

        coder.process(sp16, serial);        
        
        outputBuffer.setLength(SERIAL_SIZE);
        outputBuffer.setFormat(outputFormat);
        outputBuffer.setData(serial);

        int processResult = BUFFER_PROCESSED_OK;

        if (inputLength > 0)
            processResult |= INPUT_BUFFER_NOT_CONSUMED;
        return processResult;
    }


    private static int readShorts(byte[] input, int inputOffset, short[] output, int outputOffset, int outputLength)
    {
        for (int o=outputOffset, i=inputOffset; o<outputLength; o++, i+=2)
            output[o] = ArrayIOUtils.readShort(input, i);
        return outputLength;
    }

    public Component getControlComponent()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
}
