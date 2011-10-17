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
import javax.media.Buffer;

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
    public Buffer getSilentBuffer(Codec codec, int packetSize);
    
    public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize);
    
    public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> buffers);
}
