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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.BadHeaderException;
import javax.media.Demultiplexer;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import org.onesec.raven.ivr.CodecManager;

/**
 *
 * @author Mikhail Titov
 */
public class ContainerParserDataSource extends PullBufferDataSource {
    private final DataSource source;
    private final Demultiplexer parser;
    private final ContainerParserDataStream[] streams;

    public ContainerParserDataSource(CodecManager codecManager, DataSource source) 
            throws ContainerParserDataSourceException, IOException 
    {
        this.source = source;
        this.parser = codecManager.buildDemultiplexer(source.getContentType());
        if (parser==null)
            throw new ContainerParserDataSourceException(String.format(
                    "Can't find parser for content type (%s)", source.getContentType()));
        try {
            parser.setSource(source);
            streams = new ContainerParserDataStream[]{new ContainerParserDataStream(parser.getTracks()[0], this)};
        } catch (Exception e) {
            throw new ContainerParserDataSourceException(
                    String.format("Error configuring parser (%s)", parser.getClass().getName())
                    , e);
        }
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
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void start() throws IOException {
        parser.start();
//        try {
//            streams[0].setTrack(parser.getTracks()[0]);
//        } catch (BadHeaderException ex) {
//            throw new IOException(ex);
//        }
    }

    @Override
    public void stop() throws IOException {
        parser.stop();
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