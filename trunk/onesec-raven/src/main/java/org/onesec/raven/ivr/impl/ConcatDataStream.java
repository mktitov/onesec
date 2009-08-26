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
    private final Queue<Buffer> bufferQueue;
    private final ConcatDataSource dataSource;
    private final ContentDescriptor contentDescriptor;
    private final Node owner;
    private BufferTransferHandler transferHandler;

    public ConcatDataStream(Queue<Buffer> bufferQueue, ConcatDataSource dataSource, Node owner)
    {
        this.bufferQueue = bufferQueue;
        this.dataSource = dataSource;
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.owner = owner;
    }

    public Format getFormat()
    {
        return dataSource.getFormat();
    }

    public void read(Buffer buffer) throws IOException
    {
        Buffer queueBuf = bufferQueue.poll();
        if (queueBuf==null)
            buffer.setDiscard(true);
        else
            buffer.copy(queueBuf);
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
        return "Transfering buffers to rtp session";
    }

    public void run()
    {
        while (!dataSource.isDataConcated() || !bufferQueue.isEmpty())
        {
            transferData(null);
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error("Transfer buffers to rtp session task was interrupted");
            }
        }
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(
                    "Transfer buffers to rtp session task was interrupted finished");
    }
}
