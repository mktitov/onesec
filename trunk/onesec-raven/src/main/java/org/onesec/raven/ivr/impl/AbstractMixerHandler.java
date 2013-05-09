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
import org.onesec.raven.RingQueue;
import org.onesec.raven.impl.RingQueueImpl;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.onesec.raven.ivr.MixerHandler;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractMixerHandler implements MixerHandler {
    private volatile DataHandler dataHandler;
    protected final LoggerHelper logger;
    private final CodecManager codecManager;
    private final AudioFormat format;

    private volatile MixerHandler nextHandler;
    private final AtomicInteger bytesCount = new AtomicInteger(0);

    public AbstractMixerHandler(CodecManager codecManager, PushBufferDataSource datasource, AudioFormat format, 
            LoggerHelper logger) throws CodecManagerException, IOException 
    {
        this.logger = logger;
        this.codecManager = codecManager;
        this.format = format;
        this.dataHandler = new DataHandler(datasource);
        this.dataHandler.init();
    }
        
    protected void replaceDataSource(PushBufferDataSource datasource) throws Exception {
        DataHandler oldHandler = dataHandler;
        if (datasource!=null) {
            DataHandler newHandler = new DataHandler(datasource);
            newHandler.init();
            this.dataHandler = newHandler;
        } else
            this.dataHandler = null;
        if (oldHandler!=null)
            oldHandler.stop();
    }

    public void connect() throws IOException {
        DataHandler _dataHandler = dataHandler;
        if (_dataHandler!=null)
            _dataHandler.datasource.connect();
    }

    public void disconnect() {
        DataHandler _dataHandler = dataHandler;
        if (_dataHandler!=null)
            _dataHandler.datasource.disconnect();
    }

    public void start() throws IOException {
        DataHandler _dataHandler = dataHandler;
        if (_dataHandler!=null)
            _dataHandler.datasource.start();
    }

    public void stop() throws IOException {
        DataHandler _dataHandler = dataHandler;
        if (_dataHandler!=null)
            _dataHandler.datasource.stop();
    }

    public boolean isAlive() {
        DataHandler _dataHandler = dataHandler;
        return _dataHandler!=null && _dataHandler.isAlive();
    }

    public RingQueue getQueue() {
        DataHandler _dataHandler = dataHandler;
        return _dataHandler==null? null : _dataHandler.buffers;
    }

//    public Buffer peek() {
////        return bytesCount.get()>1200? buffers.peek() : null;
//        return buffers.peek();
//    }
//
//    public Buffer pop() {
//        return buffers.pop();
//    }

    public MixerHandler getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(MixerHandler handler) {
        this.nextHandler = handler;
    }

    public PushBufferDataSource getDataSource() {
        DataHandler _dataHandler = dataHandler;
        return _dataHandler==null? null : _dataHandler.datasource;
    }

    public void transferData(PushBufferStream stream) {
    }
    
    private class DataHandler implements BufferTransferHandler {
        private final RingQueueImpl<Buffer> buffers = new RingQueueImpl<Buffer>(32);
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final PushBufferDataSource datasource;

        public DataHandler(PushBufferDataSource datasource) throws CodecManagerException {
            this.datasource = new TranscoderDataSource(codecManager, datasource, format, logger);
        }
        
        public void init() throws IOException {
            datasource.getStreams()[0].setTransferHandler(this);
            datasource.connect();
            datasource.start();
        }
        
        public void stop() {
            datasource.getStreams()[0].setTransferHandler(null);
            datasource.disconnect();
            try {
                datasource.stop();
            } catch (IOException ex) {
                if (logger.isErrorEnabled())
                    logger.error("Data handler stopping error", ex);
            }
        }
        
        public boolean isAlive() {
            return alive.get() || buffers.hasElement();
        }

        public void transferData(PushBufferStream stream) {
            try {
                PushBufferDataSource ds = datasource;
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
}
