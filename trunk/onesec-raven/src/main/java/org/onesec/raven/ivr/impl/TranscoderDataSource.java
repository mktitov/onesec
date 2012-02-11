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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecConfig;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;

/**
 *
 * @author Mikhail Titov
 */
public class TranscoderDataSource extends PushBufferDataSource {
    
    private final PushBufferDataSource source;
    private final Format outputFormat;
    private final TranscoderDataStream[] streams;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final CodecManager codecManager;
    private final PushBufferStream sourceStream;

    public TranscoderDataSource(CodecManager codecManager, PushBufferDataSource source, Format outputFormat) 
            throws CodecManagerException 
    {
        this.source = source;
        this.outputFormat = outputFormat;
        this.codecManager = codecManager;
        this.sourceStream = source.getStreams()[0];
        this.streams = new TranscoderDataStream[]{new TranscoderDataStream(sourceStream)};
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
                System.out.println(" !! Input format: "+sourceStream.getFormat());
                CodecConfig[] codecChain = codecManager.buildCodecChain(sourceStream.getFormat(), outputFormat);
                for (CodecConfig codec: codecChain) {
                    System.out.println(" !! Codec: "+codec.getCodec());
                    System.out.println("       IN: "+codec.getInputFormat());
                    System.out.println(" !!   OUT: "+codec.getOutputFormat());
                }
                streams[0].init(codecChain, outputFormat);
            } catch (CodecManagerException ex) {
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
    public Object getControl(String arg0) {
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
}
