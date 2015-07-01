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
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.rtp.RtpManagerConfigurator;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImpl extends AbstractRtpStream implements OutgoingRtpStream
{
//    @Service
//    private static RTPManagerService rtpManagerService;

    private AudioStream audioStream;
    private RTPManager rtpManager;
    private SendStream sendStream;
//    private SessionAddress destAddress;

    public OutgoingRtpStreamImpl(InetAddress address, int portNumber, RtpManagerConfigurator configurator)
    {
        super(address, portNumber, "Outbound RTP", configurator);
    }

//    public long getHandledBytes()
//    {
//        return 0;
//    }
//
//    public long getHandledPackets()
//    {
//        return 0;
//    }
    
    protected RTPManager createRtpManager() throws Exception {
        return rtpManagerConfigurator.configureOutboundManager(
                address, port, InetAddress.getByName(remoteHost), remotePort, logger);
        
    }

    public void open(String remoteHost, int remotePort, AudioStream audioStream) throws RtpStreamException {
        try {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Trying to open outgoing RTP stream to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));
            this.audioStream = audioStream;
            rtpManager = createRtpManager();
            sendStream = rtpManager.createSendStream(audioStream.getDataSource(), 0);
            sendStream.setBitRate(1);
            BufferControl control = (BufferControl)rtpManager.getControl(BufferControl.class.getName());
            control.setMinimumThreshold(60);
            control.setBufferLength(60);
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "RTP stream was successfully opened to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));
        } catch(Exception e) {
            throw new RtpStreamException(
                    String.format(
                        "Outgoing RTP. Error opening RTP stream to remote host (%s) using port (%s)"
                        , remoteHost, remotePort)
                    , e);
        }
    }
    
    protected void releaseRtpManager() {
        rtpManager.dispose();
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
            } finally {
//                rtpManager.removeTarget(destAddress, "disconnected");
                rtpManager.removeTargets("disconnected");
            }
        } finally {
            releaseRtpManager();
        }        
    }

    public void start() throws RtpStreamException {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Starting rtp packets transmission...");
            sendStream.start();
            if (logger.isDebugEnabled())
                logger.debug("Rtp packets transmission started");
        }
        catch (IOException ex) {
            throw new RtpStreamException(
                    String.format(
                        "Outgoing RTP. Error start outgoing rtp stream (remote address: %s; remote port: %s)"
                        , remoteHost, remotePort)
                    , ex);
        }
    }
}
