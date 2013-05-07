/*
 * Copyright 2013 Mikhail Titov.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.media.Buffer;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.impl.RingQueue;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.onesec.raven.ivr.MixerHandler;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractMixerHandler implements MixerHandler, BufferTransferHandler {
    private final PushBufferDataSource datasource;
    private final RingQueue<Buffer> buffers = new RingQueue<Buffer>(32);
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final LoggerHelper logger;

    private volatile MixerHandler nextHandler;
    private final AtomicInteger bytesCount = new AtomicInteger(0);

    public AbstractMixerHandler(CodecManager codecManager, PushBufferDataSource datasource, AudioFormat format, 
            LoggerHelper logger) throws CodecManagerException 
    {
        this.logger = logger;
        this.datasource = new TranscoderDataSource(codecManager, datasource, format, logger);
    }
    
    public void init() throws IOException {
        datasource.getStreams()[0].setTransferHandler(this);
        datasource.connect();
        datasource.start();
    }

    public boolean isAlive() {
        return alive.get() || buffers.hasElement();
    }

    public Buffer peek() {
//        return bytesCount.get()>1200? buffers.peek() : null;
        return buffers.peek();
    }

    public Buffer pop() {
        return buffers.pop();
    }

    public MixerHandler getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(MixerHandler handler) {
        this.nextHandler = handler;
    }

    public PushBufferDataSource getDataSource() {
        return datasource;
    }

    public void transferData(PushBufferStream stream) {
        try {
            Buffer buffer = new Buffer();
            stream.read(buffer);
            byte[] data = (byte[]) buffer.getData();
            if (data!=null)
                bytesCount.addAndGet(data.length);
            buffers.push(buffer);
            if (buffer.isEOM())
                alive.compareAndSet(true, false);
        } catch (IOException e) {
            if (logger.isErrorEnabled())
                logger.error("DataSourceHandler. Buffer reading error", e);
        }
    }
}
