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

import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface RtpStreamManager
{
    /**
     * Return the incoming rtp stream (the object that can receive the incoming rtp stream).
     * The stream must be {@link RtpStreamHandler#release() released}
     * after use.
     */
    public IncomingRtpStream getIncomingRtpStream(Node owner);
    /**
     * Returns the outgoing rtp stream. The stream must be {@link RtpStream#release() released}
     * after use.
     */
    public OutgoingRtpStream getOutgoingRtpStream(Node owner);
    /**
     * Reserves the address by the node passed in the parameter. Next time the call of the method
     * {@link #getOutgoingRtpStream(Node)} returns the stream with the address and port reserved by
     * this method.
     * @param node the node which reserves the pair of the ip address and the port
     * @return the reserved address and the port
     */
    public RtpAddress reserveAddress(Node node);
    /**
     * Unreserve the address and the port reserved for this node
     * @param node
     */
    public void unreserveAddress(Node node);
}
