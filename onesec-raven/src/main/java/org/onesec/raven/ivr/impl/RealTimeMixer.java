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

import javax.media.protocol.PushBufferDataSource;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.MixerHandler;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeMixer extends AbstractRealTimeMixer {

    public RealTimeMixer(CodecManager codecManager, Node owner, String logPrefix, ExecutorService executor, 
            int noiseLevel, double maxGainCoef) 
    {
        super(codecManager, owner, new LoggerHelper(owner, (logPrefix==null?"":logPrefix)+"Mixer. "), 
                executor, noiseLevel, maxGainCoef);
    }

    public RealTimeMixer(CodecManager codecManager, Node owner, LoggerHelper logger, ExecutorService executor, 
            int noiseLevel, double maxGainCoef) 
    {
        super(codecManager, owner, new LoggerHelper(logger, "Mixer. "), 
                executor, noiseLevel, maxGainCoef);
    }

    @Override
    protected void applyBufferToHandlers(MixerHandler firstHandler, int[] data, int len, int streamsCount, 
        double maxGainCoef, int bufferSize) 
    {
    }
    
    public void addDataSource(PushBufferDataSource dataSource) throws Exception {
        addDataSourceHandler(new Handler(dataSource));
    }    
    
    private class Handler extends AbstractMixerHandler {
        public Handler(PushBufferDataSource datasource) throws Exception {
            super(codecManager, datasource, FORMAT, RealTimeMixer.this.logger);
        }

        @Override public void applyProcessingBuffer(int[] buffer) {}
        @Override public void applyMergedBuffer(int[] data, int len, int streamsCount, double maxGainCoef, int bufferSize) {}
    }    
}
