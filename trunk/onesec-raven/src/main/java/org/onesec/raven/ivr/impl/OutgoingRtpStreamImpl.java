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

import java.io.IOException;
import java.net.InetAddress;
import javax.media.control.BufferControl;
import javax.media.rtp.*;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.SessionEvent;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImpl extends AbstractRtpStream implements OutgoingRtpStream
{
    @Service
    private static RTPManagerService rtpManagerService;

    private AudioStream audioStream;
    private RTPManager rtpManager;
    private SendStream sendStream;
    private SessionAddress destAddress;

    public OutgoingRtpStreamImpl(InetAddress address, int portNumber)
    {
        super(address, portNumber, "Outgoing RTP");
    }

    public long getHandledBytes()
    {
        return 0;
    }

    public long getHandledPackets()
    {
        return 0;
    }

    public void open(String remoteHost, int remotePort, AudioStream audioStream) throws RtpStreamException
    {
        try
        {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess(
                        "Trying to open outgoing RTP stream to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));
            this.audioStream = audioStream;
            destAddress = new SessionAddress(InetAddress.getByName(remoteHost), remotePort);
            rtpManager = rtpManagerService.createRtpManager();
            rtpManager.initialize(new SessionAddress(address, port));
            rtpManager.addTarget(destAddress);
//            Listener listener = new Listener();
//            rtpManager.addReceiveStreamListener(listener);
//            rtpManager.addRemoteListener(listener);
//            rtpManager.addSendStreamListener(listener);
//            rtpManager.addSessionListener(listener);
            sendStream = rtpManager.createSendStream(audioStream.getDataSource(), 0);
            sendStream.setBitRate(1);
            BufferControl control = (BufferControl)rtpManager.getControl(BufferControl.class.getName());
            control.setMinimumThreshold(60);
            control.setBufferLength(60);
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess(
                        "RTP stream was successfully opened to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));
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

    @Override
    public void doRelease() throws Exception
    {
        if (sendStream==null)
            return;
        TransmissionStats stats = sendStream.getSourceTransmissionStats();
        incHandledBytesBy(stats.getBytesTransmitted());
        incHandledPacketsBy(stats.getPDUTransmitted());
        try{
            try{
                try {
                    audioStream.close();
                } finally {
                    sendStream.close();
                }
            }finally{
                rtpManager.removeTarget(destAddress, "disconnected");
            }
        }finally{
            rtpManager.dispose();
        }
        
    }

    public void start() throws RtpStreamException
    {
        try 
        {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Starting rtp packets transmission..."));
            sendStream.start();
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Rtp packets transmission started"));
        }
        catch (IOException ex)
        {
            throw new RtpStreamException(
                    String.format(
                        "Outgoing RTP. Error start outgoing rtp stream (remote address: %s; remote port: %s)"
                        , remoteHost, remotePort)
                    , ex);
        }
    }
    
    private class Listener implements ReceiveStreamListener, RemoteListener, SendStreamListener, SessionListener {

        public void update(ReceiveStreamEvent event) {
            owner.getLogger().debug(logMess("ReceiveStreamListener event: %s", event.getClass().getName()));
        }

        public void update(RemoteEvent event) {
            owner.getLogger().debug(logMess("RemoteEvent event: %s", event.getClass().getName()));
        }

        public void update(SendStreamEvent event) {
            owner.getLogger().debug(logMess("SendStreamEvent event: %s", event.getClass().getName()));
        }

        public void update(SessionEvent event) {
            owner.getLogger().debug(logMess("SessionEvent event: %s", event.getClass().getName()));
        }
    }
}
