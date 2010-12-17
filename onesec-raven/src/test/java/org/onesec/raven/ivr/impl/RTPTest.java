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

package org.onesec.raven.ivr.impl;

import com.cisco.jtapi.extensions.CiscoMediaCapability;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.sun.media.codec.audio.ulaw.Packetizer;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.PlugInManager;
import javax.media.Processor;
import javax.media.control.PacketSizeControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnFailedEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.codec.AlawAudioFormat;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.InputStreamSource;

/**
 *
 * @author Mikhail Titov
 */
public class RTPTest
{
    private Format outFormat;

    @Before
    public void prepare() throws Exception
    {
        if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC))
            System.out.println("ulaw rtp code removed from plugin");
        UlawPacketizer up = new UlawPacketizer();
        PlugInManager.addPlugIn(UlawPacketizer.class.getName()
                , up.getSupportedInputFormats(), up.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        AlawEncoder en = new AlawEncoder();
        PlugInManager.addPlugIn(AlawEncoder.class.getName()
                , en.getSupportedInputFormats()
				, en.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        AlawPacketizer p = new AlawPacketizer();
        showControls("codec", p.getControls());
        System.out.println("Supported input formats: "+p.getSupportedInputFormats()[0].toString());
        System.out.println("Supported output formats: "+p.getSupportedOutputFormats(null)[0].toString());
        PlugInManager.addPlugIn(AlawPacketizer.class.getName()
                , p.getSupportedInputFormats()
				, p.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);

        RTPManager tempManager = RTPManager.newInstance();
        outFormat = p.getSupportedOutputFormats(null)[0];
        tempManager.addFormat(outFormat, 8);
    }

//    @Test
    public void test() throws Exception
    {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);

        Processor processor = Manager.createProcessor(dataSource);
        //        processor.addControllerListener(this);
        processor.configure();
        waitForState(processor, Processor.Configured);
        Format format = new AudioFormat(AudioFormat.ALAW, 8000d, 8, 1);
        TrackControl[] tracks = processor.getTrackControls();
        tracks[0].setFormat(format);
        processor.realize();
        waitForState(processor, Processor.Realized);

        //        Player player = Manager.createPlayer(dataSource);
        PushBufferDataSource ds = (PushBufferDataSource) processor.getDataOutput();
        Player player = Manager.createPlayer(ds);
        player.start();
        processor.start();
        TimeUnit.SECONDS.sleep(5);
        System.out.println("Content type: "+ds.getContentType());
        System.out.println("Format: "+ds.getStreams()[0].getFormat());
        //        fail();
    }

//    @Test
    public void rtpTest() throws Exception
    {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);

        Processor processor = Manager.createProcessor(dataSource);
        //        processor.addControllerListener(this);
        processor.configure();
        waitForState(processor, Processor.Configured);
        Format format = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
//        Format format = new AudioFormat(BonusAudioFormatEncodings.ALAW_RTP, 8000d, 8, 1);
        TrackControl[] tracks = processor.getTrackControls();
        tracks[0].setFormat(format);
//        processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));
        processor.realize();
        waitForState(processor, Processor.Realized);

        //        Player player = Manager.createPlayer(dataSource);
        PushBufferDataSource ds = (PushBufferDataSource) processor.getDataOutput();
        DataSink session = Manager.createDataSink(
                ds, new MediaLocator("rtp://10.50.1.134:1234/audio"));
        session.open();
        session.start();
        ds.start();
        processor.start();
        
//        Player player = Manager.createPlayer(ds);
//        player.start();
//        processor.start();
        TimeUnit.SECONDS.sleep(10);
        System.out.println("Content type: "+ds.getContentType());
        System.out.println("Format: "+ds.getStreams()[0].getFormat());
        //        fail();
    }

//    @Test
    public void rtpTest3() throws Exception
    {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);

        Processor processor = Manager.createProcessor(dataSource);
        //        processor.addControllerListener(this);
        processor.configure();
        waitForState(processor, Processor.Configured);
//        Format format = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
//        Format format = new AudioFormat(Constants.ALAW_RTP, 8000d, 8, 1);
        Format format = new AudioFormat(AlawAudioFormat.ALAW_RTP, 8000d, 8, 1);
        TrackControl[] tracks = processor.getTrackControls();
        tracks[0].setFormat(format);
        processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));
        processor.realize();
        waitForState(processor, Processor.Realized);
        showControls("processor", processor.getControls());
        showControls("processor track", processor.getTrackControls());
        //        Player player = Manager.createPlayer(dataSource);
        PushBufferDataSource ds = (PushBufferDataSource) processor.getDataOutput();
        showControls("data source", ds.getControls());
        showControls("data source stream 0", ds.getStreams()[0].getControls());
        PacketSizeControl control = (PacketSizeControl) processor.getControl(PacketSizeControl.class.getName());
        if (control!=null){
            System.out.println("Found packet control");
            control.setPacketSize(240);
        }
        SessionAddress destAddress = new SessionAddress(InetAddress.getByName("localhost"), 1234);
        RTPManager rtpManager = RTPManager.newInstance();
        rtpManager.addFormat(format, 8);
        rtpManager.initialize(new SessionAddress());
        rtpManager.addTarget(destAddress);
        showControls("RTPManager", rtpManager.getControls());
        SendStream sendStream = rtpManager.createSendStream(ds, 0);
        sendStream.setBitRate(8000);
        System.out.println("Send stream: "+sendStream.getClass().getName());
//        sendStream.s
        sendStream.start();

        ds.start();
        processor.start();

        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    public void callTest() throws Exception
    {
        CListener listener = new CListener();
        JtapiPeer jtapiPeer = JtapiPeerFactory.getJtapiPeer(null);
        Provider provider = jtapiPeer.getProvider(
                "10.16.15.1;login=cti_user1;passwd=cti_user1;appinfo=cti port");
        provider.addObserver(listener);
        try
        {
            waitForProviderState(provider, Provider.IN_SERVICE);
            System.out.println("Provider in service");
            Address address = provider.getAddress("88014");
            if (address==null)
                throw new Exception("Address not found");
            System.out.println("Address: "+address.toString()+", class: "+address.getClass().getName());
            address.addObserver(listener);
            CiscoMediaTerminal terminal = (CiscoMediaTerminal) address.getTerminals()[0];
            if (terminal==null)
                throw new Exception("Terminal not found");
            System.out.println("Found terminal: "+terminal.toString());
            CiscoMediaCapability cap = new CiscoMediaCapability(2, 30);
//            cap.
//            CiscoMediaCapability[] caps = new CiscoMediaCapability[]{Codec.G711_MU_LAW.getCiscoMediaCapability()};
            terminal.register(InetAddress.getByName("10.50.1.134"), 1234, Codec.AUTO.getCiscoMediaCapabilities());
            terminal.addObserver(listener);
            address.addCallObserver(listener);

            TimeUnit.SECONDS.sleep(5);
            Call call = provider.createCall();
            call.connect(terminal, address, "88024");
//            call.connect(terminal, address, "09989128672947");
            TimeUnit.SECONDS.sleep(60);
        }
        finally
        {
            provider.removeObserver(listener);
            provider.shutdown();
        }
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


    private void showControls(String title, Object[] controls)
    {
        System.out.println("The CONTROLS of "+title);
        if (controls!=null)
            for (Object control: controls)
                System.out.println("    CONTROL: "+control.getClass().getName());
    }

    private static void waitForState(Processor p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(50);
            if (System.currentTimeMillis()-startTime>5000)
                throw new Exception("Processor state wait timeout");
        }
    }
    private class CListener implements
            ControllerListener, ProviderObserver, CiscoTerminalObserver, CallControlCallObserver,
            MediaCallObserver, AddressObserver
    {
//        private IvrEndpointConversationImpl conversation;
        private CallControlCall call;

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
                    try {
                        CiscoRTPOutputStartedEv rtpOutput = (CiscoRTPOutputStartedEv) event;
                        CiscoRTPOutputProperties props = rtpOutput.getRTPOutputProperties();
                        startRtp(props);
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
            for (CallEv event: events){
                switch (event.getID()){
                    case ConnFailedEv.ID: {
                        ConnFailedEv ev = (ConnFailedEv) event;
                        System.out.println("FAILED cause: "+ev.getCause());
                        break;}
                    case CallCtlConnFailedEv.ID:
                        CallCtlConnFailedEv ev = (CallCtlConnFailedEv) event;
                        System.out.println("CallCtl FAILED cause: "+event.getCause());
                }
                if (event instanceof CallCtlConnOfferedEv)
                {
                    CallCtlConnOfferedEv offEvent = (CallCtlConnOfferedEv)event;
                    CallControlConnection conn = (CallControlConnection) offEvent.getConnection();
                    try {
                        conn.accept();
                        call = (CallControlCall) event.getCall();
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
                else if (event instanceof CallCtlConnEstablishedEv)
                {
//                    if (conversation!=null)
//                        conversation.startConversation();
                }
                else if (event instanceof MediaTermConnDtmfEv)
                {
                    System.out.println("DTMF: "+((MediaTermConnDtmfEv)event).getDtmfDigit());
//                    conversation.continueConversation(((MediaTermConnDtmfEv)event).getDtmfDigit());
                }
            }

        }

        public void addressChangedEvent(AddrEv[] events)
        {
            System.out.println("Address events: "+eventsToString(events));
            for (AddrEv event: events){
                switch (event.getID()){
                    case ConnFailedEv.ID:
                    case CallCtlConnFailedEv.ID: System.out.println("FAILED: "+event.toString());
                }
            }
        }

        private String eventsToString(Object[] events)
        {
            StringBuilder buf = new StringBuilder();
            for (Object ev: events)
                buf.append(ev.toString()+", ");
            return buf.toString();
        }

        private void startRtp(CiscoRTPOutputProperties props) throws Exception
        {
            System.out.println("PAYLOAD: "+props.getPayloadType());
            Codec codec = Codec.getCodecByCiscoPayload(props.getPayloadType());
            System.out.println("USING codec: "+codec);
            InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
            IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);

            Processor processor = Manager.createProcessor(dataSource);
            //        processor.addControllerListener(this);
            processor.configure();
            waitForState(processor, Processor.Configured);
//            Format format = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
    //        Format format = new AudioFormat(Constants.ALAW_RTP, 8000d, 8, 1);
//            Format format = new AudioFormat(AlawAudioFormat.ALAW_RTP, 8000d, 8, 1);
            TrackControl[] tracks = processor.getTrackControls();
            Format format = codec.getAudioFormat();
            System.out.println("USING audio format: "+format);
            tracks[0].setFormat(format);
            processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));
            processor.realize();
            waitForState(processor, Processor.Realized);
            showControls("processor", processor.getControls());
            showControls("processor track", processor.getTrackControls());
            //        Player player = Manager.createPlayer(dataSource);
            PushBufferDataSource ds = (PushBufferDataSource) processor.getDataOutput();
            showControls("data source", ds.getControls());
            showControls("data source stream 0", ds.getStreams()[0].getControls());
            PacketSizeControl control = (PacketSizeControl) processor.getControl(PacketSizeControl.class.getName());
            if (control!=null){
                System.out.println("Found packet control. New packet size is "+props.getPacketSize());
                control.setPacketSize(props.getPacketSize()*8);
            }
            SessionAddress destAddress = new SessionAddress(props.getRemoteAddress(), props.getRemotePort());
            RTPManager rtpManager = RTPManager.newInstance();
            rtpManager.addFormat(format, 8);
            rtpManager.initialize(new SessionAddress());
            rtpManager.addTarget(destAddress);
            showControls("RTPManager", rtpManager.getControls());
            SendStream sendStream = rtpManager.createSendStream(ds, 0);
            sendStream.setBitRate(8000);
            System.out.println("Send stream: "+sendStream.getClass().getName());
    //        sendStream.s
            sendStream.start();

            ds.start();
            processor.start();
        }
    }

}
