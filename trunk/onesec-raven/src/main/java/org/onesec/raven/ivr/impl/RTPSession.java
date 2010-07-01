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
import java.util.Collection;
import javax.media.control.BufferControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.rtcp.Feedback;
import org.onesec.raven.ivr.RTPManagerService;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class RTPSession 
{
    private final ConcatDataSource source;
    private final RTPManager rtpManager;
    private final SendStream sendStream;
    private final SessionAddress destAddress;

    @Service
    private static RTPManagerService rtpManagerService;

    public RTPSession(String host, int port, ConcatDataSource source) throws Exception
    {
        this.source = source;
        destAddress = new SessionAddress(InetAddress.getByName(host), port);
        rtpManager = rtpManagerService.createRtpManager();
        rtpManager.initialize(new SessionAddress());
        rtpManager.addTarget(destAddress);
        sendStream = rtpManager.createSendStream(source, 0);
        rtpManager.addRemoteListener(new RemoteListener() {
            public void update(RemoteEvent event) {
                System.out.println("!!! RECIEVED REPORT: "+event.toString());
                if (event instanceof ReceiverReportEvent){
                    Collection<Feedback> feedbacks = ((ReceiverReportEvent)event).getReport().getFeedbackReports();
                    if (feedbacks!=null)
                        for (Feedback f: feedbacks)
                            System.out.println(String.format(
                                "FEEDBACK: DLSR=%s; FractionLost=%s; Jitter=%s; LSR=%s; NumLost=%s"
                                , f.getDLSR(), f.getFractionLost(), f.getJitter(), f.getLSR(), f.getNumLost()));
                }
            }
        });
        sendStream.setBitRate(1);
        BufferControl control = (BufferControl)rtpManager.getControl(BufferControl.class.getName());
        System.out.println(String.format(
                "!!! Buffer control length: %s, threshold enabled: %s, minimum threshold: %s"
                , control.getBufferLength(), control.getEnabledThreshold()
                , control.getMinimumThreshold()));
        control.setMinimumThreshold(60);
        control.setBufferLength(60);
    }

    public void start() throws IOException
    {
        sendStream.start();
    }

    public void stop() throws Exception
    {
        try {
            try {
                try {
                    source.close();
                } finally {
                    sendStream.close();
                }
            } finally {
                rtpManager.removeTarget(destAddress, "disconnected");
            }
        } finally {
            rtpManager.dispose();
        }
    }

    public void update(RemoteEvent event) 
    {
    }
}
