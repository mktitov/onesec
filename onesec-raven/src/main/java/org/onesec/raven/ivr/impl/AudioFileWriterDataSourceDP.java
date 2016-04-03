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
import javax.media.Multiplexer;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
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
    //
    private File file;
    private PushBufferDataSource[] dataSources;
    private Multiplexer mux;
            
    private final Behaviour INITILIZING = new Behaviour("INITIALIZING") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof Init) {
                final Init params = (Init)message;
                file = params.file;
                dataSources = params.dataSources;
                mux = params.codecManager.buildMultiplexer(params.contentType);
                if (mux==null) {
                    getLogger().error(String.format("Not found multiplexer for content type (%s)", params.contentType));
                    getFacade().stop();
                    
                }
                mux.setContentDescriptor(new ContentDescriptor(params.contentType));
                mux.setNumTracks(dataSources.length);
                return VOID;
            } else {
                return UNHANDLED;
            }
        }
    };

    @Override
    public void init(DataProcessorFacade facade, DataProcessorContext context) {
        context.become(INITILIZING, true);
    }

    @Override
    public void postStop() {
    }

    @Override
    public void childTerminated(DataProcessorFacade child) {
    }
    
    @Override
    public Object processData(Object dataPackage) throws Exception {
        return null;
    }
    
    public final class Init {
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
        
    }
}
