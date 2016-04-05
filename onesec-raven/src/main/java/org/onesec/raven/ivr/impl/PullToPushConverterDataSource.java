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
import javax.media.Time;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PullToPushConverterDataSource extends PushBufferDataSource {
    
    private final PullBufferDataSource source;
    private final ExecutorService executor;
    private final PullToPushConverterDataStream[] streams;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();

    public PullToPushConverterDataSource(PullBufferDataSource source, ExecutorService executor, Node owner) {
        this.source = source;
        this.executor = executor;
        this.streams = new PullToPushConverterDataStream[]{new PullToPushConverterDataStream(
                owner, source.getStreams()[0])};
    }

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
        if (!connected.compareAndSet(false, true))
            return;
        source.connect();
        streams[0].reset();
        try {
            executor.execute(streams[0]);
        } catch (ExecutorServiceException ex) {
            throw new IOException("Error starting Pull to Push BufferDataSource converter", ex);
        }
    }

    @Override
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            try {
                source.disconnect();
            } finally {
                streams[0].stop();
            }
        }
    }

    @Override
    public void start() throws IOException {
        if (connected.get() && started.compareAndSet(false, true)) {
            streams[0].cont();
            source.start();
        }
    }

    @Override
    public void stop() throws IOException {
        if (started.compareAndSet(true, false))
            try {
                source.stop();
            } finally {
                streams[0].pause();
            }
    }

    @Override
    public Object getControl(String controlName) {
        return source.getControl(controlName);
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
