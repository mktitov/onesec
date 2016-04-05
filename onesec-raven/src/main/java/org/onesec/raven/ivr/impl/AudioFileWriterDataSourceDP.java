/*
 * Copyright 2016 Mikhail Titov.
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
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.Seekable;
import javax.media.protocol.SourceTransferHandler;
import org.onesec.raven.ivr.CodecManager;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;

/**
 *
 * @author Mikhail Titov
 */
public class AudioFileWriterDataSourceDP extends AbstractDataProcessorLogic {
    public final static String ERROR_MESSAGE = "ERROR";
    public final static String START_MESSAGE = "START"; //??
    public final static String STOP_MESSAGE = "STOP"; //??
    public final static String INITIALIZED_MESSAGE = "INITIALIZED";
    public final static String STARTED_MESSAGE = "STARTED";
    public final static String STOPPED_MESSAGE = "STOPPED";
    //internal MESSAGES
    private final static String WRITE_TO_FILE_ERROR = "WRITE_TO_FILE_ERROR";
    
    private File file;
    private PushBufferDataSource[] dataSources;
    private Multiplexer mux;
    private boolean initialized;
    private boolean muxInitialized;
    private InboundTransferHandler[] inboundTransferHanders;
    private RandomAccessFile out;
            
    private final Behaviour INIT_STAGE = new Behaviour("INIT_STAGE") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof Init) {
                final Init params = (Init)message;
                file = params.file;
                //checking dataSources count
                dataSources = params.dataSources;
                if (dataSources==null || dataSources.length<1) {
                    getLogger().error("Invalid number of dataSources. Must not be null or zero count");
                    getFacade().stop();
                    return ERROR_MESSAGE;
                }
//                //checking dataSources format
//                Format format = dataSources[0].getStreams()[0].getFormat();
//                for (int i=1; i<dataSources.length; ++i)
//                    if (!format.matches(dataSources[i].getStreams()[0].getFormat())) {
//                        getLogger().error("Audio formats not matches to one another in passed dataSources");
//                        getFacade().stop();
//                        return ERROR_MESSAGE;
//                    }
//                //creating mux
                mux = params.codecManager.buildMultiplexer(params.contentType);
                if (mux==null) {
                    getLogger().error(String.format("Not found multiplexer for content type (%s)", params.contentType));
                    getFacade().stop();
                    return ERROR_MESSAGE;
                }
//                for (int i=0; i<dataSources.length; ++i)
//                    if (mux.setInputFormat(format, i)==null) {
//                        getFacade().stop();
//                        getLogger().error("Invalid mux format: "+format);
//                        return ERROR_MESSAGE;
//                    }
                mux.setContentDescriptor(new ContentDescriptor(params.contentType));
                mux.setNumTracks(dataSources.length);
                become(START_STAGE);
                initialized = true;
                return INITIALIZED_MESSAGE;
            } else {
                return UNHANDLED;
            }
        }
    };
    
    private final Behaviour START_STAGE = new Behaviour("START_STAGE") {
        @Override public Object processData(Object message) throws Exception {
            if (message==START_MESSAGE) {
                try {
                    //initializing
                    //creating mux
                    
                    //starting dataSources
                    inboundTransferHanders = new InboundTransferHandler[dataSources.length];
                    DataProcessorFacade bufferProcessor = getContext().addChild(
                            getContext().createChild("Buffer processor", new BufferProcessor(mux)));
                    for (int i=0; i<dataSources.length; i++) {
                        dataSources[i].connect();
                    }
                    //checking dataSources format
                    Format format = dataSources[0].getStreams()[0].getFormat();
                    for (int i=1; i<dataSources.length; ++i)
                        if (!format.matches(dataSources[i].getStreams()[0].getFormat())) {
                            getLogger().error("Audio formats not matches to one another in passed dataSources");
                            getFacade().stop();
                            return ERROR_MESSAGE;
                        }
                    //setting mux input format
                    for (int i=0; i<dataSources.length; ++i)
                        if (mux.setInputFormat(format, i)==null) {
                            getFacade().stop();
                            getLogger().error("Invalid mux format: "+format);
                            return ERROR_MESSAGE;
                        }
                    
                    //starting mux
                    out = new RandomAccessFile(file, "rw");
                    mux.open();
                    muxInitialized = true;
                    PushDataSource ds = (PushDataSource) mux.getDataOutput();
                    ds.getStreams()[0].setTransferHandler(new MuxTransferHandler());
                    ds.connect();
                    ds.start();
                    for (int i=0; i<dataSources.length; i++) {
                        InboundTransferHandler handler = new InboundTransferHandler(i, bufferProcessor);
                        inboundTransferHanders[i]=handler;
                        dataSources[i].getStreams()[0].setTransferHandler(handler);
                        dataSources[i].start();
                    }
                    
                    become(RUN_STAGE);
                    return STARTED_MESSAGE;
                } catch (Exception ex) {
                    if (getLogger().isErrorEnabled())
                        getLogger().error("Error start writing to file", ex);
                    getFacade().stop();
                    return ERROR_MESSAGE;
                }
            } else
                return UNHANDLED;
        }        
    };
    
    private final Behaviour RUN_STAGE = new Behaviour("RUN_STAGE") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof Buffer) {
                return VOID;
            } else if (message instanceof CloseTrack) {
                closeChannel(((CloseTrack)message).trackID);
                if (!hasActiveChannel())
                    getFacade().send(STOP_MESSAGE);
                return VOID;
            } else if (message==STOP_MESSAGE || message==WRITE_TO_FILE_ERROR) {
//                processClose();
                getFacade().stop();
                return STOPPED_MESSAGE;
            } else
                return UNHANDLED;
        }
    };

    @Override
    public void postInit() {
        getContext().become(INIT_STAGE, true);
    }

    @Override
    public void postStop() {
        if (initialized)
            processClose();
    }

    @Override
    public void childTerminated(DataProcessorFacade child) {
    }
    
    @Override
    public Object processData(Object dataPackage) throws Exception {
        return null;
    }
    
    private void closeChannel(int trackId) {
        if (inboundTransferHanders[trackId] != null) {
            dataSources[trackId].getStreams()[0].setTransferHandler(null);
            inboundTransferHanders[trackId] = null;
        }
    }
    
    private boolean hasActiveChannel() {
        for (InboundTransferHandler handler: inboundTransferHanders) 
            if (handler!=null)
                return true;
        return false;
    }
    
    private void processClose() {
        try {
            try {
                for (PushBufferDataSource dataSource : dataSources) {
                    dataSource.getStreams()[0].setTransferHandler(null);
                    try {
                        dataSource.stop();
                    } finally {
                        dataSource.disconnect();
                    }
                }
            } finally {
                try {
                    if (mux!=null && mux.getDataOutput()!=null) {
                        if (muxInitialized) 
                            mux.close();
                        PushDataSource ds = (PushDataSource) mux.getDataOutput();
                        ds.getStreams()[0].setTransferHandler(null);
                        mux.getDataOutput().stop();
                        mux.getDataOutput().disconnect();
                    }
                } finally {
                    if (out!=null)
                        out.close();
                }
            }
        } catch (Exception e) {
            if (getLogger().isErrorEnabled())
                getLogger().error("Error while closing audio file writer", e);
        }
    }
        
    public final static class Init {
        private final File file;
        private final PushBufferDataSource[] dataSources;
        private final CodecManager codecManager;
        private final String contentType;

        public Init(File file, PushBufferDataSource[] dataSources, CodecManager codecManager, String contentType) {
            this.file = file;
            this.dataSources = dataSources;
            this.codecManager = codecManager;
            this.contentType = contentType;
        }

        @Override
        public String toString() {
            return String.format("INIT (contentType:%s, file:%s, ds count: %s)", contentType, file, dataSources.length);
        }
    }
    
    private final static class CloseTrack {
        private final int trackID;

        public CloseTrack(int trackID) {
            this.trackID = trackID;
        }

        @Override
        public String toString() {
            return "CloseTrack: "+trackID;
        }
    }
    
    private static class BufferProcessor extends AbstractDataProcessorLogic {
        private final Multiplexer mux;

        public BufferProcessor(Multiplexer mux) {
            this.mux = mux;
        }

        @Override @SuppressWarnings("empty-statement")
        public Object processData(Object message) throws Exception {
            if (message instanceof Buffer) {
                Buffer buffer = (Buffer)message;
                int trackId = (int) buffer.getSequenceNumber();
                while (mux.process(buffer, trackId) > 1) ;
                if (buffer.isEOM())
                    getContext().getParent().send(new CloseTrack(trackId));
                return VOID;
            } else
                return UNHANDLED;
        }        
    }
    
//    privte
    
    private class InboundTransferHandler implements BufferTransferHandler {
        private final int trackID;
        private final DataProcessorFacade bufferProcessor;

        public InboundTransferHandler(int trackID, DataProcessorFacade bufferProcessor) {
            this.trackID = trackID;
            this.bufferProcessor = bufferProcessor;
        }

        @Override
        public void transferData(PushBufferStream sourceStream) {
            try {
                Buffer buffer = new Buffer();
                sourceStream.read(buffer);
                if (buffer.isDiscard())
                    return;
                buffer.setSequenceNumber(trackID);
                bufferProcessor.send(buffer);                
            } catch (IOException ex) {
                getFacade().send(new CloseTrack(trackID));
                getLogger().error("Buffer transfer error in INBOUND hander", ex);
//                processError(ex);
            }
        }
    }
    
    private class MuxTransferHandler implements SourceTransferHandler, Seekable {
        private final byte[] buffer = new byte[128];

        @Override
        public void transferData(PushSourceStream sourceStream) {
            int cnt;
            try {
                while ((cnt = sourceStream.read(buffer, 0, buffer.length)) > 0) {
                    out.write(buffer, 0, cnt);
                }
            } catch (IOException ex) {
                if (getLogger().isErrorEnabled())
                    getLogger().error("Error writing to audio to file", ex);
                getFacade().send(WRITE_TO_FILE_ERROR);
            }
        }

        @Override
        public long seek(long pos) {
            try {
                out.seek(pos);
                return pos;
            } catch (IOException ex) {
                if (getLogger().isErrorEnabled())
                    getLogger().error("Seeking error", ex);
                getFacade().send(WRITE_TO_FILE_ERROR);
                return -1;
            }
        }

        @Override
        public long tell() {
            try {
                return out.getFilePointer();
            } catch (IOException ex) {
                if (getLogger().isErrorEnabled())
                    getLogger().error("Telling error", ex);
                getFacade().send(WRITE_TO_FILE_ERROR);
                return -1;
            }
        }

        @Override
        public boolean isRandomAccess() {
            return true;
        }
    }
}
