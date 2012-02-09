/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecConfig;

/**
 *
 * @author Mikhail Titov
 */
public class TranscoderDataStream implements PushBufferStream, BufferTransferHandler {
    
    private final static int CONT_STATE = Codec.INPUT_BUFFER_NOT_CONSUMED;
    private final static byte[] EMPTY_BUFFER = new byte[0];
    private final static ContentDescriptor CONTENT_DESCRIPTOR = 
            new ContentDescriptor(ContentDescriptor.RAW);
    
    private final CodecState[] codecs;
    private final PushBufferStream sourceStream;
    private final Format outFormat;
    private BufferTransferHandler transferHandler;
    private volatile Buffer bufferToSend;
    private volatile boolean endOfStream = false;

    public TranscoderDataStream(CodecConfig[] codecChain, Format outFormat, PushBufferStream sourceStream)
    {
        this.outFormat = outFormat;
        this.codecs = new CodecState[codecChain.length];
        sourceStream.setTransferHandler(this);
        this.sourceStream = sourceStream;
        for (int i=0; i<codecChain.length; ++i)
            codecs[i] = new CodecState(codecChain[i].getCodec());
    }

    public Format getFormat() {
        return outFormat;
    }

    public void read(Buffer buffer) throws IOException {
        Buffer _buf = bufferToSend;
        System.out.println(" <<<<< BUF OUT >>>>>>");
        if (_buf!=null) {
            buffer.copy(_buf);
            if (_buf.isEOM())
                endOfStream = true;
        } else
            buffer.setDiscard(true);
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return CONTENT_DESCRIPTOR;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public boolean endOfStream() {
        return endOfStream;
    }

    public Object[] getControls() {
        return null;
    }

    public Object getControl(String controlType) {
        return null;
    }

    public void transferData(PushBufferStream stream) {
        Buffer buf = new Buffer();
        try {
            stream.read(buf);
            System.out.println(" >>>> BUF IN <<<<<");
            processBufferByCodec(buf, 0);
        } catch (IOException ex) {
        }
    }
    
    private void processBufferByCodec(Buffer buf, int codecInd) {
        if (buf.isEOM())
            System.out.println("  !!!  EOM   !!!");
        if (codecInd>=codecs.length) {
            bufferToSend = buf;
            BufferTransferHandler handler = transferHandler;
            if (handler!=null)
//            if (handler!=null && !buf.isDiscard() || buf.getData()!=null)
                handler.transferData(this);
        } else {
//            if (buf.getData()==null)
//                buf.setData(EMPTY_BUFFER);
            CodecState state = codecs[codecInd];
            int res=0;
            do {
//                Buffer buf2 = new Buffer();
//                buf2.copy(buf);
                long startTs = System.currentTimeMillis();
//                try {
                    res = state.codec.process(buf, state.getOrCreateOutBuffer());
//                                    Thread.sleep(0, 500);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(TranscoderDataStream.class.getName()).log(Level.SEVERE, null, ex);
//                }
                System.out.println("Processing time is: "+(System.currentTimeMillis()-startTs));
//                state.outBuffer.setFormat(buf.getFormat());
//                state.outBuffer.setSequenceNumber(buf.getSequenceNumber());
//                state.outBuffer.setData(buf.getData());
//                state.outBuffer.setTimeStamp(buf.getTimeStamp());
//                state.outBuffer = buf;
                if (buf.isEOM())
                    state.outBuffer.setEOM(true);
                if ( (res & Codec.OUTPUT_BUFFER_NOT_FILLED)==0 || (buf.isEOM() && (res & CONT_STATE)==0) ) 
                    processBufferByCodec(state.getAndResetOutBuffer(), codecInd+1);
            } while ( (res & CONT_STATE) == CONT_STATE);
        }
    }
    
    private class CodecState {
        final Codec codec;
        Buffer inBuffer;
        Buffer outBuffer;

        public CodecState(Codec codec) {
            this.codec = codec;
        }

        private Buffer getOrCreateOutBuffer() {
            if (outBuffer==null) 
                outBuffer = new Buffer();
            return outBuffer;
        }
        
        private Buffer getAndResetOutBuffer() {
            Buffer res = outBuffer;
            outBuffer = null;
            return res;
        }
    }
}
