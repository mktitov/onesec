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
package org.onesec.raven.ivr.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.ConferenceMixerSession;
import org.onesec.raven.ivr.MixerHandler;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeConferenceMixer extends AbstractRealTimeMixer {

    public RealTimeConferenceMixer(CodecManager codecManager, Node owner, LoggerHelper logger, 
        ExecutorService executor, int noiseLevel, double maxGainCoef) 
    {
        super(codecManager, owner, logger, executor, noiseLevel, maxGainCoef);
    }
    
    public ConferenceMixerSession addDataSource(PushBufferDataSource ds) throws Exception {
        Handler handler = new Handler(ds);
        addDataSourceHandler(handler);
        return handler;
    }

    @Override
    protected void applyBufferToHandlers(MixerHandler firstHandler, int[] data, int len, int streamsCount, 
        double maxGainCoef, int bufferSize) 
    {
        Handler handler = (Handler)firstHandler;
        while (handler!=null) {
            if (!handler.isSessionStopped())
                handler.applyMergedBuffer(data, len, streamsCount, maxGainCoef, bufferSize);
        }
    }
    
    private static class BufferData {
        private final int[] data;
        private final byte[] byteData;
        private final int len;
        private final int streamsCount;
        private final int bufferSize;
        private final double maxGainCoef;

        public BufferData(int[] selfData, int[] data, int len, int streamsCount, int bufferSize, 
                double maxGainCoef) 
        {
            this.data = Arrays.copyOf(data, len);
            this.len = len;
            if (selfData!=null)
                for (int i=0; i<len; ++i)
                    data[i]-=selfData[i];
            this.streamsCount = streamsCount - (selfData==null? 0:1);
            this.bufferSize = bufferSize;
            this.maxGainCoef = maxGainCoef;
            this.byteData = new byte[len];
        }
    }
    
    class Handler extends AbstractMixerHandler implements ConferenceMixerSession {
        private final int[] selfData = new int[BUFFER_SIZE];
        private volatile boolean hasData = false;
        private final AtomicBoolean sessionStopped = new AtomicBoolean();
        private final HandlerDataSource conferenceAudio = new HandlerDataSource();

        public Handler(PushBufferDataSource datasource) throws Exception {
            super(codecManager, datasource, FORMAT, logger);
        }
        
        public boolean isSessionStopped() {
            return sessionStopped.get();
        }

        public void replaceParticipantAudio(PushBufferDataSource audioSource) throws Exception {
            replaceDataSource(audioSource);
        }

        public void stopParticipantAudio() throws Exception {
            replaceDataSource(null);
        }

        @Override
        public boolean isAlive() {
            return super.isAlive() || !sessionStopped.get();
        }

        public void applyProcessingBuffer(int[] buffer) {
            if (buffer==null) 
                hasData = false;
            else {
                System.arraycopy(buffer, 0, selfData, 0, buffer.length);
                hasData = true;
            }
        }

        public void applyMergedBuffer(int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize) {
            conferenceAudio.processConferenceAudioData(new BufferData(
                    hasData? selfData:null, data, len, streamsCount, bufferSize, maxGainCoef));
        }

        public boolean stopSession() {
            return sessionStopped.compareAndSet(false, true);
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
        @Override public void stop() throws IOException { }

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

        public Format getFormat() {
            return FORMAT;
        }
        
        public void setAudioData(BufferData data) {
            this.audioData = data;
            BufferTransferHandler _transferHandler = transferHandler;
            if (_transferHandler!=null)
                _transferHandler.transferData(this);
        }

        public void read(Buffer buffer) throws IOException {
            BufferData a = audioData;
            if (a!=null) 
                createBuffer(buffer, a.data, a.byteData, a.len, a.streamsCount, a.maxGainCoef, a.bufferSize);
            else 
                buffer.setDiscard(true);
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
