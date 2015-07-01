/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.onesec.raven.codec.g729;

import java.awt.Component;
import java.util.*;

import javax.media.*;
import javax.media.control.PacketSizeControl;
import javax.media.format.*;

/**
 * @author Lubomir Marinov
 */
public class G729FullEncoder extends AbstractCodecExt implements PacketSizeControl
{
    private static final short BIT_1 = Ld8k.BIT_1;

    private static final int L_FRAME = Ld8k.L_FRAME;
//    private static final int L_FRAME = 160;

    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;
//    private static final int SERIAL_SIZE = 162;

    private static final int INPUT_FRAME_SIZE_IN_BYTES = 2 * L_FRAME;

    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = L_FRAME / 8;
//    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = 20;

    private int packetSize = 160;
    private int frameCount = packetSize/80;
    private int currentFrame;

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
    public G729FullEncoder() {
        super("G.729 Encoder", AudioFormat.class
            , new AudioFormat[]{new G729AudioFormat(
                    AudioFormat.G729_RTP, new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1))});

        supportedInputFormats = new AudioFormat[]{
            new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)
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

    /*
     * Implements AbstractCodecExt#doClose().
     */
    protected void doClose()
    {
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

        currentFrame = 0;
    }

    /*
     * Implements AbstractCodecExt#doProcess(Buffer, Buffer).
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
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

//        byte[] output = validateByteArraySize(
//                    outputBuffer,
//                    outputBuffer.getOffset() + 2 * OUTPUT_FRAME_SIZE_IN_BYTES);
        byte[] output = validateByteArraySize(
                    outputBuffer,
                    outputBuffer.getOffset() + frameCount * OUTPUT_FRAME_SIZE_IN_BYTES);

        packetize(serial, output, outputBuffer.getOffset() + OUTPUT_FRAME_SIZE_IN_BYTES * currentFrame);
        
        outputBuffer.setLength(outputBuffer.getLength() + OUTPUT_FRAME_SIZE_IN_BYTES);

        outputBuffer.setFormat(outputFormat);

        int processResult = BUFFER_PROCESSED_OK;

        if (currentFrame == (frameCount-1))
            currentFrame = 0;
        else
        {
            ++currentFrame;
            processResult |= OUTPUT_BUFFER_NOT_FILLED;
        }
        if (inputLength > 0)
            processResult |= INPUT_BUFFER_NOT_CONSUMED;
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

    private static int readShorts(byte[] input, int inputOffset, short[] output, int outputOffset, int outputLength)
    {
        for (int o=outputOffset, i=inputOffset; o<outputLength; o++, i+=2)
            output[o] = ArrayIOUtils.readShort(input, i);
        return outputLength;
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

    public Component getControlComponent()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}