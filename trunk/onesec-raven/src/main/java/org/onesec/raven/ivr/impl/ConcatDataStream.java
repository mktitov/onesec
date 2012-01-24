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
import org.raven.log.LogLevel;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataStream implements PushBufferStream, BufferTransferHandler, Task
{
    public static int MAX_SILENCE_BUFFER_COUNT = 1500;

    private final Buffer silentBuffer;
    private final Queue<Buffer> bufferQueue;
    private final ConcatDataSource dataSource;
    private final ContentDescriptor contentDescriptor;
    private final Node owner;
    private final int packetLength; //ms?
    private final int maxSendAheadPacketsCount;
    private BufferTransferHandler transferHandler;
    private Buffer bufferToSend;
    private String action;
    private long packetNumber;
    private long sleepTime;
    private AtomicInteger silencePacketCount = new AtomicInteger(0);
    private String logPrefix;
    private AtomicReference<SourceInfo> sourceInfo = new AtomicReference<SourceInfo>();

    public ConcatDataStream(
            Queue<Buffer> bufferQueue, ConcatDataSource dataSource, Node owner
            , int packetSize, int maxSendAheadPacketsCount, Buffer silentBuffer)
    {
        this.bufferQueue = bufferQueue;
        this.dataSource = dataSource;
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.owner = owner;
        this.packetLength = packetSize/8;
        this.maxSendAheadPacketsCount = maxSendAheadPacketsCount;
        this.silentBuffer = silentBuffer;
    }
    
    public void initNewSource() {
        sourceInfo.set(new SourceInfo());
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

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

    public void transferData(PushBufferStream stream) {
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return String.format(logMess(
                "Transfering buffers to rtp session. Action: %s. packetCount: %s; sleepTime: %s"
                , action, packetNumber, sleepTime));
    }

    public void run() {
        dataSource.setStreamThreadRunning(true);
        try
        {
            long startTime = System.currentTimeMillis();
            packetNumber = 0;
            SourceInfo si = null;
            while ((!dataSource.isClosed() || !bufferQueue.isEmpty())
                    && silencePacketCount.get()<MAX_SILENCE_BUFFER_COUNT)
            {
                try {
                    action = "getting new buffer from queue";
                    bufferToSend = bufferQueue.poll();
                    action = "sending transfer event";
                    if (bufferToSend!=null && (si=sourceInfo.get())!=null) {
                        if ()
                    }
                    transferData(null);
                    ++packetNumber;
                    action = "sleeping";
                    long timeDiff = System.currentTimeMillis()-startTime;
                    long expectedPacketNumber = timeDiff/packetLength;
                    long correction = timeDiff % packetLength;
//                    sleepTime = (packetNumber-expectedPacketNumber-
//                            (bufferToSend==null? 0 : maxSendAheadPacketsCount))*packetLength;
                    sleepTime = (packetNumber-expectedPacketNumber)*packetLength - correction;
                    if (sleepTime>0)
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(logMess(
                                "Transfer buffers to rtp session task was interrupted"), ex);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Transfer buffers to rtp session task was finished"));
        } finally {
            dataSource.setStreamThreadRunning(false);
        }
    }

    private String logMess(String mess, Object... args) {
        return dataSource.logMess(mess, args);
    }
    
    private class SourceInfo {
        private long expectedSourceBufferNumber = 0;
        private long sourceBufferNumber = 0;
    }
}
