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
import static java.lang.Math.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.RingQueue;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.MixerHandler;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractRealTimeMixer<H extends MixerHandler<H>> extends PushBufferDataSource {
    public final static long TICK_INTERVAL = 20;
    public final static int BUFFER_SIZE = (int) (TICK_INTERVAL * 8);
    public final static ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor(ContentDescriptor.RAW);
    public final static AudioFormat FORMAT = new AudioFormat(AudioFormat.LINEAR, 8000d, 8, 1, -1
            , 0, 8, 16000.0, byte[].class);
    
    protected final LoggerHelper logger;
    protected final CodecManager codecManager;
    protected final Node owner;
    protected final ExecutorService executor;
    protected final int noiseLevel;
    protected final double maxGainCoef;

    private volatile H firstHandler;
    private volatile H lastHandler;
    private volatile int handlersCount = 0;
    private volatile boolean stopped = false;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final PushBufferStream[] streams = new PushBufferStream[]{new Stream()};
    
    public AbstractRealTimeMixer(CodecManager codecManager, Node owner, LoggerHelper logger
            , ExecutorService executor, int noiseLevel, double maxGainCoef) 
    {
        this.codecManager = codecManager;
        this.owner = owner;
        this.executor = executor;
        this.noiseLevel = noiseLevel;
        this.maxGainCoef = maxGainCoef;
        this.logger = logger;
    }
    
    protected void addDataSourceHandler(H handler) throws IOException {
//        handler.init();
        synchronized(this) {
            if (firstHandler==null)
                firstHandler=handler;
            if (lastHandler!=null)
                lastHandler.setNextHandler(handler);
            lastHandler = handler;
            ++handlersCount;
        }
    }
    
    protected abstract void applyBufferToHandlers(H firstHandler, int[] data, 
            int len, int streamsCount, double maxGainCoef, int bufferSize);
    
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
                MixerHandler handler = firstHandler;
                while (handler!=null) {
                    handler.connect();
                    handler = handler.getNextHandler();
                }
                executor.execute((Task)streams[0]);
            } catch (ExecutorServiceException ex) {
                if (logger.isErrorEnabled())
                    logger.error("Error executing STREAM task", ex);
                throw new IOException(ex);
            }
    }

    @Override public void disconnect() {
        if (!connected.get())
            return;
        stopped = true;
        MixerHandler handler = firstHandler;
        while (handler!=null) {
            handler.disconnect();
            handler = handler.getNextHandler();
        }
        connected.set(false);
    }

    @Override public void start() throws IOException {
        if (!started.compareAndSet(false, true))
            return;
        MixerHandler handler = firstHandler;
        while (handler!=null) {
            handler.start();
            handler = handler.getNextHandler();
        }
    }

    @Override
    public void stop() throws IOException {
        if (!started.compareAndSet(true, false))
            return;
        MixerHandler handler = firstHandler;
        while (handler!=null) {
            handler.stop();
            handler = handler.getNextHandler();
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
    public static Buffer createBuffer(Buffer buf, int[] data, byte[] byteData, int len, int streamsCount, 
            double maxGainCoef, int bufferSize) 
    {
        if (streamsCount==0) Arrays.fill(byteData, (byte)127);
        else  {
            double koef = maxGainCoef;
            if (koef>1.01) {
                int max=0;
                for (int i=0; i<len; i++) {
                    data[i] = data[i]/streamsCount;
                    max = max(max, abs(data[i]));
                }
                koef = min(max>0? 127./max : 1., maxGainCoef);
            }
            for (int i=0; i<len; ++i) {
                if (koef>1.01) 
                    data[i] = (int) (koef*data[i]);
                byteData[i] = (byte) (data[i]+127);
            }
        }
//        Buffer buf = new Buffer();
        buf.setFormat(FORMAT);
        buf.setData(byteData);
        buf.setOffset(0);
        buf.setLength(len==0? bufferSize : len);
        return buf;
    }
    
    private class Stream implements PushBufferStream, Task {
        private final int[] data = new int[BUFFER_SIZE];
        private final int[] workData = new int[BUFFER_SIZE];
        private final byte[] byteData = new byte[BUFFER_SIZE];
        
        private volatile Buffer bufferToTranssmit;
        private volatile BufferTransferHandler transferHandler;

        public Format getFormat() {
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
                if (logger.isDebugEnabled())
                    logger.debug("Merger task executed");
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
            Arrays.fill(data, 0);
            MixerHandler<H> handler = firstHandler;
            MixerHandler<H> prevHandler = null;
            int maxlen = 0; int streamsCount = 0;
            while (handler!=null) {
                int processedBytes = processHandlerBuffer(handler);
                if (processedBytes>0) {
                    maxlen = max(maxlen, processedBytes);
                    ++streamsCount;
                }
                if (handler.getNextHandler()!=null && !handler.isAlive()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Data source handler removed");
                    if (prevHandler==null) firstHandler = handler.getNextHandler();
                    else prevHandler.setNextHandler(handler.getNextHandler());
                } else
                    prevHandler = handler;
                handler = handler.getNextHandler();
            }
            applyBufferToHandlers(firstHandler, data, maxlen, streamsCount, maxGainCoef, BUFFER_SIZE);
            bufferToTranssmit = createBuffer(new Buffer(), data, byteData, maxlen, streamsCount, maxGainCoef, 
                    BUFFER_SIZE);
            BufferTransferHandler _transferHandler = transferHandler;
            if (_transferHandler!=null)
                _transferHandler.transferData(this);
        }
                       
        private int processHandlerBuffer(MixerHandler handler) {
            RingQueue<Buffer> bufferQueue = handler.getQueue();
            if (bufferQueue==null)
                return 0;
            Arrays.fill(workData, 0);
            Buffer buffer = bufferQueue.peek();
            int len = BUFFER_SIZE; int offset = 0; int max=0;
            while (buffer!=null && len>0) {
                int buflen = buffer.getLength();
                if (buffer.isDiscard()) {
                    bufferQueue.pop();
                    buffer = bufferQueue.peek();
                } else {
                    byte[] bufdata = (byte[]) buffer.getData();
                    int bufOffset = buffer.getOffset();
                    int bytesToRead = min(len, buflen);
                    for (int i=bufOffset; i<bufOffset+bytesToRead; ++i) {
                        int val = (bufdata[i] & 0x00FF) - 127;
                        max = max(max, abs(val));
                        workData[offset++] = val;
                    }
                    if (bytesToRead==buflen || buffer.isEOM()) {
                        bufferQueue.pop();
                        buffer = bufferQueue.peek();
                    } else {
                        buffer.setOffset(bufOffset+bytesToRead);
                        buffer.setLength(buflen-bytesToRead);
                    }
                    len-=bytesToRead;
                }
            }
            if (max>noiseLevel) {
                handler.applyProcessingBuffer(workData);
                for (int i=0; i<offset; ++i)
                    data[i]+=workData[i];
                return offset;
            } else {
                handler.applyProcessingBuffer(null);
                return 0;
            }
        }
    }
}
