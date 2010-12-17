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

import com.sun.media.rtp.RTPSessionMgr;
import java.net.InetAddress;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.rtcp.SourceDescription;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class IncomingRtpStreamImpl extends AbstractRtpStream 
        implements IncomingRtpStream, ReceiveStreamListener
{
    @Service
    private static RTPManagerService rtpManagerService;

    private RTPManager rtpManager;
    private RTPSessionMgr sessionManager;
    private SessionAddress destAddress;

    public IncomingRtpStreamImpl(InetAddress address, int port)
    {
        super(address, port, "Incoming RTP");
    }

    public long getHandledBytes()
    {
        return 0;
    }

    public long getHandledPackets()
    {
        return 0;
    }

    @Override
    public void doRelease() throws Exception
    {
        sessionManager.dispose();
    }

    public void open(String remoteHost, int remotePort) throws RtpStreamException
    {
        try
        {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess(
                        "Trying to open incoming RTP stream to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));

            rtpManager = rtpManagerService.createRtpManager();
            rtpManager.addReceiveStreamListener(this);
            rtpManager.initialize(new SessionAddress(address, port));
            InetAddress dest = InetAddress.getByName(remoteHost);
            destAddress = new SessionAddress(dest, SessionAddress.ANY_PORT);
            rtpManager.addTarget(destAddress);

//            sessionManager = new RTPSessionMgr();
//            sessionManager.addReceiveStreamListener(this);
//
//            SessionAddress localAddress = new SessionAddress(address, port);
//            InetAddress dest = InetAddress.getByName(remoteHost);
////            destAddress = new SessionAddress(dest, remotePort, dest, remotePort+1);
//            destAddress = new SessionAddress(dest, SessionAddress.ANY_PORT);
//
//            SourceDescription[] userdesclist = new SourceDescription[]{
//                new SourceDescription(SourceDescription.SOURCE_DESC_EMAIL, "raven@some.com", 1, false)
//                , new SourceDescription(SourceDescription.SOURCE_DESC_NAME, "Raven", 1, false)
//                , new SourceDescription(SourceDescription.SOURCE_DESC_CNAME, sessionManager.generateCNAME(), 1, false)
//                , new SourceDescription(SourceDescription.SOURCE_DESC_TOOL, "JMF RTP Player v2.0", 1, false)
//            };
//
//            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                owner.getLogger().debug(logMess("Initializing session"));
//            sessionManager.initSession(
//                    localAddress, sessionManager.generateSSRC(), userdesclist, 0.05, 0.25);
//            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                owner.getLogger().debug(logMess("Starting session"));
//            sessionManager.startSession(destAddress, 1, null);


        }
        catch(Exception e)
        {
            throw new RtpStreamException(
                    String.format(
                        "Outgoing RTP. Error opening RTP stream to remote host (%s) using port (%s)"
                        , remoteHost, remotePort)
                    , e);
        }
    }

    public void start()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void update(ReceiveStreamEvent event)
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Received stream event (%s)", event.getClass().getName()));
    }

}
