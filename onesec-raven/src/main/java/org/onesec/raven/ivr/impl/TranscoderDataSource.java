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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecConfig;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class TranscoderDataSource extends PushBufferDataSource {
    
    private final PushBufferDataSource source;
    private final AudioFormat outputFormat;
    private final TranscoderDataStream[] streams;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final CodecManager codecManager;
    private final PushBufferStream sourceStream;
//    private final Node owner;
//    private final String logPrefix;
    private final Logger logger;

    public TranscoderDataSource(CodecManager codecManager, PushBufferDataSource source
            , AudioFormat outputFormat, LoggerHelper logger) 
            throws CodecManagerException 
    {
//        this.logger = new LoggerHelper(owner, (logPrefix==null?"":logPrefix)+"Transcoder. ");
        this.logger = new LoggerHelper(logger, "Transcoder. ");
//        this.owner = owner;
//        this.logPrefix = logPrefix;
        this.source = source;
        this.outputFormat = outputFormat;
        this.codecManager = codecManager;
        this.sourceStream = source.getStreams()[0];
        this.streams = new TranscoderDataStream[]{new TranscoderDataStream(sourceStream, logger)};
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
        if (connected.compareAndSet(false, true)) {
            source.connect();
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Initializing ");
                    logger.debug("Input format: "+sourceStream.getFormat());
                    logger.debug("Output format: "+outputFormat);
                    logger.debug("Codec chain");
                }
                CodecConfig[] codecChain = codecManager.buildCodecChain(
                        (AudioFormat)sourceStream.getFormat(), outputFormat);
                if (logger.isDebugEnabled()) {
                    for (int i=0; i<codecChain.length; i++) {
                        logger.debug("[{}] Codec: {}", i, codecChain[i].getCodec());
                        logger.debug("[{}] in : {}", i, codecChain[i].getInputFormat());
                        logger.debug("[{}] out: {}", i, codecChain[i].getOutputFormat());
                    }
                }
                streams[0].init(codecChain, outputFormat);
            } catch (CodecManagerException ex) {
                if (logger.isErrorEnabled())
                    logger.error("Transcoder initializing error", ex);
                throw new IOException(ex);
            }
        }
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
    public Object getControl(String controlName) {
        return streams[0].getControl(controlName);
    }

    @Override
    public Object[] getControls() {
        return streams[0].getControls();
    }

    @Override
    public Time getDuration() {
        return DURATION_UNKNOWN;
    }
    
//    String logMess(String mess, Object... args) {
//        return (logPrefix==null? "" : logPrefix)+"Transcoder. "+String.format(mess, args);
//    }
}
