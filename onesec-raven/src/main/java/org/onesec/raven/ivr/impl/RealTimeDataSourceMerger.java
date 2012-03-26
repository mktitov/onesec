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
import java.util.List;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataSourceMerger extends PushBufferDataSource {

    private final CodecManager codecManager;
    private final Node owner;
    private final String logPrefix;
    
    private final List<DataSourceHandler> datasources = new ArrayList<DataSourceHandler>(5);

    public RealTimeDataSourceMerger(CodecManager codecManager, Node owner, String logPrefix) {
        this.codecManager = codecManager;
        this.owner = owner;
        this.logPrefix = logPrefix;
    }
    
    public void addDataSource(PushBufferDataSource dataSource) throws CodecManagerException {
        DataSourceHandler handler = new DataSourceHandler(dataSource);
        handler.init();
        datasources.add(handler);
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
    
    private class Stream implements PushBufferStream {

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
        
    }
    
    private class DataSourceHandler implements BufferTransferHandler {
        private final static int BUFFER_SIZE = 160;
        private final PushBufferDataSource datasource;

        public DataSourceHandler(PushBufferDataSource datasource) throws CodecManagerException {
            this.datasource = new TranscoderDataSource(codecManager, datasource, 
                    Codec.LINEAR.getAudioFormat(), owner, logMess(""));
        }
        
        public void init() {
            datasource.getStreams()[0].setTransferHandler(this);
        }

        public void transferData(PushBufferStream stream) {
            Buffer buffer = new Buffer();
            buffer.setData(new byte[BUFFER_SIZE]);
            buffer.setLength(BUFFER_SIZE);
        }
    }
}
