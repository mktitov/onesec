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
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationImplTest extends OnesecRavenTestCase
{
    private RtpStreamManagerNode manager;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private ContainerNode conversationOwner;

    @Before
    public void prepare() throws Exception
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtpManager");
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());

        RtpAddressNode address = new RtpAddressNode();
        address.setName(getInterfaceAddress().getHostAddress());
        manager.addAndSaveChildren(address);
        address.setStartingPort(18384);
        assertTrue(address.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(20);
        assertTrue(executor.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        conversationOwner = new ContainerNode("conversation owner");
        tree.getRootNode().addAndSaveChildren(conversationOwner);
        conversationOwner.setLogLevel(LogLevel.TRACE);
        assertTrue(conversationOwner.start());
//        Thread.currentThread().
    }

    @Test
    public void test() throws Exception
    {
        AudioFileNode audioNode = createAudioFileNode("audio1", "src/test/wav/test.wav");
        createPauseActionNode(scenario, 2000l);
        createPlayAudioActionNode("play", scenario, audioNode);
        createStopConversationNode("stop", scenario);

        makeACall("88024");
    }

    private void makeACall(String number) throws Exception
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
            Address address = provider.getAddress("88037");
            if (address==null)
                throw new Exception("Address not found");
            System.out.println("Address: "+address.toString()+", class: "+address.getClass().getName());
            address.addObserver(listener);
//            CiscoMediaTerminal terminal = (CiscoMediaTerminal) address.getTerminals()[0];
            CiscoRouteTerminal terminal = (CiscoRouteTerminal) address.getTerminals()[0];
            if (terminal==null)
                throw new Exception("Terminal not found");
            System.out.println("Found terminal: "+terminal.toString());
//            CiscoMediaTerminal terminal = (CiscoMediaTerminal) provider.getTerminal("CTI_InfoPort");
//            if (terminal==null)
//                throw new Exception("Terminal not found");
//            System.out.println("Found terminal: "+terminal.toString());
            CiscoMediaCapability[] caps =
                    new CiscoMediaCapability[]{CiscoMediaCapability.G711_64K_30_MILLISECONDS};
//            terminal.register(InetAddress.getByName("10.50.1.134"), 1234, caps);
//            terminal.register(caps);
            terminal.register(caps, CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
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
//            TimeUnit.SECONDS.sleep(20);
//            dataSource.addSource(source3);
            TimeUnit.SECONDS.sleep(30);

//            System.out.println("   Press the Enter key to exit");
//            System.in.read();
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

    private void createStopConversationNode(String nodeName, Node scenario)
    {
        StopConversationActionNode stopNode = new StopConversationActionNode();
        stopNode.setName(nodeName);
        scenario.addAndSaveChildren(stopNode);
        assertTrue(stopNode.start());
    }
    
    private AudioFileNode createAudioFileNode(String nodeName, String filename) throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName(nodeName);
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream(filename);
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());
        return audioFileNode;
    }

    private PlayAudioActionNode createPlayAudioActionNode(
            String name, Node owner, AudioFileNode audioFileNode)
    {
        PlayAudioActionNode playAudioActionNode = new PlayAudioActionNode();
        playAudioActionNode.setName("Play audio");
        owner.addAndSaveChildren(playAudioActionNode);
        playAudioActionNode.setAudioFile(audioFileNode);
        assertTrue(playAudioActionNode.start());
        return playAudioActionNode;
    }

    private PauseActionNode createPauseActionNode(Node owner, Long interval)
    {
        PauseActionNode pauseAction = new PauseActionNode();
        pauseAction.setName("pause");
        owner.addAndSaveChildren(pauseAction);
        pauseAction.setInterval(interval);
        assertTrue(pauseAction.start());
        return pauseAction;
    }

    private IfNode createIfNode(String name, Node owner, String expression) throws Exception
    {
        IfNode ifNode = new IfNode();
        ifNode.setName(name);
        owner.addAndSaveChildren(ifNode);
        ifNode.setUsedInTemplate(Boolean.FALSE);
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue(expression);
        assertTrue(ifNode.start());
        return ifNode;
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 4000);
        assertFalse(res.isWaitInterrupted());
    }


    private class CListener implements
            ControllerListener, ProviderObserver, CiscoTerminalObserver, CallControlCallObserver,
            MediaCallObserver, AddressObserver
    {
        private IvrEndpointConversationImpl conversation;
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
                        if (conversation==null){
                            conversation = new IvrEndpointConversationImpl(
                                    conversationOwner, executor, scenario
                                    , manager, true, null);
                            //TODO: Восстановить работу теста
//                            conversation.init(
//                                    call, props.getRemoteAddress().getHostAddress(), props.getRemotePort()
//                                    , props.getPacketSize()*8, 5, 0, Codec.G711_MU_LAW);
                        }

//                        conversation.startConversation();

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
                    conversation.continueConversation(((MediaTermConnDtmfEv)event).getDtmfDigit());
                }
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