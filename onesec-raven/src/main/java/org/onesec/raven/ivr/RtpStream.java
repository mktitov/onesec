/*
 *  Copyright 2009 Mikhail Titov.
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

import java.util.Map;
import org.raven.tree.impl.LoggerHelper;

/**
 * The base contract for {@link OutgoingRtpStream} and {@link IncomingRtpStream} rtp streams.
 * @author Mikhail Titov
 */
public interface RtpStream extends RtpAddress
{
    /**
     * Do not call this method direct. This method must be used by {@link RtpStreamManager}.
     * @param rtpStat the object that aggregates the global statistics
     */
//    public void init(RtpStat rtpStat);
    /**
     * Releases rtp stream
     */
    public void release();
    /**
     * Returns amount of bytes handled by stream.
     */
    public long getHandledBytes();
    /**
     * Returns amount of packets handled by stream.
     */
    public long getHandledPackets();
    /**
     * Returns the address of the remote side
     */
    public String getRemoteHost();
    /**
     * Returns the port on the remote side
     */
    public int getRemotePort();
    /**
     * Returns the stream creation time
     */
    public long getCreationTime();
    
//    public void setLogPrefix(String prefix);
    public void setLogger(LoggerHelper logger);
    
    public Map<String, Object> getStat();
}
