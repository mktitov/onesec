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
import javax.media.Time;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.raven.tree.Node;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataSource extends PushBufferDataSource {

    private final PushBufferDataSource source;
    private final RealTimeDataStream[] streams;
    private final Node owner;
    private final String logPrefix;

    public RealTimeDataSource(PushBufferDataSource source, Node owner, String logPrefix) {
        this.source = source;
        this.owner = owner;
        this.logPrefix = logPrefix;
        streams = new RealTimeDataStream[]{new RealTimeDataStream(this, source.getStreams()[0])};
    }
    
    public long getDiscardedBuffersCount() {
        return streams[0].getDiscardedBuffersCount();
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
        source.connect();
    }

    @Override
    public void disconnect() {
        source.disconnect();
    }

    @Override
    public void start() throws IOException {
        source.start();
    }

    @Override
    public void stop() throws IOException {
        source.stop();
    }

    @Override
    public Object getControl(String control) {
        return source.getControl(control);
    }

    @Override
    public Object[] getControls() {
        return source.getControls();
    }

    @Override
    public Time getDuration() {
        return source.getDuration();
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+" RealTimeSource. "+String.format(mess, args);
    }
    
    Logger getLogger() {
        return owner.getLogger();
    }
}
