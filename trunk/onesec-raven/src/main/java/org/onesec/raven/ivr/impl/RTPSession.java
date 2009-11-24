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
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;

/**
 *
 * @author Mikhail Titov
 */
public class RTPSession
{
//    private final DataSink session;
    private final ConcatDataSource source;
    private final RTPManager rtpManager;
    private final SendStream sendStream;

    public RTPSession(String host, int port, ConcatDataSource source) throws Exception
    {
        this.source = source;
        SessionAddress destAddress = new SessionAddress(InetAddress.getByName(host), port);
        rtpManager = RTPManager.newInstance();
        rtpManager.initialize(new SessionAddress());
        rtpManager.addTarget(destAddress);
        sendStream = rtpManager.createSendStream(source, 0);
        sendStream.setBitRate(1);
        BufferControl control = (BufferControl)rtpManager.getControl(BufferControl.class.getName());
        System.out.println(String.format(
                "!!! Buffer control length: %s, threshold enabled: %s, minimum threshold: %s"
                , control.getBufferLength(), control.getEnabledThreshold()
                , control.getMinimumThreshold()));
        control.setMinimumThreshold(60);
        control.setBufferLength(60);
        
//        session = Manager.createDataSink(
//                source, new MediaLocator("rtp://"+host+":"+port+"/audio/1"));
    }

    public void start() throws IOException
    {
        sendStream.start();
//        session.open();
//        session.start();
    }

    public void stop() throws IOException
    {
        try
        {
            source.close();
//            session.stop();
//            session.close();
        }
        finally
        {
            sendStream.close();
        }
    }
}
