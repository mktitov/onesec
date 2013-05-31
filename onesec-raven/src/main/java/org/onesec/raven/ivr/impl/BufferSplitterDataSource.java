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
import java.util.ArrayList;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class BufferSplitterDataSource extends PushBufferDataSource {
    
    private final PushBufferDataSource source;
    private final PushBufferStream[] streams;
    private final LoggerHelper logger;
    private final BufferCacheListener bufferCacheListener;
    private final ExecutorService executor;
    private final Node owner;
    public final static AudioFormat FORMAT = new AudioFormat(AudioFormat.LINEAR, 8000d, 8, 1, -1
            , 0, 8, 16000.0, byte[].class);

    public BufferSplitterDataSource(PushBufferDataSource source, int maxBufferSize, 
            CodecManager codecManager, LoggerHelper logger) throws CodecManagerException 
    {
        this(source, maxBufferSize, codecManager, logger, null, null, null);
    }

    public BufferSplitterDataSource(PushBufferDataSource source, int maxBufferSize, 
            CodecManager codecManager, LoggerHelper logger, BufferCacheListener bufferCacheListener,
            ExecutorService executor, Node owner) 
        throws CodecManagerException 
    {
        this.source = new TranscoderDataSource(codecManager, source, FORMAT, logger);
        this.logger = new LoggerHelper(logger, "Buffer Splitter. ");
        this.streams = new PushBufferStream[]{new Stream(this.source.getStreams()[0], maxBufferSize)};
        this.source.getStreams()[0].setTransferHandler((BufferTransferHandler)streams[0]);
        this.bufferCacheListener = bufferCacheListener;
        this.executor = executor;
        this.owner = owner;
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
        source.connect();
    }

    @Override
    public void disconnect() {
        source.disconnect();
    }

    @Override
    public void start() throws IOException {
        source.start();
    }

    @Override
    public void stop() throws IOException {
        source.stop();
    }

    @Override
    public Object getControl(String control) {
        return source.getControl(control);
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
        
        private volatile BufferTransferHandler transferHandler;
        private final PushBufferStream sourceStream;
        private final int maxBufferSize;
        private final ArrayList<Buffer> buffers = bufferCacheListener==null? null : new ArrayList<Buffer>(512);
        private volatile Buffer buffer;

        public Stream(PushBufferStream sourceStream, int bufferSize) {
            this.sourceStream = sourceStream;
            this.maxBufferSize = bufferSize;
        }

        public Format getFormat() {
            return sourceStream.getFormat();
        }

        public void read(Buffer buf) throws IOException {
            buf.copy(buffer);
        }

        public void setTransferHandler(BufferTransferHandler handler) {
            this.transferHandler = handler;
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

        public Object getControl(String control) {
            return sourceStream.getControl(control);
        }

        public void transferData(PushBufferStream stream) {
            try {
                Buffer sourceBuf = new Buffer();
                stream.read(sourceBuf);
                if (sourceBuf.isDiscard())
                    return;
                byte[] data = (byte[]) sourceBuf.getData();
                int len = sourceBuf.getLength();
                int pos = 0;
                while (pos<len) {
                    int clen = Math.min(maxBufferSize, len-pos);
                    byte[] newData = new byte[clen];
                    System.arraycopy(data, pos, newData, 0, clen);
                    buffer = new Buffer();
                    buffer.setData(newData);
                    buffer.setFormat(sourceBuf.getFormat());
                    buffer.setOffset(0);
                    buffer.setLength(clen);
                    pos+=clen;
                    if (bufferCacheListener!=null)
                        buffers.add(buffer);
                    if (pos>=len && sourceBuf.isEOM()) {
                        buffer.setEOM(true);
                        if (bufferCacheListener!=null)
                            sendCachedBuffersToListener();
                    }
                    if (transferHandler!=null)
                        transferHandler.transferData(this);
                    Thread.sleep(clen/8);
                }
            } catch (Throwable e) {
                if (logger.isErrorEnabled()) 
                    logger.error("Transfer data error", e);
            }
        }
        
        private void sendCachedBuffersToListener() {
            executor.executeQuietly(new AbstractTask(owner, "Sending buffers to listener") {
                @Override public void doRun() throws Exception {
                    bufferCacheListener.buffersCached(buffers.toArray(new Buffer[buffers.size()]));
                }
            });
        }
    }
}
