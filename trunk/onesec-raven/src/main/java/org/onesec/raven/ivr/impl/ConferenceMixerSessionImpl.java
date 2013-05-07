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

import javax.media.protocol.PushBufferDataSource;
import org.onesec.raven.ivr.ConferenceMixerSession;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceMixerSessionImpl extends AbstractMixerHandler implements ConferenceMixerSession {

    public void applyProcessingBuffer(int[] buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void applyMergedBuffer(int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean stopSession() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PushBufferDataSource getConferenceAudioSource() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
