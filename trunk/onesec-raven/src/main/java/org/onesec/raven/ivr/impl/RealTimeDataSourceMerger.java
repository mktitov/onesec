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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.impl.RingQueue;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataSourceMerger extends PushBufferDataSource {

    private final CodecManager codecManager;
    private final Node owner;
    private final String logPrefix;
    private final ExecutorService executor;

    private volatile DataSourceHandler firstHandler;
    private volatile DataSourceHandler lastHandler;
    private volatile int handlersCount = 0;
    private volatile boolean stopped = false;

    public RealTimeDataSourceMerger(CodecManager codecManager, Node owner, String logPrefix
            , ExecutorService executor) 
    {
        this.codecManager = codecManager;
        this.owner = owner;
        this.logPrefix = logPrefix;
        this.executor = executor;
    }

    
    public void addDataSource(PushBufferDataSource dataSource) throws CodecManagerException {
        DataSourceHandler handler = new DataSourceHandler(dataSource);
        handler.init();
        synchronized(this) {
            if (firstHandler==null)
                firstHandler=handler;
            if (lastHandler!=null)
                lastHandler.nextHandler = handler;
            lastHandler = handler;
            ++handlersCount;
        }
    }

    @Override
    public PushBufferStream[] getStreams() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void connect() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getControl(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object[] getControls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Time getDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"Merger. "+String.format(mess, args);
    }
    
    private class Stream implements PushBufferStream, Task {
        
        private final static long TICK_INTERVAL = 20;
        private final static int BUFFER_SIZE = (int) (TICK_INTERVAL * 8);

        public Format getFormat() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void read(Buffer buffer) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setTransferHandler(BufferTransferHandler transferHandler) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ContentDescriptor getContentDescriptor() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getContentLength() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean endOfStream() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object[] getControls() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getControl(String controlType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Node getTaskNode() {
            return owner;
        }

        public String getStatusMessage() {
            return "Merging audio streams";
        }

        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                long packetNumber = 0;
                while (!stopped) {
                    //transsmit
                    mergeAndTranssmit();
                    //sleep
                    ++packetNumber;
                    long timeDiff = System.currentTimeMillis() - startTime;
                    long expectedPacketNumber = timeDiff/TICK_INTERVAL;
                    long correction = timeDiff % TICK_INTERVAL;
                    long sleepTime = (packetNumber-expectedPacketNumber) * TICK_INTERVAL - correction;
                    if (sleepTime>0)
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void mergeAndTranssmit() {
            byte[] data = new byte[BUFFER_SIZE];
            DataSourceHandler handler = firstHandler;
            while (handler!=null) {
                Buffer buffer = handler.buffers.peek();
                int len = BUFFER_SIZE;
                int offset = 0;
                while (buffer!=null && len>0) {
                    byte[] bufdata = (byte[]) buffer.getData();
                    int bufOffset = buffer.getOffset();
                    int buflen = buffer.getLength();
                    int bytesToRead = Math.min(len, buflen);
                    for (int i=bufOffset; i<bytesToRead; ++i) 
                        data[offset++] += (byte) (bufdata[i]/handlersCount);
                    if (bytesToRead==buflen)
                        handler.buffers.pop();
                    else 
                        buffer.setOffset(bufOffset+bytesToRead);
                    len-=bytesToRead;
                }
                handler = handler.nextHandler;
            }
        }
    }
    
    private class DataSourceHandler implements BufferTransferHandler {
        private final PushBufferDataSource datasource;
        private final RingQueue<Buffer> buffers = new RingQueue<Buffer>(10);
        
        private volatile DataSourceHandler nextHandler;

        public DataSourceHandler(PushBufferDataSource datasource) throws CodecManagerException {
            this.datasource = new TranscoderDataSource(codecManager, datasource, 
                    Codec.LINEAR.getAudioFormat(), owner, logMess(""));
        }
        
        public void init() {
            datasource.getStreams()[0].setTransferHandler(this);
        }

        public void transferData(PushBufferStream stream) {
            try {
                Buffer buffer = new Buffer();
                stream.read(buffer);
                buffers.push(buffer);
            } catch (IOException e) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("DataSourceHandler. Buffer reading error"), e);
            }
        }
    }
}
