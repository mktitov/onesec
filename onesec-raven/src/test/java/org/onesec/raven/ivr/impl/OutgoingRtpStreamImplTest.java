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
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.SessionManager;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;
import javax.media.rtp.rtcp.SourceDescription;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImplTest extends OnesecRavenTestCase implements ReceiveStreamListener, SessionListener
{
    private RtpStreamManagerNode manager;
    private ExecutorServiceNode executor;
    private AudioStream audioStream;
    private InetAddress localAddress;

    @Before
    public void prepare() throws Exception
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtp manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setMaxStreamCount(10);

        localAddress  = getInterfaceAddress();
        RtpAddressNode address1 = createAddress(localAddress.getHostAddress(), 3000);
        assertTrue(manager.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

    }

    @Test
    public void test() throws Exception
    {
        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource audioSource =
                new ConcatDataSource(FileTypeDescriptor.WAVE, executor, 160, manager);

        OutgoingRtpStream sendStream = manager.getOutgoingRtpStream(manager);
        Player player = Manager.createPlayer(new MediaLocator("rtp://"+localAddress.getHostAddress()+":1234/audio/1"));
        Thread.sleep(2000);
        try
        {
            assertEquals(new Integer(1), manager.getStreamsCount());
            sendStream.open(localAddress.getHostAddress(), 1234, audioSource);
            player.start();
            sendStream.start();
            audioSource.addSource(source1);
            Thread.sleep(2000);
            audioSource.addSource(source2);
            Thread.sleep(2000);
            audioSource.addSource(source3);
            Thread.sleep(3000);
        }
        finally
        {
            sendStream.release();
            player.stop();
        }
        assertEquals(new Integer(0), manager.getStreamsCount());
    }

    private RtpAddressNode createAddress(String ip, int startingPort)
    {
        RtpAddressNode addr = new RtpAddressNode();
        addr.setName(ip);
        manager.addAndSaveChildren(addr);
        addr.setStartingPort(startingPort);
        assertTrue(addr.start());

        return addr;
    }
    private void createRtpSessionManager(int port, int port2) throws Exception
    {
        RTPSessionMgr manager = new RTPSessionMgr();
//        SessionAddress addr = new SessionAddress(iaddr, 1234);
        SessionAddress addr = new SessionAddress();
        manager.addReceiveStreamListener(this);
        manager.addSessionListener(this);
        manager.initSession(addr, getSDES(manager), .05, .25);
        manager.startSession(new SessionAddress(localAddress, 1234, localAddress, 1235), 1, null);

//        RTPManager manager = RTPManager.newInstance();
//        manager.

    }

    private SourceDescription[] getSDES(SessionManager mgr) throws Exception
    {
        SourceDescription[] desclist = new  SourceDescription[3];
        String cname = mgr.generateCNAME();

        desclist[0] = new
                    SourceDescription(SourceDescription.SOURCE_DESC_NAME,
                                      System.getProperty("user.name"),
                                      1,
                                      false);
        desclist[1] = new
                    SourceDescription(SourceDescription.SOURCE_DESC_CNAME,
                                      cname,
                                      1,
                                      false);
        desclist[2] = new
                    SourceDescription(SourceDescription.SOURCE_DESC_TOOL,
                                      "AVReceive powered by JMF",
                                      1,
                                      false);
        return desclist;
    }
    public void update(ReceiveStreamEvent event)
    {
        logEventInfo("RECEIVE STREAM EVENT: ", event);
        if (event instanceof StreamMappedEvent)
        {
            try {
                Player player = Manager.createPlayer(event.getReceiveStream().getDataSource());
                player.start();
            } catch (IOException ex) {
                Logger.getLogger(OutgoingRtpStreamImplTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoPlayerException ex) {
                Logger.getLogger(OutgoingRtpStreamImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void update(SessionEvent event)
    {
        logEventInfo("SESSION EVENT: ", event);
    }

    private void logEventInfo(String prefix, Object event)
    {
        System.out.println("@@@"+prefix+". class: "+event.getClass().getName()+"; event: "+event.toString());
    }

    private void createSessionMagager()
    {
    }

}