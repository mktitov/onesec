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
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 *
 * @author Mikhail Titov
 */
public class TranscoderDataSource extends PushBufferDataSource {
    
    private final PushBufferDataSource source;
    private final Format outputFormat;

    public TranscoderDataSource(PushBufferDataSource source, Format outputFormat) {
        this.source = source;
        this.outputFormat = outputFormat;
    }
    
    private void processBuffer(Buffer src) {
        
    }

    @Override
    public PushBufferStream[] getStreams() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    }

    @Override
    public void stop() throws IOException {
    }

    @Override
    public Object getControl(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
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
