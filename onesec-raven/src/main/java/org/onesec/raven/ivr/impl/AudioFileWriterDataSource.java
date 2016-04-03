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
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.protocol.*;
import org.onesec.raven.ivr.CodecManager;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class AudioFileWriterDataSource {
//    private final Node owner;
    private final LoggerHelper logger;
    private final File file;
    private final PushBufferDataSource[] dataSources;
//    private final String logPrefix;
    private final Multiplexer mux;
    private boolean firstBuffer = true;
    private RandomAccessFile out;
    private final AtomicBoolean fileClosed = new AtomicBoolean(false);
    private final AtomicBoolean muxClosed = new AtomicBoolean(false);
    private final AtomicBoolean muxInitialized = new AtomicBoolean(false);

    public AudioFileWriterDataSource(File file, PushBufferDataSource[] dataSources
            , CodecManager codecManager, String contentType, LoggerHelper logger) 
        throws FileWriterDataSourceException 
    {
        this.logger = new LoggerHelper(logger, "Audio file writer. ");
        this.file = file;
        this.dataSources = dataSources;
        mux = codecManager.buildMultiplexer(contentType);
        if (mux==null) 
            throw new FileWriterDataSourceException(String.format(
                    "Not found multiplexer for content type (%s)", contentType));
        mux.setContentDescriptor(new ContentDescriptor(contentType));
        mux.setNumTracks(dataSources.length);
        for (int i=0; i<dataSources.length; i++)
            this.dataSources[0].getStreams()[0].setTransferHandler(new DataSourceTransferHandler(i));
    }
    
    public void start() throws IOException {
        for (PushBufferDataSource dataSource: dataSources) {
            dataSource.connect();
            dataSource.start();
        }
    }
    
    public void stop() {
        closeMux();
        closeFile();
        try {
            for (PushBufferDataSource dataSource: dataSources)
                try {
                    dataSource.stop();
                } finally {
                    dataSource.disconnect();
                }
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error stopping data source", ex);
        }
    }
    
//    private void createFile() {
//        try {
//            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                owner.getLogger().debug(logMess("Creating file (%s)", file));
//            out = new RandomAccessFile(file, "rw");
//        } catch (FileNotFoundException e) {
//            out = null;
//            dataSource.getStreams()[0].setTransferHandler(null);
//        }
//    }
    
    private void closeFile() {
        if (!fileClosed.compareAndSet(false, true))
            return;
        if (out==null)
            return;
        if (logger.isDebugEnabled())
            logger.debug("Closing file ({})", file);
        try {
            out.close();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error while closing file ({})", file);
        }
    }
    
    private void closeMux() {
        if (!muxClosed.compareAndSet(false, true))
            return;
        try {
            if (mux!=null && mux.getDataOutput()!=null) {
                if (logger.isDebugEnabled())
                    logger.debug("Closing mux");
                if (muxInitialized.get()) 
                    mux.close();
                mux.getDataOutput().stop();
                mux.getDataOutput().disconnect();
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled())
                logger.error("Error while closing mux", e);
        }
    }
    
    private void processError(Exception error) {
        out = null;
        for (PushBufferDataSource dataSource: dataSources)
            dataSource.getStreams()[0].setTransferHandler(null);
        if (logger.isErrorEnabled())
            logger.error("Error writing data to file ({})", file, error);
        closeMux();
        closeFile();
    }
    
    private void initMux(Buffer buffer) {
        if (muxInitialized.compareAndSet(false, true) && !muxClosed.get()) {
            if (logger.isDebugEnabled())
                logger.debug("Initializing multiplexer");
            Format fmt = mux.setInputFormat(buffer.getFormat(), 0);
            try {
                if (fmt==null) 
                    throw new Exception("Not supported format: "+buffer.getFormat());
                mux.open();
//                muxInitialized = true;
                PushDataSource ds = (PushDataSource) mux.getDataOutput();
                ds.getStreams()[0].setTransferHandler(new MuxTransferHandler());
                ds.connect();
                ds.start();
            } catch (Exception ex) {
                processError(ex);
            }
        }
    }
    
    private class MuxTransferHandler implements SourceTransferHandler, Seekable {
        public MuxTransferHandler() throws FileNotFoundException {
            if (logger.isDebugEnabled())
                logger.debug("Creating file ({})", file);
            out = new RandomAccessFile(file, "rw");
        }

        public void transferData(PushSourceStream sourceStream) {
            int dataLen = sourceStream.getMinimumTransferSize();
//            System.out.println("dataLen: "+dataLen);
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
        private final int trackID;

        public DataSourceTransferHandler(int trackID) {
            this.trackID = trackID;
        }

        public void transferData(PushBufferStream sourceStream) {
            try {
                Buffer buffer = new Buffer();
                sourceStream.read(buffer);
                if (buffer.isDiscard())
                    return;
//                System.out.println(">> processing buffer");
                if (firstBuffer) {
                    initMux(buffer);
//                    createFile();
                    firstBuffer = false;
                }
                while (!muxClosed.get() && mux.process(buffer, trackID) > 1) ;
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