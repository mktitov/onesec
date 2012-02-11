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
import javax.media.Demultiplexer;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;

/**
 *
 * @author Mikhail Titov
 */
public class ContainerParserDataSource extends PullBufferDataSource {
    private final DataSource source;
    private final ContainerParserDataStream[] streams;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final CodecManager codecManager;
    private Demultiplexer parser;

    public ContainerParserDataSource(CodecManager codecManager, DataSource source) {
        this.source = source;
        this.streams = new ContainerParserDataStream[]{new ContainerParserDataStream(this)};
        this.codecManager = codecManager;
    }
    
    public ContainerParserDataSource(CodecManager codecManager, InputStreamSource inputStreamSource
            , String contentType)
    {
        this(codecManager, new IssDataSource(inputStreamSource, contentType));
    }

    @Override
    public PullBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public void connect() throws IOException {
        if (!connected.compareAndSet(false, true))
            return;
        source.connect();
        try {
            this.parser = codecManager.buildDemultiplexer(source.getContentType());
            if (parser==null)
                throw new ContainerParserDataSourceException(String.format(
                        "Can't find parser for content type (%s)", source.getContentType()));
            parser.setSource(source);
            streams[0].setTrack(parser.getTracks()[0]);
        } catch (Exception e) {
            throw new IOException(
                    String.format("Error configuring parser (%s)", parser.getClass().getName())
                    , e);
        }
    }

    @Override
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            source.disconnect();
            parser.reset();
        }
    }

    @Override
    public void start() throws IOException {
        source.start();
        parser.start();
    }

    @Override
    public void stop() throws IOException {
        parser.stop();
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
        return parser.getDuration();
    }
}