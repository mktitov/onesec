/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.onesec.raven.codec.g729;

import javax.media.*;
import javax.media.format.AudioFormat;

/**
 * @author Lubomir Marinov
 */
public abstract class AbstractCodecExt extends com.ibm.media.codec.audio.AudioCodec
{
    private final Class<? extends Format> formatClass;

    private final String name;

    private final AudioFormat[] suppOutputFormats;

    protected AbstractCodecExt(
        String name,
        Class<? extends Format> formatClass,
        AudioFormat[] supportedOutputFormats)
    {
        this.formatClass = formatClass;
        this.name = name;
        this.suppOutputFormats = supportedOutputFormats;
        this.defaultOutputFormats = supportedOutputFormats;
    }

    @Override
    public void close()
    {
        if (!opened)
            return;

        doClose();

        opened = false;
        super.close();
    }

    protected void discardOutputBuffer(Buffer outputBuffer)
    {
        outputBuffer.setDiscard(true);
    }

    protected abstract void doClose();

    protected abstract void doOpen()
        throws ResourceUnavailableException;

    protected abstract int doProcess(Buffer inputBuffer, Buffer outputBuffer);

    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        System.out.println("   >>>> getMatchingOutputFormats: "+inputFormat);
        if (suppOutputFormats != null)
            return suppOutputFormats.clone();
        this.supportedInputFormats = (AudioFormat[]) suppOutputFormats;
        return new Format[0];
    }

    @Override
    public String getName()
    {
        return (name == null) ? super.getName() : name;
    }

    /*
     * Implements AbstractCodec#getSupportedOutputFormats(Format).
     */
//    @Override
//    public Format[] getSupportedOutputFormats(Format inputFormat)
//    {
//        if (inputFormat == null)
//            return suppOutputFormats;
//
//        if (!formatClass.isInstance(inputFormat)
//                || (null == matches(inputFormat, suppOutputFormats)))
//        if ((null == matches(inputFormat, suppOutputFormats)))
//            return new Format[0];
//
//        return getMatchingOutputFormats(inputFormat);
//    }

    /**
     * Utility to perform format matching.
     */
    public static Format matches(Format in, Format outs[])
    {
        for (Format out : outs)
            if (in.matches(out))
                return out;
        return null;
    }

    @Override
    public void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        doOpen();

        opened = true;
        super.open();
    }

    /*
     * Implements AbstractCodec#process(Buffer, Buffer).
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        if (inputBuffer.isDiscard())
        {
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        return doProcess(inputBuffer, outputBuffer);
    }

    @Override
    public Format setInputFormat(Format format)
    {
        System.out.println("-------setInputFormat: "+format);
        Format res = null;
//        if (!formatClass.isInstance(format) || (null == matches(format, inputFormats))){
        if (!formatClass.isInstance(format) || (null == matches(format, supportedInputFormats))){
            System.out.println("  not matches: ");
            System.out.println("  instance of AudioFormat: "+(formatClass.isInstance(format)));
            System.out.println("  inputFormats length: "+supportedInputFormats.length);
            return null;
        }

        res = super.setInputFormat(format);
        System.out.println("  format: "+res);
        return res;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
//        if (!formatClass.isInstance(format)
//                || (null == matches(format, getMatchingOutputFormats(inputFormat))))
//            return null;

        return super.setOutputFormat(format);
    }

    @Override
    protected byte[] validateByteArraySize(Buffer buffer, int newSize)
    {
        Object data = buffer.getData();
        byte[] newBytes;

        if (data instanceof byte[])
        {
            byte[] bytes = (byte[]) data;

            if (bytes.length >= newSize)
                return bytes;

            newBytes = new byte[newSize];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        }
        else
        {
            newBytes = new byte[newSize];
            buffer.setLength(0);
            buffer.setOffset(0);
        }

        buffer.setData(newBytes);
        return newBytes;
    }
}