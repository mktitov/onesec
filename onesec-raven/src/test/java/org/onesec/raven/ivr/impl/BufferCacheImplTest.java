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

import java.util.Arrays;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.Buffer;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.RTPManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class BufferCacheImplTest extends Assert
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void silentBufferTest() throws IOException
    {
        Logger log = LoggerFactory.getLogger("Rtp Manager");
        RTPManagerServiceImpl manager = new RTPManagerServiceImpl(log, new CodecManagerImpl(log));
        BufferCacheImpl cache = new BufferCacheImpl(manager, logger);
        Buffer silentBuffer = cache.getSilentBuffer(Codec.G711_A_LAW, 160);
        assertNotNull(silentBuffer);
        Buffer silentBuffer2 = cache.getSilentBuffer(Codec.G711_A_LAW, 160);
        assertSame(silentBuffer2, silentBuffer);
    }

    @Test
    public void cacheBuffersTest() throws Exception {
        RTPManagerService rtpManager = createMock(RTPManagerService.class);
        BufferCacheImpl cache = new BufferCacheImpl(rtpManager, logger);
        Buffer[] buffers = new Buffer[]{null, null};
        Buffer[] res;
        res = cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 1);
        assertNull(res);
        cache.cacheBuffers("key", 1, Codec.G711_A_LAW, 1, Arrays.asList(buffers));
        res = cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 1);
        assertArrayEquals(buffers, res);
        assertNull(cache.getCachedBuffers("key", 2, Codec.G711_A_LAW, 1));
        assertNull(cache.getCachedBuffers("key", 1, Codec.G711_MU_LAW, 1));
        assertNull(cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 2));
    }

    @Test
    public void removeOldCachesTest() throws Exception {
        RTPManagerService rtpManager = createMock(RTPManagerService.class);
        BufferCacheImpl cache = new BufferCacheImpl(rtpManager, logger);
        Buffer[] buffers = new Buffer[]{null, null};
        Buffer[] res;
        cache.cacheBuffers("key", 1, Codec.G711_A_LAW, 1, Arrays.asList(buffers));
        res = cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 1);
        assertArrayEquals(buffers, res);
        cache.setMaxCacheIdleTime(1);
        cache.removeOldCaches();
        assertNotNull(cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 1));
        TimeUnit.MILLISECONDS.sleep(2100);
        cache.removeOldCaches();
        assertNull(cache.getCachedBuffers("key", 1, Codec.G711_A_LAW, 1));
    }
}