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
import java.util.Arrays;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.control.PacketSizeControl;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 * Breaking class G729Encoder written by Lubomir Marinov on two parts: Encoder, Packetizer
 */
public class G729Packetizer extends AbstractCodecExt implements PacketSizeControl {
    private static final short BIT_1 = Ld8k.BIT_1;
    private static final int L_FRAME = Ld8k.L_FRAME;
    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = L_FRAME / 8;
    
    private int packetSize = 160;
    private int frameCount = packetSize/80;
    
    private int currentFrame;

    public G729Packetizer() {
        super("G.729 Packetizier", AudioFormat.class
            , new AudioFormat[]{new G729AudioFormat(
                    AudioFormat.G729_RTP, new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1))});

        supportedInputFormats = new AudioFormat[]{new G729AudioFormat(
                    AudioFormat.G729, new AudioFormat(AudioFormat.G729, 8000, 8, 1))
        };

        this.controls = new Object[]{this};
    }
    
    @Override
    public Format setOutputFormat(Format format)
    {
        if (format instanceof AudioFormat && AudioFormat.G729_RTP.equals(format.getEncoding()))
            format = new G729AudioFormat(AudioFormat.G729_RTP, format);
        return super.setOutputFormat(format);
    }
    
    @Override
    protected void discardOutputBuffer(Buffer outputBuffer)
    {
        super.discardOutputBuffer(outputBuffer);
        currentFrame = 0;
    }
    
    protected void doOpen() throws ResourceUnavailableException {
        currentFrame = 0;
    }

    @Override
    protected void doClose() {
    }

    @Override
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        short[] serial = (short[]) inputBuffer.getData();

        byte[] output = validateByteArraySize(
                    outputBuffer,
                    outputBuffer.getOffset() + frameCount * OUTPUT_FRAME_SIZE_IN_BYTES);

        packetize(serial, output, outputBuffer.getOffset() + OUTPUT_FRAME_SIZE_IN_BYTES * currentFrame);
        
        outputBuffer.setLength(outputBuffer.getLength() + OUTPUT_FRAME_SIZE_IN_BYTES);
        outputBuffer.setFormat(outputFormat);

        int processResult = BUFFER_PROCESSED_OK;
        if (currentFrame == (frameCount-1))
            currentFrame = 0;
        else {
            ++currentFrame;
            processResult |= OUTPUT_BUFFER_NOT_FILLED;
        }
        return processResult;
    }    

    private void packetize(short[] serial, byte[] outputFrame, int outputFrameOffset)
    {
        Arrays.fill(outputFrame, outputFrameOffset, outputFrameOffset + L_FRAME / 8, (byte) 0);

        for (int s = 0; s < L_FRAME; s++)
            if (BIT_1 == serial[2 + s])
            {
                int o = outputFrameOffset + s / 8;
                int output = outputFrame[o];

                output |= 1 << (7 - (s % 8));
                outputFrame[o] = (byte) (output & 0xFF);
            }
    }
    

    public int setPacketSize(int numBytes)
    {
        packetSize = numBytes;
        frameCount = packetSize/80;
        return packetSize;
    }

    public int getPacketSize() 
    {
        return packetSize;
    }

    public Component getControlComponent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
