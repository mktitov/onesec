/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.rtp;

import java.net.InetAddress;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;
import org.onesec.raven.ivr.RTPManagerService;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class StandartRtpManagerConfigurator implements RtpManagerConfigurator {
    private final RTPManagerService rtpManagerService;

    public StandartRtpManagerConfigurator(RTPManagerService rtpManagerService) {
        this.rtpManagerService = rtpManagerService;
    }

    private RTPManager configure(InetAddress localAddress, int localPort, InetAddress remoteAddress, 
            int remotePort, LoggerHelper logger) throws Exception
    {
        RTPManager rtpManager = rtpManagerService.createRtpManager();
        rtpManager.initialize(new SessionAddress(localAddress, localPort));
        rtpManager.addTarget(new SessionAddress(remoteAddress, remotePort));
        return rtpManager;
    }

    public RTPManager configureInboundManager(InetAddress localAddress, int localPort, 
            InetAddress remoteAddress, int remotePort, LoggerHelper logger) throws Exception 
    {
        return configure(localAddress, localPort, remoteAddress, remotePort, logger);
    }

    public RTPManager configureOutboundManager(InetAddress localAddress, int localPort, InetAddress remoteAddress, int remotePort, LoggerHelper logger) throws Exception {
        return configure(localAddress, localPort, remoteAddress, remotePort, logger);
    }
}
