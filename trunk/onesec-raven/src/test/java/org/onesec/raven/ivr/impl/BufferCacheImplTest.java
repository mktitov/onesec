/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.io.IOException;
import javax.media.Buffer;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.Codec;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class BufferCacheImplTest extends Assert
{
    @Test
    public void test() throws IOException
    {
        RTPManagerServiceImpl manager = new RTPManagerServiceImpl(LoggerFactory.getLogger("Rtp Manager"));
        BufferCacheImpl cache = new BufferCacheImpl(manager, LoggerFactory.getLogger(BufferCacheImpl.class));
        Buffer silentBuffer = cache.getSilentBuffer(Codec.G711_A_LAW, 160);
        assertNotNull(silentBuffer);
        Buffer silentBuffer2 = cache.getSilentBuffer(Codec.G711_A_LAW, 160);
        assertSame(silentBuffer2, silentBuffer);
    }
}