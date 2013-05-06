/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.Codec;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataStream implements PushBufferStream, Task
{
    public static final int MAX_QUEUE_SIZE = 7;
    public static int MAX_SILENCE_BUFFER_COUNT = 1500;
    public static final int MAX_TIME_SKEW = 300;

    private final LoggerHelper logger;
    private final Buffer silentBuffer;
    private final Queue<Buffer> bufferQueue;
    private final ConcatDataSource dataSource;
    private final ContentDescriptor contentDescriptor;
    private final Node owner;
    private final int packetLength; //ms?
    
    private BufferTransferHandler transferHandler;
    private Buffer bufferToSend;
    private String action;
    private long packetNumber;
    private long sleepTime;
    private AtomicInteger silencePacketCount = new AtomicInteger(0);
//    private String logPrefix;
    private AtomicReference<ConcatDataSource.SourceProcessor> sourceInfo = 
            new AtomicReference<ConcatDataSource.SourceProcessor>();
    private AtomicInteger emptyQueueEvents = new AtomicInteger(0);

    public ConcatDataStream(
            Queue<Buffer> bufferQueue, ConcatDataSource dataSource, Node owner
            , int packetSize, Codec codec, int maxSendAheadPacketsCount, Buffer silentBuffer, LoggerHelper logger)
    {
        this.logger = logger;
        this.bufferQueue = bufferQueue;
        this.dataSource = dataSource;
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.owner = owner;
        this.packetLength = (int) codec.getMillisecondsForPacketSize(packetSize);
//        this.packetLength = (int) dataSource.getPacketSizeInMillis();
        this.silentBuffer = silentBuffer;
    }
    
    void sourceInitialized(ConcatDataSource.SourceProcessor source) {
        sourceInfo.set(source);
        emptyQueueEvents.set(0);
    }
    
    void sourceClosed(ConcatDataSource.SourceProcessor source) {
        sourceInfo.compareAndSet(source, null);
    }

//    public String getLogPrefix() {
//        return logPrefix;
//    }
//
//    public void setLogPrefix(String logPrefix) {
//        this.logPrefix = logPrefix;
//    }
//
    public Format getFormat() {
        return dataSource.getFormat();
    }

    public void read(Buffer buffer) throws IOException
    {
        action = "reading buffer";
        if (bufferToSend==null) {
            silencePacketCount.incrementAndGet();
            buffer.copy(silentBuffer);
        } else {
            silencePacketCount.set(0);
            buffer.copy(bufferToSend);
        }
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public boolean endOfStream() {
        return bufferQueue.size()==0 && dataSource.isClosed();
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public Object getControl(String controlType) {
        return null;
    }

    private void sendBuffer() {
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return String.format(logger.logMess(
                "Transfering buffers to rtp session. Action: %s. packetCount: %s; sleepTime: %s"
                , action, packetNumber, sleepTime));
    }

    public void run() {
        dataSource.setStreamThreadRunning(true);
        try
        {
            long startTime = System.currentTimeMillis();
            packetNumber = 0;
            ConcatDataSource.SourceProcessor si = null;
            if (logger.isDebugEnabled())
                logger.debug("Concat stream started with time quant {} ms", packetLength);
            boolean debugEnabled = logger.isDebugEnabled();
            long prevTime = System.currentTimeMillis();
            long maxTransferTime = 0;
            long droppedPacketCount = 0;
            long transferTimeSum = 0;
            long emptyBufferEventCount = 0;
            while ((!dataSource.isClosed() || !bufferQueue.isEmpty())
                    && silencePacketCount.get()<MAX_SILENCE_BUFFER_COUNT)
            {
                try {
                    long cycleStartTs = System.currentTimeMillis();
                    action = "getting new buffer from queue";
                    si = sourceInfo.get();
                    bufferToSend = bufferQueue.poll();
                    if (bufferToSend!=null && si!=null && si.isRealTime()) {
                        if (   bufferToSend.getTimeStamp()+MAX_TIME_SKEW<cycleStartTs
                            || bufferQueue.size()>MAX_QUEUE_SIZE) 
                        {
                            droppedPacketCount++;
                            continue;
                        }
                    }
                    action = "sending transfer event";
                    if (bufferToSend!=null || si==null || !si.isRealTime()) {
                        sendBuffer();
                        long tt = System.currentTimeMillis()-cycleStartTs;
                        transferTimeSum+=tt;
                        if (tt>maxTransferTime)
                            maxTransferTime=tt;
                    }
                    ++packetNumber;
                    action = "sleeping";
                    long curTime = System.currentTimeMillis();
                    long timeDiff = curTime - startTime;
                    long expectedPacketNumber = timeDiff/packetLength;
                    long correction = timeDiff % packetLength;
                    emptyBufferEventCount = expectedPacketNumber - packetNumber;
                    if (si!=null && debugEnabled && curTime-prevTime>30000) {
                        prevTime = curTime;
                        logger.debug(String.format(
                                "Empty buffers events count: %s; "
                                + "avgTransferTime: %s; maxTransferTime: %s; buffers size: %s; "
                                + " dropped packets count: %s"
                                , emptyBufferEventCount, transferTimeSum/packetNumber
                                , maxTransferTime
                                , bufferQueue.size()
                                , droppedPacketCount));
                    }
                    sleepTime = (packetNumber-expectedPacketNumber)*packetLength - correction;
                    if (sleepTime>0)
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    if (logger.isErrorEnabled())
                        logger.error("Transfer buffers to rtp session task was interrupted", ex);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (debugEnabled) {
                logger.debug("Transfer buffers to rtp session task was finished");
                logger.debug(String.format(
                        "Empty buffers events count: %s; "
                        + "avgTransferTime: %s; maxTransferTime: %s; buffers size: %s; "
                        + " dropped packets count: %s"
                        , emptyBufferEventCount, transferTimeSum/packetNumber, maxTransferTime
                        , bufferQueue.size(), droppedPacketCount));
            }
        } finally {
            dataSource.setStreamThreadRunning(false);
        }
    }

//    private String logMess(String mess, Object... args) {
//        return dataSource.logMess(mess, args);
//    }
    
    private class SourceInfo {
        private long expectedSourceBufferNumber = 0;
        private long sourceBufferNumber = 0;
    }
}
