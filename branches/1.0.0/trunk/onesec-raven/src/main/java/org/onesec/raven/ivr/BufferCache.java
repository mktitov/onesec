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

package org.onesec.raven.ivr;

import java.util.Collection;
import java.util.List;
import javax.media.Buffer;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface BufferCache
{
    /**
     * Returns the silent buffer for passed parameters
     * @param codec codec
     * @param packetSize of silent buffer
     */
    public Buffer getSilentBuffer(ExecutorService executor, Node requester, Codec codec, int packetSize);
    /**
     * Returns cached buffers or null if cache does not contains buffers for specified parameters
     * @param key the key of the cache
     * @param checksum checksum of the cache content
     * @param codec codec of the buffers
     * @param packetSize the packet size of buffers
     * @return
     */
    public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize);
    /**
     * Caches buffers
     * @param key the key of the cache
     * @param checksum checksum of the cache content
     * @param codec codec of the buffers
     * @param packetSize the packet size of buffers
     * @param buffers buffers that must be cached
     */
    public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> buffers);
    /**
     * Removes caches which idle time are more then {@link #getMaxCacheIdleTime()}
     */
    public void removeOldCaches();
    /**
     * Sets the max idle time of the cache in seconds
     */
    public void setMaxCacheIdleTime(long time);
    /**
     * Returns the max idle time of the cache in seconds
     */
    public long getMaxCacheIdleTime();

    public List<BuffersCacheEntity> getCacheEntities();

    public List<String> getSilentBuffersKeys();
}
