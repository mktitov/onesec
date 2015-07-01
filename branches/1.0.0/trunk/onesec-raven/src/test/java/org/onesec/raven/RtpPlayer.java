/*
 *  Copyright 2010 Mikhail Titov.
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

package org.onesec.raven;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import javax.media.Controller;
import javax.media.Manager;
import javax.media.Player;
import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import org.onesec.raven.ivr.RTPManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class RtpPlayer implements ReceiveStreamListener
{
    @Service
    private static RTPManagerService rtpManagerService;

    private final static Logger logger = LoggerFactory.getLogger(RtpPlayer.class);
    private DataSource source;
    private String remoteHost;
    private Processor processor;
    private RTPManager rtpManager;
    private SessionAddress destAddress;

    public RtpPlayer(String remoteHost) throws Exception
    {
        logger.info("Creating new RTP player");
        this.remoteHost = remoteHost;
    }

    public void start() throws Exception
    {
        logger.info("RTP player. Starting listening the rtp stream from ({})", remoteHost);
        rtpManager = rtpManagerService.createRtpManager();
        rtpManager.addReceiveStreamListener(this);
        rtpManager.initialize(new SessionAddress(InetAddress.getByName(remoteHost), 1234));
        InetAddress dest = InetAddress.getByName(remoteHost);
        destAddress = new SessionAddress(dest, SessionAddress.ANY_PORT);
        rtpManager.addTarget(destAddress);
    }

    public void update(ReceiveStreamEvent event)
    {
        logger.info("RTP Player. Received new event ({})", event.getClass().getName());
        if (event instanceof NewReceiveStreamEvent)
        {
            ReceiveStream stream = event.getReceiveStream();
            logger.info("RTP Player. Received new stream");

            source = stream.getDataSource();

            RTPControl ctl = (RTPControl)source.getControl("javax.media.rtp.RTPControl");
            if (ctl!=null)
                logger.info("RTP Player. The format of the stream: "+ctl.getFormat());

            new Thread(){
                @Override
                public void run(){
                    try{
                        Processor p = Manager.createProcessor(source);
                        p.configure();
                        waitForState(p, Processor.Configured);
                        p.getTrackControls()[0].setFormat(new AudioFormat(
                                AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED));
                        p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
                        p.realize();
                        waitForState(p, Processor.Realized);
                        p.start();
                        Player player = Manager.createPlayer(p.getDataOutput());
                        waitForState(player, Player.Realized);
                        player.start();
                    }
                    catch(Exception e){
                        logger.error("Error converting RTP to LINEAR format", e);
                    }
                }
            }.start();
            
        }
    }

    private static void waitForState(Controller p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(5);
            if (System.currentTimeMillis()-startTime>1000)
                throw new Exception("Processor state wait timeout");
        }
    }

}
