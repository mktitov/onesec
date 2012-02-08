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
import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferStream;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PullToPushConverterDataStream implements PushBufferStream, Task {
    
    private final Node owner;
    private final PullBufferStream sourceStream;
    private volatile BufferTransferHandler transferHandler;
    private volatile Buffer bufferToSend;
    private volatile boolean stop = false;

    public PullToPushConverterDataStream(Node owner, PullBufferStream sourceStream) {
        this.owner = owner;
        this.sourceStream = sourceStream;
    }

    public Format getFormat() {
        return sourceStream.getFormat();
    }

    public void read(Buffer buffer) throws IOException {
        buffer.copy(bufferToSend);
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return sourceStream.getContentDescriptor();
    }

    public long getContentLength() {
        return sourceStream.getContentLength();
    }

    public boolean endOfStream() {
        return sourceStream.endOfStream();
    }

    public Object[] getControls() {
        return sourceStream.getControls();
    }

    public Object getControl(String controlType) {
        return sourceStream.getControl(controlType);
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return "Converting PullDataSource to PushDataSource";
    }
    
    public void stop() {
        stop = true;
    }

    public void run() {
        try {
            while (!stop && !sourceStream.endOfStream()) {
                bufferToSend = new Buffer();
                sourceStream.read(bufferToSend);
                if (!bufferToSend.isDiscard()) {
                    BufferTransferHandler _handler = transferHandler;
                    if (_handler!=null)
                        _handler.transferData(this);
                }
            }
        } catch (Throwable e) {
            if (owner.getLogger().isErrorEnabled())
                owner.getLogger().error("Error converting PullBufferDataSource to PushBufferDataSource", e);
        }
    }
}
