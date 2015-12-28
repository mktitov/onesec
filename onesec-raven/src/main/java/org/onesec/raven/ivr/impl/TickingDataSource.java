/*
 * Copyright 2015 Mikhail Titov.
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
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class TickingDataSource extends PushBufferDataSource {
    private final PushBufferDataSource source;
    private final LoggerHelper logger;
    private final long tickInterval;
    private final Stream[] streams;
    private volatile TickHelper tickHelper;
    
//    private final RealTimeDataStream[] streams;
//    private final Node owner;
//    private final String logPrefix;

    public TickingDataSource(PushBufferDataSource source, long tickInterval, LoggerHelper logger) {
        this.source = source;
        this.tickInterval = tickInterval;
        this.logger = new LoggerHelper(logger, String.format(" Ticking DS [ms]. ", tickInterval));
        this.streams = new Stream[]{new Stream()};
        this.source.getStreams()[0].setTransferHandler(streams[0]);
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
        tickHelper = new TickHelper(tickInterval);
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
        private volatile Buffer bufferToSend;
        private final ContentDescriptor contentDescriptor;

        public Stream() {
            this.contentDescriptor = new ContentDescriptor(TickingDataSource.this.getContentType());
        }

        @Override
        public Format getFormat() {
            return source.getStreams()[0].getFormat();
        }

        @Override
        public void read(Buffer buffer) throws IOException {
            if (bufferToSend==null) 
                buffer.setDiscard(true);
            else
                buffer.copy(bufferToSend);            
        }

        @Override
        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        @Override
        public ContentDescriptor getContentDescriptor() {
            return contentDescriptor;
        }

        @Override
        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        @Override
        public boolean endOfStream() {
            return source.getStreams()[0].endOfStream();
        }

        @Override
        public Object[] getControls() {
            return new Object[0];
        }

        @Override
        public Object getControl(String string) {
            return null;
        }

        @Override
        public void transferData(PushBufferStream stream) {
            try {
                bufferToSend = new Buffer();
                stream.read(bufferToSend);
                if (transferHandler!=null)
                    transferHandler.transferData(this);
                tickHelper.sleep();
            } catch (Exception ex) {
                if (logger.isErrorEnabled()) {
                    logger.error("Buffer reading error", ex);
                }
            }
        }
        
    }
    
}
