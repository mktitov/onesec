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
import java.util.ArrayList;
import java.util.Arrays;
import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecConfig;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class TranscoderDataStream implements PushBufferStream, BufferTransferHandler {
    
    private final static int CONT_STATE = Codec.INPUT_BUFFER_NOT_CONSUMED;
    private final static byte[] EMPTY_BUFFER = new byte[0];
    private final static ContentDescriptor CONTENT_DESCRIPTOR = 
            new ContentDescriptor(ContentDescriptor.RAW);
    
    private final PushBufferStream sourceStream;
//    private final TranscoderDataSource owner;
    private final Logger logger;
//    private final Node node;
    private CodecState[] codecs;
    private Format outFormat;
    private BufferTransferHandler transferHandler;
    private volatile Buffer bufferToSend;
    private volatile boolean endOfStream = false;
    private long cycleStartTs;
    private long buffersCount = 0;
    private long outputBuffersCount = 0;
    private long processingTime = 0;

    public TranscoderDataStream(PushBufferStream sourceStream, Logger logger) {
        this.sourceStream = sourceStream;
        this.logger = logger;
//        this.owner = owner;
//        this.node = node;
    }
    
    void init(CodecConfig[] codecChain, Format outputFormat) {
        this.outFormat = outputFormat;
        this.codecs = new CodecState[codecChain.length];
        sourceStream.setTransferHandler(this);
        for (int i=0; i<codecChain.length; ++i)
            codecs[i] = new CodecState(codecChain[i]);
    }
    
    public Format getFormat() {
        return outFormat;
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
        ArrayList controls = new ArrayList();
        for (CodecState codecState: codecs) {
            Object[] codecControls = codecState.codec.getControls();
            if (codecControls!=null && codecControls.length>0)
                controls.addAll(Arrays.asList(codecControls));
        }
        return controls.isEmpty()? null : controls.toArray();
    }

    public Object getControl(String controlType) {
        for (CodecState codecState: codecs) {
            Object control = codecState.codec.getControl(controlType);
            if (control!=null)
                return control;
        }
        return null;
    }

    public void read(Buffer buffer) throws IOException {
        Buffer _buf = bufferToSend;
        if (_buf!=null) {
            outputBuffersCount++;
            buffer.copy(_buf);
            if (_buf.isEOM()) {
                endOfStream = true;
            }
        } else
            buffer.setDiscard(true);
    }

    public void transferData(PushBufferStream stream) {
        Buffer buf = new Buffer();
        try {
            cycleStartTs = System.nanoTime();
            stream.read(buf);
            buffersCount++;
            processBufferByCodec(buf, 0);
            processingTime += System.nanoTime()-cycleStartTs;
//            String.format
            if (buf.isEOM() && logger.isDebugEnabled())
                logger.debug(String.format(
                        "Source processed. inputBuffersCount: %s; outputBuffersCount: %s; "
                        + "average cycle time: %.4f ms, %s ns"
                        , buffersCount, outputBuffersCount, (double)processingTime/1e9/buffersCount
                        , processingTime/buffersCount));
        } catch (IOException ex) {
        }
    }
    
    private void processBufferByCodec(Buffer buf, int codecInd) {
        if (codecInd>=codecs.length) {
            bufferToSend = buf;
            BufferTransferHandler handler = transferHandler;
            if (handler!=null)
                handler.transferData(this);
        } else {
            CodecState state = codecs[codecInd];
            int res=0;
            do {
                res = state.codec.process(buf, state.getOrCreateOutBuffer());
                if ( (res & Codec.BUFFER_PROCESSED_FAILED)>0) 
                    break;
                if ( (res & Codec.OUTPUT_BUFFER_NOT_FILLED)==0 || (buf.isEOM() && (res & CONT_STATE)==0) ) {
                    if (res==0 && buf.isEOM())
                        state.outBuffer.setEOM(true);
                    if (state.outBuffer.isEOM() && state.outBuffer.getData()==null)
                        state.outBuffer.setData(EMPTY_BUFFER);
                    processBufferByCodec(state.getAndResetOutBuffer(), codecInd+1);
                }
            } while ( (res & CONT_STATE) == CONT_STATE);
        }
    }
    
    private class CodecState {
        final Codec codec;
        final Format inputFormat;
        Buffer inBuffer;
        Buffer outBuffer;

        public CodecState(CodecConfig codecConfig) {
            this.codec = codecConfig.getCodec();
            this.inputFormat = codecConfig.getInputFormat();
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
