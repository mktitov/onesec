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
package org.onesec.raven.net.sip_0.impl_0;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.impl.AbstractPacketProcessor;
import org.onesec.raven.net.sip_0.SipMessage;
import org.onesec.raven.net.sip_0.SipMessageProcessor;
import org.onesec.raven.net.sip_0.SipPacketProcessor;
import org.onesec.raven.net.sip_0.SipRequest;
import org.onesec.raven.net.sip_0.SipResponse;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class SipPacketProcessorImpl extends AbstractPacketProcessor implements SipPacketProcessor {
    private final SipMessageProcessor messageProcessor;
    private final Queue<MessLines> outboundQueue = new ConcurrentLinkedQueue<MessLines>();
    private final byte[] decodeBuffer = new byte[1024];
    private SipMessageDecoderImpl messageDecoder;

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
        try {
            if (messageDecoder==null)
                messageDecoder = new SipMessageDecoderImpl(logger, decodeBuffer);
            SipMessage mess = messageDecoder.decode(buffer);
            if (mess!=null) {
                if (mess instanceof SipResponse)
                    messageProcessor.processResponse((SipResponse)mess);
                else
                    messageProcessor.processRequest((SipRequest)mess);
                messageDecoder = null;
            }
            return ProcessResult.CONT;
        } finally {
            buffer.clear();
        }
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
//        outboundQueue.offer(new MessLines(request));
    }

    private class MessLines {
        private  Queue<byte[]> lines;

        public MessLines(SipMessage mess) {
//            this.lines = mess.getLines();
        }
    }
}
