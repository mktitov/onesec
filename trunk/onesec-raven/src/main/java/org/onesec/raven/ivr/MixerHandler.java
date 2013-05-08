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
package org.onesec.raven.ivr;

import java.io.IOException;
import javax.media.Buffer;
import javax.media.protocol.PushBufferDataSource;
import org.onesec.raven.RingQueue;

/**
 *
 * @author Mikhail Titov
 */
public interface MixerHandler<H extends MixerHandler> {
//    public void init() throws IOException;
    public boolean isAlive();
    public RingQueue<Buffer> getQueue();
//    public Buffer peek();
//    public Buffer pop();
    public H getNextHandler();
    public void setNextHandler(H handler);
    public void applyProcessingBuffer(int[] buffer);
    public void applyMergedBuffer(int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize);
    public PushBufferDataSource getDataSource();
    public void connect() throws IOException;
    public void disconnect();
    public void start() throws IOException;
    public void stop() throws IOException;
}
