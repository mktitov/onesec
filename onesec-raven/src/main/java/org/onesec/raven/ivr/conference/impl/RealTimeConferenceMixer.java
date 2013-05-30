/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.conference.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.onesec.raven.ivr.conference.ConferenceMixerSession;
import org.onesec.raven.ivr.MixerHandler;
import org.onesec.raven.ivr.impl.AbstractMixerHandler;
import org.onesec.raven.ivr.impl.AbstractRealTimeMixer;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeConferenceMixer extends AbstractRealTimeMixer {
    private static final byte[] EMPTY_BUFFER = new byte[BUFFER_SIZE];

    public RealTimeConferenceMixer(CodecManager codecManager, Node owner, LoggerHelper logger, 
        ExecutorService executor, int noiseLevel, double maxGainCoef) 
    {
        super(codecManager, owner, logger, executor, noiseLevel, maxGainCoef);
    }
    
    public ConferenceMixerSession addParticipant(String name, PushBufferDataSource ds) throws Exception {
        Handler handler = new Handler(name, ds, logger);
        addDataSourceHandler(handler);
        return handler;
    }
    
    public void playAudio(String name, PushBufferDataSource audioSource) throws Exception {
        addDataSourceHandler(new PlayAudioHandler(audioSource, logger));
    }

    @Override
    protected void applyBufferToHandlers(MixerHandler firstHandler, final int[] data, int len, int streamsCount, 
        double maxGainCoef, int bufferSize) 
    {
        MixerHandler handler = (Handler)firstHandler;
        while (handler!=null) {
            if (handler instanceof Handler && !((Handler)handler).isSessionStopped())
                handler.applyMergedBuffer(data, len, streamsCount, maxGainCoef, bufferSize);
            handler = handler.getNextHandler();
        }
//        Handler handler = (Handler)firstHandler;
//        while (handler!=null) {
//            if (!handler.isSessionStopped())
//                handler.applyMergedBuffer(data, len, streamsCount, maxGainCoef, bufferSize);
//            handler = (Handler) handler.getNextHandler();
//        }
    }
    
    private static class BufferData {
        private final int[] data;
        private final byte[] byteData;
        private final int len;
        private final int streamsCount;
        private final int bufferSize;
        private final double maxGainCoef;

        public BufferData(final int[] selfData, final int[] data, int len, int streamsCount, int bufferSize, 
                double maxGainCoef, boolean debug) 
        {
//            if (debug)
//                System.out.println(String.format("selfData: %s", selfData==null? "null" : "not null"));
            this.data = Arrays.copyOf(data, bufferSize);
            this.len = len;
            if (selfData!=null)
                for (int i=0; i<len; ++i)
                    this.data[i]-=selfData[i];
            this.streamsCount = streamsCount - (selfData==null? 0:1);
            this.bufferSize = bufferSize;
            this.maxGainCoef = maxGainCoef;
            this.byteData = new byte[bufferSize];
        }
    }
    
    class PlayAudioHandler extends AbstractMixerHandler {

        public PlayAudioHandler(PushBufferDataSource datasource, LoggerHelper logger) throws CodecManagerException, IOException {
            super(codecManager, datasource, FORMAT, new LoggerHelper(logger, "Audio player"));
        }

        public void applyProcessingBuffer(int[] buffer) { }
        public void applyMergedBuffer(int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize) { }
    }
    
    class Handler extends AbstractMixerHandler implements ConferenceMixerSession {
        private final int[] selfData = new int[BUFFER_SIZE];
        private volatile boolean hasData = false;
        private final AtomicBoolean sessionStopped = new AtomicBoolean();
        private final HandlerDataSource conferenceAudio = new HandlerDataSource();
        private final String name;
        private volatile boolean muted = false;

        public Handler(String name, PushBufferDataSource datasource, LoggerHelper logger) throws Exception {
            super(codecManager, datasource, FORMAT, new LoggerHelper(logger, "["+name+"]. "));
            this.name = name;
            if (this.logger.isDebugEnabled())
                this.logger.debug("Connected");
        }
        
        public boolean isSessionStopped() {
            return sessionStopped.get();
        }

        public void replaceParticipantAudio(PushBufferDataSource audioSource) throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("Unmuting...");
            replaceDataSource(audioSource);
            muted = audioSource==null;
            if (logger.isDebugEnabled())
                logger.debug("Unmuted");
        }

        public void stopParticipantAudio() throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("Muting...");
            replaceDataSource(null);
            muted = true;
            if (logger.isDebugEnabled())
                logger.debug("Muted...");
        }

        @Override
        public void stop() throws IOException {
            super.stop();
            conferenceAudio.stop();
        }

        @Override
        public boolean isAlive() {
            return super.isAlive() || !sessionStopped.get();
        }

        public void applyProcessingBuffer(final int[] buffer) {
            if (buffer==null) 
                hasData = false;
            else {
                System.arraycopy(buffer, 0, selfData, 0, buffer.length);
                hasData = true;
            }
        }

        public void applyMergedBuffer(final int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize) {
            conferenceAudio.processConferenceAudioData(new BufferData(
                    hasData && !muted? selfData:null, data, len, streamsCount, bufferSize, maxGainCoef, false));
        }

        public boolean stopSession() throws Exception {
            if (sessionStopped.compareAndSet(false, true)) {
                if (logger.isDebugEnabled())
                    logger.debug("Stopping mixer session");
                stop();
                return true;
            } else 
                return false;
        }

        public PushBufferDataSource getConferenceAudioSource() {
            return conferenceAudio;
        }
    }
    
    private class HandlerDataSource extends PushBufferDataSource {
        private final PushBufferStream[] streams = new PushBufferStream[]{new HandlerStream()};

        @Override
        public PushBufferStream[] getStreams() {
            return streams;
        }

        @Override
        public String getContentType() {
            return ContentDescriptor.RAW;
        }

        @Override public void connect() throws IOException { }
        @Override public void disconnect() { }
        
        @Override public void start() throws IOException { }
        
        @Override public void stop() throws IOException { 
            ((HandlerStream)streams[0]).stop();
        }

        @Override public Object getControl(String paramString) {
            return null;
        }

        @Override public Object[] getControls() {
            return null;
        }

        @Override public Time getDuration() {
            return DURATION_UNKNOWN;
        }
        
        public void processConferenceAudioData(BufferData data) {
            ((HandlerStream)streams[0]).setAudioData(data);
        }
    }
    
    private class HandlerStream implements PushBufferStream {
        private volatile BufferTransferHandler transferHandler;
        private volatile BufferData audioData;
        private AtomicBoolean stopped = new AtomicBoolean();

        public Format getFormat() {
            return FORMAT;
        }
        
        public void stop() {
            if (stopped.compareAndSet(false, true)) 
                informTransferHandler();
        }
        
        public void setAudioData(BufferData data) {
            this.audioData = data;
            informTransferHandler();
        }
        
        private void informTransferHandler() {
            BufferTransferHandler _transferHandler = transferHandler;
            if (_transferHandler!=null)
                _transferHandler.transferData(this);
        }

        public void read(Buffer buffer) throws IOException {
//            logger.debug("READING buffer");
            if (stopped.get()) {
                buffer.setLength(0);
                buffer.setEOM(true);
                buffer.setData(EMPTY_BUFFER);
                buffer.setOffset(0);
                buffer.setFormat(FORMAT);
            } else {
                BufferData a = audioData;
                if (a!=null) 
                    createBuffer(buffer, a.data, a.byteData, a.len, a.streamsCount, a.maxGainCoef, a.bufferSize);
                else 
                    buffer.setDiscard(true);
            }
        }

        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        public ContentDescriptor getContentDescriptor() {
            return CONTENT_DESCRIPTOR;
        }

        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        public boolean endOfStream() {
            return false;
        }

        public Object[] getControls() {
            return null;
        }

        public Object getControl(String paramString) {
            return null;
        }
    }
}
