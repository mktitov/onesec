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
package org.onesec.raven.net.sip.impl;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.impl.AbstractPacketProcessor;
import org.onesec.raven.net.sip.SipMessage;
import org.onesec.raven.net.sip.SipMessageProcessor;
import org.onesec.raven.net.sip.SipPacketProcessor;
import org.onesec.raven.net.sip.SipRequest;
import org.onesec.raven.net.sip.SipResponse;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class SipPacketProcessorImpl extends AbstractPacketProcessor implements SipPacketProcessor {
    private final static String SIP_VERSION = "SIP/2.0";
    private final static String HEAD_ENCODING = "UTF-8";
    private final SipMessageProcessor messageProcessor;
    private final Queue<MessLines> outboundQueue = new ConcurrentLinkedQueue<MessLines>();
    private final byte[] decodeBuffer = new byte[1024];
    private MessageDecoder messageDecoder;

    public SipPacketProcessorImpl(SipMessageProcessor messageProcessor, SocketAddress address
            , boolean serverSideProcessor, boolean datagramProcessor, String desc
            , LoggerHelper logger, ByteBufferPool bufferPool
            , int bufferSize) 
    {
        super(address, true, true, serverSideProcessor, datagramProcessor, desc, logger, bufferPool, bufferSize);
        this.messageProcessor = messageProcessor;
    }

    @Override
    protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
        while (buffer.hasRemaining()) {
            if (messageDecoder==null)
                messageDecoder = new MessageDecoder();
            SipMessage mess = messageDecoder.decode(buffer);
            if (mess!=null) {
                if (mess instanceof SipResponse)
                    messageProcessor.processResponse((SipResponse)mess);
                else
                    messageProcessor.processRequest((SipRequest)mess);
                messageDecoder = null;
            }
        }
        return ProcessResult.CONT;
    }
    

    @Override
    protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
        buffer.compact();
        MessLines mess = outboundQueue.peek();
        while (!mess.lines.isEmpty()) {
            byte[] line = mess.lines.peek();
            if (buffer.remaining() >= line.length) {
                buffer.put(line);
                mess.lines.poll();
            } else break;
        }
        if (mess.lines.isEmpty())
            outboundQueue.poll();
        return ProcessResult.CONT;
    }

    @Override
    protected boolean containsPacketsForOutboundProcessing() {
        return !outboundQueue.isEmpty();
    }

    @Override
    protected void doStopUnexpected(Throwable e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendMessage(SipMessage request) {
        outboundQueue.offer(new MessLines(request));
    }

    private class MessLines {
        private final Queue<byte[]> lines;

        public MessLines(SipMessage mess) {
            this.lines = mess.getLines();
        }
    }
    
    private class MessageDecoder {
        private SipMessage message;
        private String contentEncoding;
        private String encoding;
        private boolean decodingContent;
        private int contentLength;
        private StringBuilder content;
        
        private SipMessage decode(ByteBuffer buffer) throws Exception {
            if (!decodingContent)
                decodeHeaders(buffer);
            if (decodingContent && contentLength>0)
                if (decodeContent(buffer))
                    return createSipMessage();
            return null;
        }
        
        private SipMessage createSipMessage() {
            return null;
        }
        
        private void decodeHeaders(ByteBuffer buffer) throws Exception {
            int pos = buffer.position();
            while (buffer.hasRemaining() && !decodingContent) {
                if (buffer.get() == 13 && buffer.hasRemaining() && buffer.get()==10) {
                    int len = buffer.position()-pos-2;
                    buffer.position(pos);
                    buffer.get(decodeBuffer, 0, len);
                    decodeHeaderOrMessageTypeFromLine(new String(decodeBuffer, 0, len, HEAD_ENCODING));
                    buffer.get(); buffer.get();
                    pos = buffer.position();
                }
            }
            if (pos!=buffer.position())
                buffer.position(pos);
        }

        private void decodeHeaderOrMessageTypeFromLine(String line) throws Exception {
            if (line.isEmpty()) 
                decodingContent = true;
            else {
                if (message == null) 
                    createMessage(line);
                else 
                    createAndAddHeader(line);
            }
        }
        
        private void createMessage(String line) throws Exception {
            String[] toks = line.split(line);
            if (toks.length!=3) 
                throw new Exception(String.format(
                        "Error processing start line (%s). Invalid number of elements expected %s but was %s."
                        , line, 3, toks.length));
            if (SIP_VERSION.equals(toks[0]))
                message = new SipResponseImpl(Integer.parseInt(toks[1]), toks[2]);
            else 
                message = new SipRequestImpl(toks[0], toks[1]);
        }
        
        private void createAndAddHeader(String line) {
            
        }

        private boolean decodeContent(ByteBuffer buffer) throws Exception {
            int bytesToRead = buffer.remaining()>contentLength? contentLength : buffer.remaining();
            if (bytesToRead>0) {
                buffer.get(decodeBuffer, 0, bytesToRead);
                content.append(new String(decodeBuffer, 0, bytesToRead, contentEncoding));
                contentLength -= bytesToRead;
            }
            return contentLength==0;
        }
    }
}
