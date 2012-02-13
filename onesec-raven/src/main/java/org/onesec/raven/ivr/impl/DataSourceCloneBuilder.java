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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceCloneBuilder implements BufferTransferHandler {
    
    private final PushBufferDataSource source;
    private final Set<DataSourceClone> clones = new HashSet<DataSourceClone>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Node owner;
    private final String logPrefix;
    private Buffer bufferToSend;

    public DataSourceCloneBuilder(PushBufferDataSource source, Node owner, String logPrefix) {
        this.source = source;
        this.owner = owner;
        this.logPrefix = logPrefix;
        this.source.getStreams()[0].setTransferHandler(this);
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Created"));
    }
    
    public PushBufferDataSource createClone() {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Creating new clone"));
        lock.writeLock().lock();
        try {
            DataSourceClone clone = new DataSourceClone();
            clones.add(clone);
            return clone;
        } finally  {
            lock.writeLock().unlock();
        }
    }
    
    private void removeClone(DataSourceClone clone) {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Removing clone"));
        lock.writeLock().lock();
        try {
            clones.remove(clone);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void open() throws IOException {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Initializing"));
        source.connect();
        source.start();
    }
    
    public void close() {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Closing"));
        try {
            try {
                source.stop();
            } finally {
                source.disconnect();
            }
        } catch (Exception e) {
            
        }
    }

    public void transferData(PushBufferStream stream) {
        Buffer buffer = new Buffer();
        try {
            stream.read(buffer);
            lock.readLock().lock();
            try {
                for (DataSourceClone clone: clones) {
                    bufferToSend = new Buffer();
                    bufferToSend.copy(buffer);
                    clone.streams[0].sendData();
                }
            } finally {
                lock.readLock().unlock();
            }
        } catch (IOException ex) {
        }
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"DataSourceCloneBuilder. "+String.format(mess, args);
    }
    
    private class DataSourceClone extends PushBufferDataSource {
        
        private volatile boolean connected = false;
        private final DataStreamClone[] streams = new DataStreamClone[]{new DataStreamClone()};

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
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
            removeClone(this);
        }

        @Override
        public void start() throws IOException {
        }

        @Override
        public void stop() throws IOException {
        }

        @Override
        public Object getControl(String controlType) {
            return source.getControl(controlType);
        }

        @Override
        public Object[] getControls() {
            return source.getControls();
        }

        @Override
        public Time getDuration() {
            return source.getDuration();
        }
    }
    
    private class DataStreamClone implements PushBufferStream {
        
        private volatile BufferTransferHandler transferHandler;

        public Format getFormat() {
            return source.getStreams()[0].getFormat();
        }

        public void read(Buffer buffer) throws IOException {
            if (bufferToSend!=null)
                buffer.copy(bufferToSend);
            else
                buffer.setDiscard(true);
        }
        
        public void sendData(){
            BufferTransferHandler handler = transferHandler;
            if (handler!=null)
                handler.transferData(this);
        }

        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        public ContentDescriptor getContentDescriptor() {
            return source.getStreams()[0].getContentDescriptor();
        }

        public long getContentLength() {
            return source.getStreams()[0].getContentLength();
        }

        public boolean endOfStream() {
            return source.getStreams()[0].endOfStream();
        }

        public Object[] getControls() {
            return source.getStreams()[0].getControls();
        }

        public Object getControl(String controlType) {
            return source.getStreams()[0].getControl(controlType);
        }
    }
}
