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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.control.BufferControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.TransmissionStats;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImpl extends AbstractRtpStream implements OutgoingRtpStream
{
    private AudioStream audioStream;
    private RTPManager rtpManager;
    private SendStream sendStream;
    private String remoteHost;
    private int remotePort;

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

    public void open(String remoteHost, int remotePort, AudioStream audioStream) throws RtpStreamException
    {
        try
        {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(String.format(
                        "Outgoing RTP. Trying to open RTP stream to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));
            this.audioStream = audioStream;
            SessionAddress destAddress = new SessionAddress(InetAddress.getByName(remoteHost), remotePort);
            rtpManager = RTPManager.newInstance();
            rtpManager.initialize(new SessionAddress(address, port));
            rtpManager.addTarget(destAddress);
            sendStream = rtpManager.createSendStream(audioStream.getDataSource(), 0);
            sendStream.setBitRate(1);
            BufferControl control = (BufferControl)rtpManager.getControl(BufferControl.class.getName());
            control.setMinimumThreshold(60);
            control.setBufferLength(60);
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(String.format(
                        "Outgoing RTP. RTP stream was successfully opened to the remote host (%s) using port (%s)"
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
        TransmissionStats stats = sendStream.getSourceTransmissionStats();
        incHandledBytesBy(stats.getBytesTransmitted());
        incHandledPacketsBy(stats.getPDUTransmitted());
        try
        {
            audioStream.close();
        }
        finally
        {
            sendStream.close();
        }
        
    }

    public void start() throws RtpStreamException
    {
        try 
        {
            sendStream.start();
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
}
