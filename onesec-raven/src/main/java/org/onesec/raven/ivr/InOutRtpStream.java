/*
 * Copyright 2014 Mikhail Titov.
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
package org.onesec.raven.ivr;

/**
 *
 * @author Mikhail Titov
 */
public interface InOutRtpStream extends RtpStream, RtpStreamManager {
    /**
     * Creates full duplex channel to this remote host and port
     */
    public void open(String remoteHost, int remotePort) throws RtpStreamException;
    /**
     * Returns amount of bytes handled by inbound stream.
     */
    public long getInboundHandledBytes();
    /**
     * Returns amount of packets handled by inbound stream.
     */
    public long getInboundHandledPackets();
    /**
     * Returns amount of bytes handled by outbound stream.
     */
    public long getOutboundHandledBytes();
    /**
     * Returns amount of packets handled by outbound stream.
     */
    public long getOutboundHandledPackets();

}
