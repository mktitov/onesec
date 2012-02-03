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
import javax.media.protocol.PushBufferStream;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataStream implements PushBufferStream, BufferTransferHandler {
    
    private final PushBufferStream stream;
    private BufferTransferHandler transferHandler;

    public RealTimeDataStream(PushBufferStream stream) {
        this.stream = stream;
        this.stream.setTransferHandler(this);
    }

    public Format getFormat() {
        return stream.getFormat();
    }

    public void read(Buffer buffer) throws IOException {
        stream.read(buffer);
        System.out.println("SEQ# "+buffer.getSequenceNumber());
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return stream.getContentDescriptor();
    }

    public long getContentLength() {
        return stream.getContentLength();
    }

    public boolean endOfStream() {
        return stream.endOfStream();
    }

    public Object[] getControls() {
        return stream.getControls();
    }

    public Object getControl(String controlType) {
        return stream.getControl(controlType);
    }

    public void transferData(PushBufferStream stream) {
        transferHandler.transferData(this);
    }
}
