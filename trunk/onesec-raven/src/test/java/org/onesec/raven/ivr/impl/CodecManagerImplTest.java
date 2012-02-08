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

import com.sun.media.parser.audio.WavParser;
import javax.media.Demultiplexer;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import org.onesec.raven.ivr.CodecConfig;
import org.junit.*;
import org.onesec.raven.ivr.CodecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.onesec.raven.ivr.Codec.*;

/**
 *
 * @author Mikhail Titov
 */
public class CodecManagerImplTest extends Assert {
    
    private CodecManager manager;
    private Logger logger = LoggerFactory.getLogger(CodecManager.class);
    
    @Before
    public void prepare() throws Exception {
        manager = new CodecManagerImpl(logger);
    }

    @Test
    public void buildCodecChainTest() throws Exception {
        long startTs = System.currentTimeMillis();
        CodecConfig[] codecs = manager.buildCodecChain(G711_MU_LAW.getAudioFormat(), G729.getAudioFormat());
        for (int i=1; i<10000; ++i)
            codecs = manager.buildCodecChain(G711_MU_LAW.getAudioFormat(), G729.getAudioFormat());
        logger.debug("Processing time: {}", System.currentTimeMillis()-startTs);
        assertNotNull(codecs);
        for (CodecConfig codec: codecs) {
            logger.debug("CODEC: {}", codec.getCodec());
            logger.debug("   INPUT  FORMAT: {}", codec.getInputFormat());
            logger.debug("   OUTPUT FORMAT: {}", codec.getOutputFormat());
        }
//        assertEquals(2, codecs.length);
    }
    
    @Test
    public void buildDemultiplexerTest() {
        Demultiplexer parser = manager.buildDemultiplexer(FileTypeDescriptor.WAVE);
        assertNotNull(parser);
        assertTrue(parser instanceof WavParser);
    }
}
