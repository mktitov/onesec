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
import org.onesec.raven.ivr.Codec;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataStream implements PushBufferStream, BufferTransferHandler {
    
    public static final int MAX_TIME_SKEW = 150;
    
    private final PushBufferStream stream;
    private long packetLengthInMillis=0;
    private final Logger logger;
    private BufferTransferHandler transferHandler;
    private RealTimeDataSource source;
    private long counter = 0;
    private long startTs = 0;
    private volatile long discardedBuffersCount = 0;

    public RealTimeDataStream(RealTimeDataSource source, PushBufferStream stream) {
        this.stream = stream;
        this.source = source;
        this.logger = source.getLogger();
        this.stream.setTransferHandler(this);
    }

    public long getDiscardedBuffersCount() {
        return discardedBuffersCount;
    }

    public Format getFormat() {
        return stream.getFormat();
    }

    public void read(Buffer buffer) throws IOException {
        stream.read(buffer);
//        if (packetLengthInMillis!=-1) {
//            if (packetLengthInMillis==0) {
//                packetLengthInMillis = Codec.getMillisecondsForFormat(stream.getFormat(), buffer.getLength());
//                if (packetLengthInMillis==0) {
//                    if (logger.isWarnEnabled()) 
//                        logger.warn(source.logMess(
//                                "Can't detect packet size in milliseconds for format (%s)"
//                                , buffer.getFormat()));
//                    packetLengthInMillis=-1;
//                } else if (logger.isDebugEnabled())
//                    logger.debug(source.logMess(
//                            "The incoming stream packet length in millisecods is (%s)", packetLengthInMillis));
//            }
//            if (packetLengthInMillis>0) {
//                if (counter==0) 
//                    buffer.setTimeStamp(startTs=System.currentTimeMillis());
//                else
//                    buffer.setTimeStamp(startTs+counter*packetLengthInMillis);
//                if (buffer.getTimeStamp()+MAX_TIME_SKEW < System.currentTimeMillis()) {
//                    buffer.setDiscard(true);
//                    ++discardedBuffersCount;
//                }
//                counter++;
//            }
//        }
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
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }
}
