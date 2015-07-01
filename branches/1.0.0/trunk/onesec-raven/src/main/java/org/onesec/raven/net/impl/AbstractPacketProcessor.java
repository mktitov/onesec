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
package org.onesec.raven.net.impl;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.net.ByteBufferHolder;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractPacketProcessor implements PacketProcessor {
    public static enum ProcessResult {CONT, STOP};
    
    protected final AtomicBoolean validFlag = new AtomicBoolean(true);
    private final SocketAddress address;
    private final boolean needInboundProcessing;
    private final boolean needOutboundProcessing;
    private final boolean serverSideProcessor;
    private final boolean datagramProcessor;
    private final String desc;
    protected final ByteBuffer inBuffer;
    protected final ByteBuffer outBuffer;
    private final ByteBufferHolder inBufferHolder;
    private final ByteBufferHolder outBufferHolder;
    protected final LoggerHelper logger;
    private boolean stoppingOutboundProcessing = false;
    
    private final AtomicBoolean processing = new AtomicBoolean(false);

    public AbstractPacketProcessor(SocketAddress address, boolean needInboundProcessing
            , boolean needOutboundProcessing, boolean serverSideProcessor, boolean datagramProcessor
            , String desc
            , LoggerHelper logger
            , ByteBufferPool bufferPool
            , int bufferSize) 
    {
        this.address = address;
        this.needInboundProcessing = needInboundProcessing;
        this.needOutboundProcessing = needOutboundProcessing;
        this.serverSideProcessor = serverSideProcessor;
        this.datagramProcessor = datagramProcessor;
        this.desc = desc+" /"+address.toString()+(datagramProcessor?" (UDP)":" (TCP)")+"/";
        this.logger = logger;
        this.inBufferHolder = needInboundProcessing? bufferPool.getBuffer(bufferSize) : null;
        this.inBuffer = needInboundProcessing? inBufferHolder.getBuffer() : null;
        this.outBufferHolder = needOutboundProcessing? bufferPool.getBuffer(bufferSize) : null;
        this.outBuffer = needOutboundProcessing? outBufferHolder.getBuffer() : null;
        if (needOutboundProcessing)
            this.outBuffer.flip();
    }
    
    public boolean isValid() {
        return validFlag.get();
    }

    public void processInboundBuffer(ReadableByteChannel channel) {
        try {
            ProcessResult processRes = null;
            if (datagramProcessor) {
                SocketAddress addr = ((DatagramChannel)channel).receive(inBuffer);
                if (addr!=null)
                    processRes = doProcessInboundBuffer((ByteBuffer)inBuffer.flip());
            } else {
//                logger.debug("Buffer remaining: "+inBuffer.remaining()+":"+inBuffer.capacity());
                int res = channel.read(inBuffer);
//                logger.debug("Processing read operation. "+res);
                if (res==-1) 
                    processRes = doProcessInboundBuffer(null);
                else if (res>0)
                    processRes = doProcessInboundBuffer((ByteBuffer)inBuffer.flip());
            }
            if (processRes == ProcessResult.STOP)
                stop();
        } catch (Throwable ex) {
            if (logger.isErrorEnabled())
                logger.error("Error processing inbound packet", ex);
        }
    }

    public void processOutboundBuffer(WritableByteChannel channel) {
        try {
//            logger.debug("PROCESSING OUTBOUND OPERATION. Has remaining "+outBuffer.hasRemaining());
            if (!stoppingOutboundProcessing) {
                ProcessResult res = doProcessOutboundBuffer(outBuffer);
                outBuffer.flip();
                channel.write(outBuffer);
                if (res==ProcessResult.STOP) {
                    if (outBuffer.hasRemaining())
                        stoppingOutboundProcessing = true;
                    else
                        stop();
                }
            } else {
                channel.write(outBuffer);
                if (!outBuffer.hasRemaining())
                    stop();
            }
        } catch (Throwable ex) {
            if (logger.isErrorEnabled())
                logger.error("Error processing outbound packet", ex);
        }
    }

    public boolean hasPacketForOutboundProcessing() {
        return needOutboundProcessing && (outBuffer.hasRemaining() || containsPacketsForOutboundProcessing());
    }
    
    protected abstract ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception;
    protected abstract ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception;
    protected abstract boolean containsPacketsForOutboundProcessing();

    public boolean isNeedInboundProcessing() {
        return needInboundProcessing;
    }

    public boolean isNeedOutboundProcessing() {
        return needOutboundProcessing;
    }

    public boolean isServerSideProcessor() {
        return serverSideProcessor;
    }

    public boolean isDatagramProcessor() {
        return datagramProcessor;
    }

    public void stopUnexpected(Throwable e) {
        try {
            if (!validFlag.compareAndSet(true, false))
                return;
            if (logger.isErrorEnabled())
                logger.error("Unexpected processing stop", e);
            doStopUnexpected(e);
        } finally {
            releaseResources();
        }
    }

    public void stop() {
        if (validFlag.compareAndSet(true, false)) 
            releaseResources();
    }
    
    private void releaseResources() {
        if (inBufferHolder!=null)
            inBufferHolder.release();
        if (outBufferHolder!=null)
            outBufferHolder.release();
    }
    
    protected abstract void doStopUnexpected(Throwable e);

    public SocketAddress getAddress() {
        return address;
    }

    public boolean isProcessing() {
        return processing.get();
    }

    public boolean changeToProcessing() {
        return processing.compareAndSet(false, true);
    }

    public void changeToUnprocessing() {
        processing.compareAndSet(true, false);
    }

    @Override
    public String toString() {
        return desc;
    }
}
