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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.protocol.*;
import org.onesec.raven.ivr.CodecManager;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AudioFileWriterDataSource {
    private final Node owner;
    private final File file;
    private final PushBufferDataSource dataSource;
    private final String logPrefix;
    private final Multiplexer mux;
    private boolean firstBuffer = true;
    private RandomAccessFile out;
    private final AtomicBoolean fileClosed = new AtomicBoolean(false);
    private final AtomicBoolean muxClosed = new AtomicBoolean(false);

    public AudioFileWriterDataSource(Node owner, File file, PushBufferDataSource dataSource
            , CodecManager codecManager, String contentType, String logPrefix) 
        throws FileWriterDataSourceException 
    {
        this.owner = owner;
        this.file = file;
        this.dataSource = dataSource;
        this.logPrefix = logPrefix;
        mux = codecManager.buildMultiplexer(contentType);
        if (mux==null) 
            throw new FileWriterDataSourceException(String.format(
                    "Not found multiplexer for content type (%s)", contentType));
        mux.setContentDescriptor(new ContentDescriptor(contentType));
        mux.setNumTracks(1);
        this.dataSource.getStreams()[0].setTransferHandler(new DataSourceTransferHandler());
    }
    
    public void start() throws IOException {
        dataSource.connect();
        dataSource.start();
    }
    
    public void stop() {
        closeMux();
        closeFile();
        try {
            dataSource.stop();
        } catch (IOException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Error stopping data source"), ex);
        }
        dataSource.disconnect();
    }
    
    private void createFile() {
        try {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Creating file (%s)", file));
            out = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            out = null;
            dataSource.getStreams()[0].setTransferHandler(null);
        }
    }
    
    private void closeFile() {
        if (!fileClosed.compareAndSet(false, true))
            return;
        if (out==null)
            return;
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Closing file (%s)", file));
        try {
            out.close();
        } catch (IOException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Error while closing file (%s)", file));
        }
    }
    
    private void closeMux() {
        if (!muxClosed.compareAndSet(false, true))
            return;
        try {
            if (mux!=null && mux.getDataOutput()!=null) {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(logMess("Closing mux"));
                mux.close();
                mux.getDataOutput().stop();
                mux.getDataOutput().disconnect();
            }
        } catch (IOException e) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Error while closing mux"), e);
        }
    }
    
    private void processError(Exception error) {
        out = null;
        dataSource.getStreams()[0].setTransferHandler(null);
        if (owner.isLogLevelEnabled(LogLevel.ERROR))
            owner.getLogger().error(logMess("Error writing data to file (%s)", file), error);
        closeMux();
        closeFile();
    }
    
    private String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"Audio file writer. "+String.format(mess, args);
    }

    private void initMux(Buffer buffer) {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Initializing multiplexer"));
        Format fmt = mux.setInputFormat(buffer.getFormat(), 0);
        try {
            if (fmt==null) 
                throw new Exception("Not supported format: "+buffer.getFormat());
            mux.open();
            PushDataSource ds = (PushDataSource) mux.getDataOutput();
            ds.getStreams()[0].setTransferHandler(new MuxTransferHandler());
            ds.connect();
            ds.start();
        } catch (Exception ex) {
            processError(ex);
        }
    }
    
    private class MuxTransferHandler implements SourceTransferHandler, Seekable {
        

        public MuxTransferHandler() throws FileNotFoundException {
            out = new RandomAccessFile(file, "rw");
        }

        public void transferData(PushSourceStream sourceStream) {
            int dataLen = sourceStream.getMinimumTransferSize();
            if (dataLen>0) {
                byte[] data = new byte[dataLen];
                try {
                    int processedBytes = sourceStream.read(data, 0, dataLen);
                    out.write(data, 0, processedBytes);
                } catch (IOException ex) {
                    processError(ex);
                }
            }
            if (sourceStream.endOfStream())
                closeFile();
        }

        public long seek(long pos) {
            try {
                out.seek(pos);
                return pos;
            } catch (IOException ex) {
                processError(ex);
                return -1;
            }
        }

        public long tell() {
            try {
                return out.getFilePointer();
            } catch (IOException ex) {
                processError(ex);
                return -1;
            }
        }

        public boolean isRandomAccess() {
            return true;
        }
    }
    
    private class DataSourceTransferHandler implements BufferTransferHandler {

        public void transferData(PushBufferStream sourceStream) {
            try {
                Buffer buffer = new Buffer();
                sourceStream.read(buffer);
                if (buffer.isDiscard())
                    return;
                if (firstBuffer) {
                    initMux(buffer);
                    createFile();
                    firstBuffer = false;
                }
                while (mux.process(buffer, 0) > 1) ;
                if (buffer.isEOM()) {
                    closeMux();
                    closeFile();
                }
            } catch (IOException ex) {
                processError(ex);
            }
        }
    }
}