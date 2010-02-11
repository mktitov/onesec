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

    private final Queue<Buffer> bufferQueue;
    private final ConcatDataSource dataSource;
    private final ContentDescriptor contentDescriptor;
    private final Node owner;
    private final int packetLength;
    private final int maxSendAheadPacketsCount;
    private BufferTransferHandler transferHandler;
    private Buffer silentBuffer;
    private Buffer bufferToSend;
    private String action;
    private long packetNumber;
    private long sleepTime;
    private AtomicInteger silencePacketCount = new AtomicInteger(0);

    public ConcatDataStream(
            Queue<Buffer> bufferQueue, ConcatDataSource dataSource, Node owner
            , int packetSize, int maxSendAheadPacketsCount)
    {
        this.bufferQueue = bufferQueue;
        this.dataSource = dataSource;
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.owner = owner;
        this.packetLength = packetSize/8;
        this.maxSendAheadPacketsCount = maxSendAheadPacketsCount;
    }

    public Format getFormat()
    {
        return dataSource.getFormat();
    }

    public void read(Buffer buffer) throws IOException
    {
//        Buffer queueBuf = bufferQueue.poll();
        action = "reading buffer";
        if (bufferToSend==null)
        {
            if (silentBuffer==null)
                buffer.setDiscard(true);
            else
            {
                silencePacketCount.incrementAndGet();
                buffer.copy(silentBuffer);
            }
        }
        else
        {
            silencePacketCount.set(0);
            if (silentBuffer==null)
            {
                silentBuffer = new Buffer();
                silentBuffer.copy(bufferToSend);
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug("AudioStream. Silence buffer added to stream");
            }
            buffer.copy(bufferToSend);
        }
    }

    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    public long getContentLength()
    {
        return LENGTH_UNKNOWN;
    }

    public boolean endOfStream()
    {
        return bufferQueue.size()==0 && dataSource.isDataConcated();
    }

    public Object[] getControls()
    {
        return new Object[0];
    }

    public Object getControl(String controlType)
    {
        return null;
    }

    public void transferData(PushBufferStream stream)
    {
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }

    public Node getTaskNode()
    {
        return owner;
    }

    public String getStatusMessage()
    {
        return String.format(
                "Transfering buffers to rtp session. Action: %s. packetCount: %s; sleepTime: %s"
                , action, packetNumber, sleepTime);
    }

    public void run()
    {
        dataSource.setStreamThreadRunning(true);
        try
        {
            long startTime = System.currentTimeMillis();
            packetNumber = 0;
            while ((!dataSource.isDataConcated() || !bufferQueue.isEmpty())
                    && silencePacketCount.get()<MAX_SILENCE_BUFFER_COUNT)
            {
                try
                {
                    action = "getting new buffer from queue";
                    bufferToSend = bufferQueue.poll();
                    action = "sending transfer event";
                    transferData(null);
                    ++packetNumber;
                    action = "sleeping";
                    long expectedPacketNumber = (System.currentTimeMillis()-startTime)/packetLength;
                    sleepTime = (packetNumber-expectedPacketNumber-(bufferToSend==null? 0 : maxSendAheadPacketsCount))*packetLength;
                    if (sleepTime>0)
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                }
                catch (InterruptedException ex) {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(
                                "Transfer buffers to rtp session task was interrupted");
                    Thread.currentThread().interrupt();
                }
            }
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(
                        "Transfer buffers to rtp session task was finished");
        }
        finally
        {
            dataSource.setStreamThreadRunning(false);
        }
    }
}
