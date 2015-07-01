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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class AsyncPushBufferDataSource extends PushBufferDataSource {
    private final PushBufferDataSource source;
    private final int queueSize;
    private final Node owner;
    private final boolean passEventsToSource;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final ExecutorService executor;
    private final LoggerHelper logger;
    private final PushBufferStream[] streams;

    public AsyncPushBufferDataSource(PushBufferDataSource source, ExecutorService executor, int queueSize, 
            Node owner, LoggerHelper logger, boolean passEventsToSource) 
    {
        this.source = source;
        this.owner = owner;
        this.executor = executor;
        this.queueSize = queueSize;
        this.logger = new LoggerHelper(logger, "Async DS. ");
        this.passEventsToSource = passEventsToSource;
        this.streams = new PushBufferStream[]{new Stream()};
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return source.getContentType();
    }

    @Override
    public void connect() throws IOException {
        if (connected.compareAndSet(false, true)) {
            if (passEventsToSource)
                source.connect();
            ((Stream)streams[0]).connect();
            source.getStreams()[0].setTransferHandler((Stream)streams[0]);
        }
    }

    @Override
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            if (passEventsToSource)
                source.disconnect();
            ((Stream)streams[0]).disconnect();
            source.getStreams()[0].setTransferHandler(null);
        }
    }

    @Override
    public void start() throws IOException {
        if (started.compareAndSet(false, true) && passEventsToSource)
            source.start();
    }

    @Override
    public void stop() throws IOException {
        if (started.compareAndSet(true, false) && passEventsToSource)
            source.stop();
    }

    @Override
    public Object getControl(String name) {
        return source.getControl(name);
    }

    @Override
    public Object[] getControls() {
        return source.getControls();
    }

    @Override
    public Time getDuration() {
        return source.getDuration();
    }
    
    private class Stream implements PushBufferStream, BufferTransferHandler {
        private final ArrayBlockingQueue<Buffer> buffers = new ArrayBlockingQueue<Buffer>(queueSize);
        private final PushBufferStream stream = source.getStreams()[0];
        private final AtomicBoolean eos = new AtomicBoolean();
        private volatile BufferTransferHandler transferHandler;
        private volatile Buffer currentBuffer;
        private final AtomicBoolean runnig = new AtomicBoolean();
        private volatile Worker worker;

        public Format getFormat() {
            return stream.getFormat();
        }
        
        public void connect() {
            disconnect();
            Worker _worker = new Worker();
            executor.executeQuietly(_worker);
            worker = _worker;
        }
        
        public void disconnect() {
            Worker _worker = worker;
            worker = null;
            if (_worker!=null)
                _worker.stop();
        }

        public void read(Buffer buffer) throws IOException {
            Buffer _currentBuffer = currentBuffer;
            if (_currentBuffer==null) 
                buffer.setDiscard(true);
            else {
                buffer.copy(_currentBuffer);
                if (_currentBuffer.isEOM()) {
                    eos.set(true);
                    disconnect();
                }
            }
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
            return eos.get();
        }

        public Object[] getControls() {
            return stream.getControls();
        }

        public Object getControl(String paramString) {
            return stream.getControl(paramString);
        }

        public void transferData(PushBufferStream stream) {
            try {
                final Buffer buffer = new Buffer();
                stream.read(buffer);
                if (!buffers.offer(buffer) && logger.isErrorEnabled())
                    logger.error("Buffer queue is full!!!");
            } catch (IOException e) {
                if (logger.isErrorEnabled())
                    logger.error("Error reading buffer from source", e);
            }
        }

        private class Worker implements Task {
            private final AtomicBoolean stopped = new AtomicBoolean();            
            
            public void stop() {
                stopped.compareAndSet(false, true);
            }

            public Node getTaskNode() {
                return owner;
            }

            public String getStatusMessage() {
                return "Transfering buffers between push buffer data sources";
            }

            public void run() {
                while (!stopped.get()) {
                    try {
                        Buffer buf = buffers.poll(5, TimeUnit.MILLISECONDS);
                        BufferTransferHandler _transferHandler = transferHandler;
                        if (buf!=null && _transferHandler!=null) {
                            currentBuffer = buf;
                            _transferHandler.transferData(Stream.this);
                        }
                    } catch (InterruptedException ex) {
                        if (logger.isWarnEnabled())
                            logger.warn("Worker interrupted", ex);
                    }
                }
            }
        }
    }
}
