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

package org.onesec.raven.ivr.impl;

import java.net.InetAddress;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.OutgoingRtpStream;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImpl extends AbstractRtpStream implements OutgoingRtpStream
{

    public OutgoingRtpStreamImpl(InetAddress address, int portNumber)
    {
        super(address, portNumber);
    }

    public long getHandledBytes()
    {
        return 0;
    }

    public long getHandledPackets()
    {
        return 0;
    }

    public void open(String remoteHost, int remotePort, AudioStream audioStream)
    {
    }
}
