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

import com.cisco.jtapi.extensions.CiscoMediaCapability;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Ignore;
import org.junit.Test;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class RTPSessionTest extends EasyMock
{
    @Test @Ignore
    public void test() throws Exception
    {
        Node owner = createMock(Node.class);
        ExecutorService executorService = createMock(ExecutorService.class);
        Logger logger = createMock(Logger.class);

        executorService.execute(executeTask());
        expectLastCall().times(2);
        expect(owner.isLogLevelEnabled(LogLevel.ERROR)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.isLogLevelEnabled(LogLevel.DEBUG)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        logger.error(logMessage(true), logMessage(false));
        expectLastCall().anyTimes();
        logger.debug(logMessage(false));
        expectLastCall().anyTimes();


        replay(owner, executorService, logger);

        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource dataSource =
                new ConcatDataSource(FileTypeDescriptor.WAVE, executorService, 160, owner);

        RTPSession session = new RTPSession("127.0.0.1", 1234, dataSource);
        session.start();
//        Player player = Manager.createPlayer(dataSource);
//        player.start();
//        dataSource.start();
        dataSource.addSource(source1);
        dataSource.addSource(source2);
        TimeUnit.SECONDS.sleep(10);
//        dataSource.reset();
        dataSource.addSource(source3);
        TimeUnit.SECONDS.sleep(5);
//        player.stop();
        session.stop();

        verify(owner, executorService, logger);

//        fail();
    }

    @Test
    public void testCall() throws Exception
    {
        Node owner = createMock(Node.class);
        ExecutorService executorService = createMock(ExecutorService.class);
        Logger logger = createMock(Logger.class);

        executorService.execute(executeTask());
        expectLastCall().anyTimes();
        expect(owner.isLogLevelEnabled(LogLevel.ERROR)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.isLogLevelEnabled(LogLevel.DEBUG)).andReturn(Boolean.TRUE).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        logger.error(logMessage(true), logMessage(false));
        expectLastCall().anyTimes();
        logger.debug(logMessage(false));
        expectLastCall().anyTimes();


        replay(owner, executorService, logger);

        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource dataSource =
                new ConcatDataSource(FileTypeDescriptor.WAVE, executorService, 160, owner);

        dataSource.addSource(source1);
//        dataSource.addSource(source2);

        CListener listener = new CListener(dataSource);
        JtapiPeer jtapiPeer = JtapiPeerFactory.getJtapiPeer(null);
        Provider provider = jtapiPeer.getProvider(
                "10.16.15.1;login=cti_user1;passwd=cti_user1;appinfo=cti port");
        provider.addObserver(listener);
        try
        {
            waitForProviderState(provider, Provider.IN_SERVICE);
            System.out.println("Provider in service");
            Address address = provider.getAddress("88013");
            if (address==null)
                throw new Exception("Address not found");
            System.out.println("Address: "+address.toString()+", class: "+address.getClass().getName());
            address.addObserver(listener);
            CiscoMediaTerminal terminal = (CiscoMediaTerminal) address.getTerminals()[0];
            if (terminal==null)
                throw new Exception("Terminal not found");
            System.out.println("Found terminal: "+terminal.toString());
//            CiscoMediaTerminal terminal = (CiscoMediaTerminal) provider.getTerminal("CTI_InfoPort");
//            if (terminal==null)
//                throw new Exception("Terminal not found");
//            System.out.println("Found terminal: "+terminal.toString());
            CiscoMediaCapability[] caps =
                    new CiscoMediaCapability[]{CiscoMediaCapability.G711_64K_30_MILLISECONDS};
            terminal.register(InetAddress.getByName("10.50.1.134"), 1234, caps);
            terminal.addObserver(listener);
//            Address address = terminal.getAddresses()[0];
//            if (address==null)
//                throw new Exception("Address not found");
//            System.out.println("Address: "+address.toString());
            address.addCallObserver(listener);

//            TimeUnit.SECONDS.sleep(2);
//            Call call = provider.createCall();
//            call.connect(terminal, address, "88024");
//            terminal.
            TimeUnit.SECONDS.sleep(20);
//            dataSource.addSource(source3);
            TimeUnit.SECONDS.sleep(5);

            System.out.println("   Press the Enter key to exit");
            System.in.read();

            verify(owner, executorService, logger);

        }
        finally
        {
            provider.removeObserver(listener);
            provider.shutdown();
        }
    }

    private static Task executeTask()
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(final Object argument)
            {
                new Thread(){

                    @Override
                    public void run()
                    {
                        ((Task)argument).run();
                    }

                }.start();
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });

        return null;
    }

    private static String logMessage(final boolean errorMessage)
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(Object argument)
            {
                System.out.println(argument.toString());
                return !errorMessage;
            }

            public void appendTo(StringBuffer buffer)
            {
                buffer.append("Catched error message. ");
            }
        });
        return null;
    }
    
    private static void waitForProviderState(Provider provider, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (provider.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(100);
            if (System.currentTimeMillis()-startTime>10000)
                throw new Exception("Provider state wait timeout");
        }
    }


    private static class CListener implements
            ControllerListener, ProviderObserver, CiscoTerminalObserver, CallControlCallObserver,
            MediaCallObserver, AddressObserver
    {
        private final ConcatDataSource dataSource;

        public CListener(ConcatDataSource dataSource)
        {
            this.dataSource = dataSource;
        }

        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("Controller event: "+event.toString());
        }

        public void providerChangedEvent(ProvEv[] events)
        {
            System.out.println("Provider events: "+eventsToString(events));
        }

        public void terminalChangedEvent(TermEv[] events)
        {
            System.out.println("Terminal events: "+eventsToString(events));
            for (TermEv event: events)
            {
                if (event instanceof CiscoRTPOutputStartedEv)
                {
                    CiscoRTPOutputStartedEv rtpOutput = (CiscoRTPOutputStartedEv) event;
                    CiscoRTPOutputProperties props = rtpOutput.getRTPOutputProperties();
                    String url = String.format(
                            "rtp://%s:%s/audio/1"
                            , props.getRemoteAddress().getHostAddress(), props.getRemotePort());
                    System.out.println("streaming audio to url: "+url);
                    System.out.println("Packet size: "+props.getPacketSize());
                    System.out.println("Bitrate: "+props.getBitRate());
                    System.out.println("Max frames per packet size: "+props.getMaxFramesPerPacket());
                    try {
//                        new PlayAudio(props.getRemoteAddress(), props.getRemotePort());
//                        new PlayAudio(InetAddress.getLocalHost(), 2222);
//                        new PlayAudio(url);
                        RTPSession session = new RTPSession(
                                props.getRemoteAddress().getHostAddress(), props.getRemotePort()
                                , dataSource);
                        session.start();
//                        new PlayAudio("rtp://localhost:2222/audio");
                    } catch (Exception ex) {
                        System.out.println("Error streaming");
                        ex.printStackTrace();
                    }
                }
                if (event instanceof CiscoRTPInputStartedEv)
                {
                    CiscoRTPInputStartedEv rtpInput = (CiscoRTPInputStartedEv) event;
                    System.out.println("streaming audio to url: "+rtpInput.getCiscoRTPHandle());

//                    rtpInput.getRTPInputProperties().
                }
            }
        }

        public void callChangedEvent(CallEv[] events)
        {
            System.out.println("Address call events: "+eventsToString(events));
            for (CallEv event: events)
                if (event instanceof CallCtlConnOfferedEv)
                {
                    CallCtlConnOfferedEv offEvent = (CallCtlConnOfferedEv)event;
                    CallControlConnection conn = (CallControlConnection) offEvent.getConnection();
                    try {
                        conn.accept();
                        System.out.println("Connection accepted");
                    } catch (Exception ex) {
                        System.out.println("Error accepting call");
                        ex.printStackTrace();
                    }
                }
                else if (event instanceof TermConnRingingEv)
                {
                    try{
                        ((TermConnRingingEv)event).getTerminalConnection().answer();
                    }catch (Exception e)
                    {
                        System.out.println("Error answer on call");
                        e.printStackTrace();
                    }
                }
                else if (event instanceof MediaTermConnDtmfEv)
                    System.out.println("DTMF: "+((MediaTermConnDtmfEv)event).getDtmfDigit());
        }

        public void addressChangedEvent(AddrEv[] events)
        {
            System.out.println("Address events: "+eventsToString(events));
        }

        private String eventsToString(Object[] events)
        {
            StringBuilder buf = new StringBuilder();
            for (Object ev: events)
                buf.append(ev.toString()+", ");
            return buf.toString();
        }
    }

}