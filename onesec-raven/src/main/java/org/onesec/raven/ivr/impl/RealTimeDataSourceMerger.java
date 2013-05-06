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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.impl.RingQueue;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
    public class RealTimeDataSourceMerger extends PushBufferDataSource {
    private final static ContentDescriptor CONTENT_DESCRIPTOR = 
            new ContentDescriptor(ContentDescriptor.RAW);
    private final static AudioFormat FORMAT = new AudioFormat(AudioFormat.LINEAR, 8000d, 8, 1, -1
            , 0, 8, 16000.0, byte[].class);
    
    private final CodecManager codecManager;
    private final Node owner;
    private final String logPrefix;
    private final ExecutorService executor;
    private final int noiseLevel;
    private final double maxGainCoef;

    private volatile DataSourceHandler firstHandler;
    private volatile DataSourceHandler lastHandler;
    private volatile int handlersCount = 0;
    private volatile boolean stopped = false;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final PushBufferStream[] streams = new PushBufferStream[]{new Stream()};
    
    public RealTimeDataSourceMerger(CodecManager codecManager, Node owner, String logPrefix
            , ExecutorService executor, int noiseLevel, double maxGainCoef) 
    {
        this.codecManager = codecManager;
        this.owner = owner;
        this.logPrefix = logPrefix;
        this.executor = executor;
        this.noiseLevel = noiseLevel;
        this.maxGainCoef = maxGainCoef;
    }

    
    public void addDataSource(PushBufferDataSource dataSource) throws CodecManagerException, IOException {
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
        return streams;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public void connect() throws IOException {
        if (connected.compareAndSet(false, true))
            try {
                DataSourceHandler handler = firstHandler;
                while (handler!=null) {
                    handler.datasource.connect();
                    handler = handler.nextHandler;
                }
                executor.execute((Task)streams[0]);
            } catch (ExecutorServiceException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error executing STREAM task"), ex);
                throw new IOException(ex);
            }
    }

    @Override public void disconnect() {
        if (!connected.get())
            return;
        stopped = true;
        DataSourceHandler handler = firstHandler;
        while (handler!=null) {
            handler.datasource.disconnect();
            handler = handler.nextHandler;
        }
        connected.set(false);
    }

    @Override public void start() throws IOException {
        if (!started.compareAndSet(false, true))
            return;
        DataSourceHandler handler = firstHandler;
        while (handler!=null) {
            handler.datasource.start();
            handler = handler.nextHandler;
        }
    }

    @Override
    public void stop() throws IOException {
        if (!started.compareAndSet(true, false))
            return;
        DataSourceHandler handler = firstHandler;
        while (handler!=null) {
            handler.datasource.stop();
            handler = handler.nextHandler;
        }
    }

    @Override public Object getControl(String arg0) {
        return null;
    }

    @Override public Object[] getControls() {
        return null;
    }

    @Override public Time getDuration() {
        return DURATION_UNKNOWN;
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"Merger. "+String.format(mess, args);
    }
    
    private class Stream implements PushBufferStream, Task {
        
        private final static long TICK_INTERVAL = 20;
        private final static int BUFFER_SIZE = (int) (TICK_INTERVAL * 8);
        private final int[] data = new int[BUFFER_SIZE];
        private final int[] workData = new int[BUFFER_SIZE];
        private final byte[] byteData = new byte[BUFFER_SIZE];
        
        private volatile Buffer bufferToTranssmit;
        private volatile BufferTransferHandler transferHandler;

        public Format getFormat() {
//            return Codec.LINEAR.getAudioFormat();
            return FORMAT;
        }

        public void read(Buffer buffer) throws IOException {
            Buffer buf = bufferToTranssmit;
            if (buf!=null)
                buffer.copy(buf);
        }

        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        public ContentDescriptor getContentDescriptor() {
            return CONTENT_DESCRIPTOR;
        }

        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        public boolean endOfStream() {
            return false;
        }

        public Object[] getControls() {
            return null;
        }

        public Object getControl(String controlType) {
            return null;
        }

        public Node getTaskNode() {
            return owner;
        }

        public String getStatusMessage() {
            return "Merging audio streams";
        }

        public void run() {
            try {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(logMess("Merger task executed"));
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
//                    System.out.println(">>> SLEEP TIME: "+sleepTime);
                    if (sleepTime>0)
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void mergeAndTranssmit() {
            Arrays.fill(data, 0);
            DataSourceHandler handler = firstHandler;
            DataSourceHandler prevHandler = null;
            int decHandlersBy = 0; int maxlen = 0; int buffersCount = 0;
            while (handler!=null && handlersCount>0) {
                Arrays.fill(workData, 0);
                Buffer buffer = handler.peek();
                int len = BUFFER_SIZE; int offset = 0; int max=0;
                while (buffer!=null && len>0) {
                    int buflen = buffer.getLength();
                    if (buffer.isDiscard()) {
                        handler.pop();
                        buffer = handler.peek();
                    } else {
                        byte[] bufdata = (byte[]) buffer.getData();
                        int bufOffset = buffer.getOffset();
                        int bytesToRead = Math.min(len, buflen);
                        for (int i=bufOffset; i<bufOffset+bytesToRead; ++i) {
                            int val = bufdata[i] & 0x00FF;
                            int diff = Math.abs(val-127);
                            if (diff>max) 
                                max = diff;
                            workData[offset++] = (bufdata[i] & 0x00FF);
                        }
                        if (bytesToRead==buflen || buffer.isEOM()) {
                            handler.pop();
                            if (buffer.isEOM()) 
                                decHandlersBy++;
                            buffer = handler.peek();
                        } else {
                            buffer.setOffset(bufOffset+bytesToRead);
                            buffer.setLength(buflen-bytesToRead);
                        }
                        len-=bytesToRead;
                    }
                }
                if (max>=noiseLevel) {
                    ++buffersCount;
                    for (int i=0; i<offset; ++i)
                        data[i]+=workData[i];
                    if (offset>maxlen)
                        maxlen=offset;
                }
//                if (!handler.isAlive() && handler.nextHandler!=null) {
//                    if (prevHandler==null) firstHandler = handler.nextHandler;
//                    else prevHandler.nextHandler = handler.nextHandler;
//                } else
//                    prevHandler = handler;
                handler = handler.nextHandler;
            }
            swapBuffers(maxlen, buffersCount);
            if (decHandlersBy>0)
                handlersCount -= decHandlersBy;
            Buffer buf = new Buffer();
            buf.setFormat(FORMAT);
            buf.setData(byteData);
            buf.setOffset(0);
            buf.setLength(maxlen==0? BUFFER_SIZE : maxlen);
            bufferToTranssmit = buf;
            BufferTransferHandler _transferHandler = transferHandler;
            if (_transferHandler!=null)
                _transferHandler.transferData(this);
        }
        
        private void swapBuffers(int len, int buffersCount) {
            if (buffersCount==0)
                Arrays.fill(byteData, (byte)127);
            else  {
                int min=255; int max=0;
                for (int i=0; i<len; i++) {
                    int val = (int) (data[i]/buffersCount);
                    data[i] = val;
                    if (min>val) min = val;
                    if (max<val) max = val;
                }
                max = Math.max(max, 255-min);
                double koef = max>0? 255./max : 1.;
                koef = Math.min(koef, maxGainCoef);
                for (int i=0; i<len; ++i) {
                    if (koef>1.01) 
                        data[i] = (int) (koef*(data[i]-127)+127);
                    byteData[i] = (byte) data[i];
                }
            }
        }
    }
    
    private class DataSourceHandler implements BufferTransferHandler {
        private final PushBufferDataSource datasource;
        private final RingQueue<Buffer> buffers = new RingQueue<Buffer>(20);
        private final AtomicBoolean alive = new AtomicBoolean(true);
        
        private volatile DataSourceHandler nextHandler;
        private final AtomicInteger bytesCount = new AtomicInteger(0);

        public DataSourceHandler(PushBufferDataSource datasource) throws CodecManagerException {
            this.datasource = new TranscoderDataSource(codecManager, datasource, 
                    FORMAT, owner, logMess(""));
        }
        
        public void init() throws IOException {
            datasource.getStreams()[0].setTransferHandler(this);
            if (connected.get())
                datasource.connect();
            if (started.get())
                datasource.start();
        }
        
        public Buffer peek() {
            return bytesCount.get()>1200? buffers.peek() : null;
        }
        
        public Buffer pop() {
            return buffers.pop();
        }
        
        public boolean isAlive() {
            return alive.get();
        }

        public void transferData(PushBufferStream stream) {
            try {
                Buffer buffer = new Buffer();
                stream.read(buffer);
                byte[] data = (byte[]) buffer.getData();
                if (data!=null)
                    bytesCount.addAndGet(data.length);
                buffers.push(buffer);
                if (buffer.isEOM() || stream.endOfStream())
                    alive.compareAndSet(true, false);
            } catch (IOException e) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("DataSourceHandler. Buffer reading error"), e);
            }
        }
    }
}
