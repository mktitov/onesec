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
import java.util.concurrent.atomic.AtomicReference;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.Codec;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ReplayBuffersDataSource extends PushBufferDataSource {
    private enum Status {INITIALIZED, CONNECTED, STARTED};
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.INITIALIZED);
    private final LoggerHelper logger;
    private final PushBufferStream[] streams;

    public ReplayBuffersDataSource(Buffer[] buffers, int packetSize, ExecutorService executor, Codec codec, 
            Node owner, LoggerHelper logger) 
    {
        this.logger = new LoggerHelper(logger, "Buffers replayer. ");
        this.streams = new PushBufferStream[]{
            new Stream(buffers, owner, executor, packetSize, codec)
        };
    }
    

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }
    
    private boolean makeTransition(Status expected, Status update) {
        Status oldStatus = status.get();
        if (status.compareAndSet(expected, update)) {
            if (logger.isDebugEnabled())
                logger.debug(String.format("State changed from (%s) to (%s)", oldStatus, update));
            return true;
        } else if (logger.isWarnEnabled()) 
            logger.warn(String.format("Invalid transition from state (%s) to state (%s)", status.get(), update));
        return false;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;        
    }

    @Override
    public void connect() throws IOException {
        makeTransition(Status.INITIALIZED, Status.CONNECTED);
    }

    @Override
    public void disconnect() {
        makeTransition(Status.CONNECTED, Status.INITIALIZED);
    }

    @Override
    public void start() throws IOException {
        if (makeTransition(Status.CONNECTED, Status.STARTED)) 
            ((Stream)streams[0]).start();
    }

    @Override
    public void stop() throws IOException {
        if (makeTransition(Status.STARTED, Status.CONNECTED)) 
            ((Stream)streams[0]).stop();
    }

    @Override
    public Object getControl(String paramString) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return null;
    }

    @Override
    public Time getDuration() {
        return DURATION_UNKNOWN;
    }
    
    private class Stream implements PushBufferStream {
        private final Buffer[] buffers;
        private final Node owner;
        private final ExecutorService executor;
        private final int packetSize;
        private final Codec codec;
        
        private volatile BufferTransferHandler transferHandler;
        private final AtomicInteger bufferPos = new AtomicInteger();
        private final AtomicBoolean eos = new AtomicBoolean();
        private final ContentDescriptor contentDescriptor = new ContentDescriptor(getContentType());
        private final AtomicBoolean started = new AtomicBoolean();
        private volatile Worker worker;

        public Stream(Buffer[] buffers, Node owner, ExecutorService executor, int packetSize, Codec codec) {
            this.buffers = buffers;
            this.owner = owner;
            this.executor = executor;
            this.packetSize = packetSize;
            this.codec = codec;
        }
        
        public void start() {
            if (started.compareAndSet(false, true)) {
                worker = new Worker();
                executor.executeQuietly(worker);
            }
        }
        
        public void stop() {
            if (started.compareAndSet(false, true)) {
                worker.stopped.set(true);
                worker = null;
            }
        }

        public Format getFormat() {
            return codec.getAudioFormat();
        }

        public void read(Buffer buffer) throws IOException {
            int pos = bufferPos.get();
            if (pos>=buffers.length) {
                buffer.setDiscard(true);
                eos.compareAndSet(false, true);
            } else
                buffer.copy(buffers[pos]);
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
            return eos.get();
        }

        public Object[] getControls() {
            return null;
        }

        public Object getControl(String paramString) {
            return null;
        }

        private class Worker implements Task {
            private final AtomicBoolean stopped = new AtomicBoolean();

            public Node getTaskNode() {
                return owner;
            }

            public String getStatusMessage() {
                return "Replaying buffers";
            }

            public void run() {
                try {
                    while (!stopped.get() && bufferPos.incrementAndGet() < buffers.length) {
                        BufferTransferHandler handler = transferHandler;
                        if (handler!=null)
                            handler.transferData(Stream.this);
                        Thread.sleep(codec.getMillisecondsForPacketSize(packetSize));
                    }
                } catch (InterruptedException e) {
                    if (logger.isWarnEnabled())
                        logger.warn("Worker thread interrupted");
                }
            }
        }
    }
}
