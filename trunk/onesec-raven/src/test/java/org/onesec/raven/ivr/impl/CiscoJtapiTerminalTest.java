/*
 *  Copyright 2011 Mikhail Titov.
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

import com.cisco.jtapi.extensions.CiscoCall;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.actions.PauseActionNode;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.easymock.IArgumentMatcher;
import org.raven.tree.Node;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.junit.Before;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.StateToNodeLogger;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrMediaTerminal;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.SendMessageDirection;

/**
 * @author Mikhail Titov
 */
public class CiscoJtapiTerminalTest extends OnesecRavenTestCase {
//    private final static String TEST_NUMBER = "631798";
    private final static String TEST_NUMBER = "68000"; //������� �� 88137
    private final static String RP_TEST_NUMBER = "631690";
    
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private RtpStreamManagerNode manager;
    private CiscoJtapiTerminal endpoint;
    private CiscoJtapiTerminal endpoint2;
    private ContainerNode termNode;
    private static AtomicBoolean convStopped = new AtomicBoolean();
    private IvrEndpointConversation conv;

    @Before
    public void prepare() throws Exception {
        convStopped.set(false);
        
        manager = new RtpStreamManagerNode();
        manager.setName("rtpManager");
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());

        RtpAddressNode address = new RtpAddressNode();
        address.setName(getInterfaceAddress().getHostAddress());
        manager.addAndSaveChildren(address);
        address.setStartingPort(18384);
        assertTrue(address.start());

        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("Provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(68000);
        provider.setToNumber(68009);
//        provider.setFromNumber(68000);
//        provider.setToNumber(68009);
        provider.setHost(privateProperties.getProperty("ccm_addr"));
        provider.setPassword(privateProperties.getProperty("ccm_pwd"));
        provider.setUser(privateProperties.getProperty("ccm_user"));
        assertTrue(provider.start());
        
//        provider = new ProviderNode();
//        provider.setName("88013 provider");
//        callOperator.getProvidersNode().addAndSaveChildren(provider);
//        provider.setFromNumber(88013);
//        provider.setToNumber(88049);
////        provider.setFromNumber(68050);
////        provider.setToNumber(68050);
////        provider.setHost("10.16.15.1");
//        provider.setHost("10.0.137.125");
////        provider.setPassword("cti_user1");
////        provider.setUser("cti_user1");
//        provider.setPassword(privateProperties.getProperty("ccm_dialer_proxy_kom"));
//        provider.setUser("ccm_dialer_proxy_kom");
//        assertTrue(provider.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(30);
        executor.setCorePoolSize(30);
        assertTrue(executor.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        termNode = new ContainerNode("term node");
        tree.getRootNode().addAndSaveChildren(termNode);
        termNode.setLogLevel(LogLevel.TRACE);
        assertTrue(termNode.start());

        providerRegistry = registry.getService(ProviderRegistry.class);
        stateListenersCoordinator = registry.getService(StateListenersCoordinator.class);
        StateToNodeLogger stateLogger = registry.getService(StateToNodeLogger.class);
        stateLogger.setLoggerNode(termNode);
    }

//    @Test
    public void startStopTest() throws Exception {
        waitForProvider();
        IvrMediaTerminal term = trainTerminal("88049", scenario, true, true);
        replay(term);
        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        IvrTerminalState state = endpoint.getState();
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        endpoint.start();
        state.waitForState(new int[]{IvrTerminalState.IN_SERVICE}, 5000);
        assertEquals(IvrTerminalState.IN_SERVICE, state.getId());
        Thread.sleep(100);
        endpoint.stop();
        state.waitForState(new int[]{IvrTerminalState.OUT_OF_SERVICE}, 5000);
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        verify(term);
    }

    //� ������ ����� ���������� ������ ��������� �� �����, ��������� � �����. ������ ��������:
    //  ������ �� ���������
    @Test(timeout=50000)
    public void incomingCallTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint.addConversationListener(listener);
        startEndpoint(endpoint);
        waitForConversationStop();

        stopEndpoint(endpoint);
        verify(term, listener);
    }

    //� ������ ����� ������� ��������, �� ��������� �����. ���������� ����� ������. ������ ��������:
    //  ������ �� ���������
//    @Test(timeout=50000)
    public void inviteTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal("631799", scenario, true, true);
//        IvrMediaTerminal term = trainTerminal("631751", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();
        stopEndpoint(endpoint);
        
        verify(term, listener);
    }
    
    //� ������ ����� ������� ��������, �� ��������� �����. ���������� ����� ������. 
    //� ������� ������� ����� ������ ���������������� �� 88028
//    @Test(timeout=70000)
    public void transferTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = createTransferListener("088002500990*1");
//        IvrEndpointConversationListener listener = createTransferListener("88028*19");
        replay(term);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88024*88027", 0, 0, listener, scenario, null, null);
//        endpoint.invite("088002500990*1", 0, 0, listener, scenario, null, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        stopEndpoint(endpoint);
        
        verify(term);
    }
    
    //� ������ �����
    //1. ���������� ��������� �� ����� RP ��������� � �����. 
    //2. RP ������� ������ 
    //3. � ������� ������� ����� ������ ���������������� �� 88028
//    @Test(timeout=70000)
    public void transferFromRoutePointToTerminalTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal("631798", scenario, true, true);
        IvrEndpointConversationListener listener = createTransferListener2("631799");
        IvrMediaTerminal term2 = trainTerminal("631799", scenario, true, true);
        IvrEndpointConversationListener listener2 = createLogCallListener();
        replay(term, term2);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint.addConversationListener(listener);
        startEndpoint(endpoint);
        endpoint2 = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term2, null);
        endpoint2.addConversationListener(listener2);
        startEndpoint(endpoint2);
        Thread.sleep(30000);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        stopEndpoint(endpoint);
        
        verify(term, term2);
    }
    
    //� ������ �����
    //1. ���������� ��������� �� ����� RP ��������� � �����. 
    //2. RP ������� ������ 
    //3. � ������� ������� ����� ������ ���������������� �� 88028
//    @Test(timeout=70000)
    public void transferFromRoutePointTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal("631690", scenario, true, true);
        IvrEndpointConversationListener listener = createTransferListener2("88028");
        replay(term);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint.addConversationListener(listener);
        startEndpoint(endpoint);
//        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        stopEndpoint(endpoint);
        
        verify(term);
    }
    
    //� ������ ����� ������� 
    //1. �������� �� ����� (88024). ���������� ����� ������. 
    //2. � ������� ������� ����� ����������� �� 631730
    //3. �������� �� ����� (88028). ���������� ����� ������. 
    //4. � ������� ������� ����� ������ ��������������, �.�. � ��������� ��������� 88024 >-< 88028
//    @Test(timeout=70000)
    public void parkUnparkToDirectNumberTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal("631799", scenario, true, true);
        IvrMediaTerminal term2 = trainTerminal("631798", scenario, true, true);
        IvrEndpointConversationListener listener = createTransferListener("631730");
        IvrEndpointConversationListener listener2 = createTransferListener("9998631730");
        replay(term, term2);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint2 = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term2, null);
        startEndpoint(endpoint);
        startEndpoint(endpoint2);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        
        convStopped.set(false);
        endpoint2.invite("88028", 0, 0, listener2, scenario, null, null);
        waitForConversationStop();
        Thread.sleep(1000);
        assertEquals(0, endpoint2.getActiveCallsCount());
        stopEndpoint(endpoint);
        stopEndpoint(endpoint2);
        
        verify(term, term2);
        Thread.sleep(5000);
    }
    
    //� ������ ����� ������� 
    //1. �������� �� ����� (88024). ���������� ����� ������. 
    //2. � ������� ������� ����� ����������� ��� ������ IvrEndpointConversation.park()
    //3. �������� �� ����� (88028). ���������� ����� ������. 
    //4. � ������� ������� ����� ������ ��������������, �.�. � ��������� ��������� 88024 >-< 88028
//    @Test(timeout=70000)
    public void parkUnparkTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal("631798", scenario, true, true);
        IvrMediaTerminal term2 = trainTerminal("631799", scenario, true, true);
        final AtomicReference<String> parkNumber = new AtomicReference<String>();
        IvrEndpointConversationListener listener = createParkListener(parkNumber);
        IvrEndpointConversationListener listener2 = createUnParkListener(parkNumber);
        replay(term, term2);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint2 = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term2, null);
        startEndpoint(endpoint);
        startEndpoint(endpoint2);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        
        convStopped.set(false);
        endpoint2.invite("88028", 0, 0, listener2, scenario, null, null);
        waitForConversationStop();
        Thread.sleep(1000);
        assertEquals(0, endpoint2.getActiveCallsCount());
        stopEndpoint(endpoint);
        stopEndpoint(endpoint2);
        
        verify(term, term2);
        Thread.sleep(5000);
    }
    
    //� ������ ����� ������� 
    //1. �������� �� ����� (88024). ���������� ����� ������. 
    //2. � ������� ������� ����� ����������� ��� ������ IvrEndpointConversation.park()
    //3. �������� �� ����� (88028). ���������� ����� ������. 
    //4. � ������� ������� ����� ������ ��������������, �.�. � ��������� ��������� 88024 >-< 88028
//    @Test(timeout=70000)
    public void parkUnparkFromRoutePointTest() throws Exception {
        waitForProvider();
        createSimpleScenarioWithPause();

        IvrMediaTerminal term = trainTerminal("631798", scenario, true, true);
        IvrMediaTerminal term2 = trainTerminal("88037", scenario, true, true);
        final AtomicReference<String> parkNumber = new AtomicReference<String>();
        IvrEndpointConversationListener listener = createParkListener(parkNumber);
        IvrEndpointConversationListener listener2 = createUnParkListener(parkNumber);
//        IvrEndpointConversationListener listener2 = createTransferListener3(parkNumber);
        replay(term, term2);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        endpoint2 = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term2, null);
        startEndpoint(endpoint);
        startEndpoint(endpoint2);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getActiveCallsCount());
        
        convStopped.set(false);
        endpoint2.invite("88028", 0, 0, listener2, scenario, null, null);
        waitForConversationStop();
        Thread.sleep(1000);
        assertEquals(0, endpoint2.getActiveCallsCount());
        stopEndpoint(endpoint);
        stopEndpoint(endpoint2);
        
        verify(term, term2);
        Thread.sleep(5000);
    }
    
    
//    @Test(timeout=50000)
    public void inviteToInvalidAddressTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal("631799", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener_invalidAddress();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("08502544955", 0, 0, listener, scenario, null, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();        
        stopEndpoint(endpoint);
        Thread.sleep(100);
        verify(term, listener);
    }

    //� ������ ����� ������� �������� �� ��������� ����� �� ������ ����� �� ����
//    @Test(timeout=50000)
    public void inviteTimeoutTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal("631799", scenario, true, true);
//        IvrTerminal term = trainTerminal("631799", scenario, true, true);
        IvrEndpointConversationListener listener = trainInviteTimeoutListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88027", 5, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //� ������ ����� ������� �������� �� ��������� ����� � ����� ����� ������. ������ ��������:
    //  ������ �� ��������� -> ����� 5 ��� -> ������ �� ���������
//    @Test(timeout=50000)
    public void inviteTimeoutTest2() throws Exception {
        waitForProvider();
        createSimpleScenario2(5000);

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88027", 5, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //� ������ ����� ������� �������� �� ��������� ����� � ����� ����� ������ � ������� 5 ���!.
    //�������� ������ �� �����, ������� ����� ������� ������
//    @Test(timeout=50000)
    public void inviteTimeoutTest3() throws Exception {
        waitForProvider();
        createSimpleScenario3();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88027", 8, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        Thread.sleep(9000);
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    //������ ����� ���������, ��� �� ��������� INVITE TIMEOUT ��� ���������� ������ �� ���������
    //� ������ ����� ������� �������� �� ��������� ����� � ����� ����� ������ � ������� 5 ���!.
    //����� ���������� ��������� ����� �� ���������. 
    //����� 10 ��� ���������� ����� ����� � ���������
    //������ ��������: ������ �� ���������
//    @Test(timeout=70000)
    public void inviteTimeoutTest4() throws Exception {
        waitForProvider();
        createSimpleScenario4();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener2();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88027", 8, 0, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
//        Thread.sleep(9000);
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    
    //� ������ ����� ������� �������� �� ��������� ����� � ����� ����� ������. 
    //�������� �������������� ���������:
    //  ������ �� ��������� -> ����� 5 ���  -> ������ �� ���������
    //� ������ ��������:
    //  ������ �� ��������� -> ����� ~5 ��� 
//    @Test(timeout=50000)
    public void maxCallDurationTest() throws Exception {
        waitForProvider();
        createSimpleScenario2(5000);

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88027", 0, 5, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //� ������ ����� ������� �������� �� ��������� ����� � ����� ����� ������ � ������� 5 ���. 
    //� ����� ������������ � INVITE_TIMEOUT � MAX_CONVERSATION_DUR, �� ������� ������ "����������"
    //�� MAX_CONVERSATION_DUR
    //�������� �������������� ���������:
    //  ������ �� ��������� -> ����� 10 ���  -> ������ �� ���������
    //� ������ ��������:
    //  ������ �� ��������� -> ����� ~10 ��� 
//    @Test(timeout=50000)
    public void maxCallDurationTest2() throws Exception {
        waitForProvider();
        createSimpleScenario2(10000);

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88024", 5, 10, listener, scenario, null, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    
    //���� ����� ��������� �������� 
    //  ������ �� ���������
//    @Test
    public void longCallTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
//        endpoint.invite("089128672947", 0, 0, listener, scenario, null);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
        waitForConversationStop();
        stopEndpoint(endpoint);
        
        verify(term, listener);
    }
    
    //� ������ ����� ������� ��������, �� ��������� �����. ���������� ����� ������. ������ ��������:
    //  ������ �� ��������� + DTMF signals
//    @Test(timeout=70000)
    public void sendDtmfTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListenerForDtmfSending();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
//        endpoint.invite("88024", 0, 0, listener, scenario, null);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
        waitForConversationStop();
        stopEndpoint(endpoint);
        
        verify(term, listener);
    }

    
    //� ������ ����� ������� ��������, �� ��������� �����. ���������� ����� ������. ������ ��������:
    //  ������ �� ��������� + �� ������ ����������� ��������� "������ �.�."
//    @Test(timeout=70000)
    public void sendMessageTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrMediaTerminal term = trainTerminal(TEST_NUMBER, scenario, true, true);
        IvrEndpointConversationListener listener = trainListenerForSendMessage();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term, null);
        startEndpoint(endpoint);
        endpoint.invite("88024", 0, 0, listener, scenario, null, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();
        stopEndpoint(endpoint);
        
        verify(term, listener);
        
    }

    private void startEndpoint(CiscoJtapiTerminal endpoint) throws Exception {
        IvrTerminalState state = endpoint.getState();
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        endpoint.start();
        state.waitForState(new int[]{IvrTerminalState.IN_SERVICE}, 5000);
        assertEquals(IvrTerminalState.IN_SERVICE, state.getId());
        Thread.sleep(100);
    }

    private void stopEndpoint(CiscoJtapiTerminal endpoint) throws Exception {
        endpoint.stop();
        IvrTerminalState state = endpoint.getState();
        state.waitForState(new int[]{IvrTerminalState.OUT_OF_SERVICE}, 5000);
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        assertEquals(0, endpoint.getCallsCount());
        assertEquals(0, endpoint.getConnectionsCount());
    }

    private void waitForConversationStop() throws InterruptedException {
        while (!convStopped.get()) 
            TimeUnit.MILLISECONDS.sleep(10);
    }

    private void createSimpleScenario() throws Exception {
        AudioFileNode audio = createAudioFileNode("audio", "src/test/wav/test.wav");
        createPlayAudioActionNode("play audio", scenario, audio);
        createStopConversationAction(scenario);
    }

    private void createSimpleScenarioWithPause() throws Exception {
//        AudioFileNode audio = createAudioFileNode("audio", "src/test/wav/test.wav");
//        createPlayAudioActionNode("play audio", scenario, audio);
        createPauseActionNode(scenario, 25000l);
        createStopConversationAction(scenario);
    }

    private void createSimpleScenario2(long pause) throws Exception {
        AudioFileNode audio = createAudioFileNode("audio", "src/test/wav/test.wav");
        createPlayAudioActionNode("play audio", scenario, audio);
        createPauseActionNode(scenario, pause);
        createPlayAudioActionNode("play audio 2", scenario, audio);
        createStopConversationAction(scenario);
    }

    private void createSimpleScenario3() throws Exception {
        createStopConversationAction(scenario);
    }
    
    private void createSimpleScenario4() throws Exception {
        AudioFileNode audio = createAudioFileNode("audio", "src/test/wav/test.wav");
        createPlayAudioActionNode("play audio", scenario, audio);
        createPauseActionNode(scenario, 5000l);
        createStopConversationAction(scenario);
    }


    private IvrEndpointConversationListener trainListener() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.conversationStarted(isA(IvrEndpointConversationEvent.class));
        listener.conversationStopped(handleConversationStopped());
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        listener.incomingRtpStarted(isA(IvrIncomingRtpStartedEvent.class));
        listener.outgoingRtpStarted(isA(IvrOutgoingRtpStartedEvent.class));
        return listener;
    }

    private IvrEndpointConversationListener trainListener_invalidAddress() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        listener.conversationStopped(handleConversationStopped());
        return listener;
    }

    private IvrEndpointConversationListener trainListenerForDtmfSending() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.conversationStarted(sendDTMF("123*"));
        listener.conversationStopped(handleConversationStopped());
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        listener.incomingRtpStarted(isA(IvrIncomingRtpStartedEvent.class));
        listener.outgoingRtpStarted(isA(IvrOutgoingRtpStartedEvent.class));
        return listener;
    }
    
    private IvrEndpointConversationListener trainListenerForSendMessage() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.conversationStarted(sendMessage("������ �.�."));
        listener.conversationStopped(handleConversationStopped());
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        listener.incomingRtpStarted(isA(IvrIncomingRtpStartedEvent.class));
        listener.outgoingRtpStarted(isA(IvrOutgoingRtpStartedEvent.class));
        return listener;
    }

    private IvrEndpointConversationListener trainListener2() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.conversationStarted(isA(IvrEndpointConversationEvent.class));
        expectLastCall().times(2);
        listener.conversationStopped(handleConversationStopped());
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        listener.incomingRtpStarted(isA(IvrIncomingRtpStartedEvent.class));
        expectLastCall().times(2);
        listener.outgoingRtpStarted(isA(IvrOutgoingRtpStartedEvent.class));
        expectLastCall().times(2);
        return listener;
    }

    private IvrEndpointConversationListener trainInviteTimeoutListener() {
        IvrEndpointConversationListener listener = createMock(IvrEndpointConversationListener.class);
        listener.conversationStopped(handleConversationStopped());
        listener.listenerAdded(isA(IvrEndpointConversationEvent.class));
        return listener;
    }

    private TestTerminal trainTerminal(String address, IvrConversationScenario scenario, 
            boolean enableInCalls, boolean enableInRtp)
    {
        return trainTerminal(address, scenario, enableInCalls, enableInRtp, false);
    }
    
    private TestTerminal trainTerminal(String address, IvrConversationScenario scenario, 
            boolean enableInCalls, boolean enableInRtp, boolean sharePorts)
    {
        TestTerminal term = createMock((TestTerminal.class));
        expect(term.getLogger()).andReturn(termNode).anyTimes();
        expect(term.isLogLevelEnabled(isA(LogLevel.class))).andReturn(true).anyTimes();
        expect(term.getObjectName()).andReturn(termNode.getName()).anyTimes();
        expect(term.getObjectDescription()).andReturn("Terminal").anyTimes();
        expect(term.getAddress()).andReturn(address);
        expect(term.getCodec()).andReturn(Codec.AUTO);
        expect(term.getConversationScenario()).andReturn(scenario).anyTimes();
        expect(term.getEnableIncomingCalls()).andReturn(enableInCalls);
        expect(term.getEnableIncomingRtp()).andReturn(enableInRtp);
        expect(term.getExecutor()).andReturn(executor);
        expect(term.getRtpMaxSendAheadPacketsCount()).andReturn(0);
        expect(term.getRtpPacketSize()).andReturn(null);
        expect(term.getRtpStreamManager()).andReturn(manager);
        expect(term.getPath()).andReturn(termNode.getPath()).anyTimes();
        expect(term.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(term.getName()).andReturn("Terminal "+address).anyTimes();
        expect(term.getShareInboundOutboundPort()).andReturn(sharePorts).anyTimes();
        return term;
    }

    private void waitForProvider() throws Exception {
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 70000);
        assertFalse(res.isWaitInterrupted());
    }

    private PauseActionNode createPauseActionNode(Node owner, Long interval) {
        PauseActionNode pauseAction = new PauseActionNode();
        pauseAction.setName("pause");
        owner.addAndSaveChildren(pauseAction);
        pauseAction.setInterval(interval);
        assertTrue(pauseAction.start());
        return pauseAction;
    }

    private AudioFileNode createAudioFileNode(String nodeName, String filename) throws Exception {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName(nodeName);
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream(filename);
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());
        return audioFileNode;
    }

    private PlayAudioActionNode createPlayAudioActionNode(String name, Node owner, AudioFileNode file) {
        PlayAudioActionNode playAudioActionNode = new PlayAudioActionNode();
        playAudioActionNode.setName(name);
        owner.addAndSaveChildren(playAudioActionNode);
        playAudioActionNode.setAudioFile(file);
        assertTrue(playAudioActionNode.start());
        return playAudioActionNode;
    }

    private StopConversationActionNode createStopConversationAction(Node owner) {
        StopConversationActionNode stopAction = new StopConversationActionNode();
        stopAction.setName("stop");
        owner.addAndSaveChildren(stopAction);
        assertTrue(stopAction.start());
        return stopAction;
    }
    
    private IvrEndpointConversationListener createTransferListener(final String transferAddress) {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            event.getConversation().transfer(transferAddress);
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    private IvrEndpointConversationListener createTransferListener2(final String transferAddress) {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            event.getConversation().transfer(transferAddress, false, 0, 0);
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    private IvrEndpointConversationListener createLogCallListener() {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            IvrEndpointConversationImpl conv = (IvrEndpointConversationImpl) event.getConversation();
                            CiscoCall call = conv.getCall();
                            
                            System.out.println("!!! CALL. called address: "+call.getCalledAddress());
                            System.out.println("!!! CALL. calling address: "+call.getCallingAddress());
                            System.out.println("!!! CALL. current called address: "+call.getCurrentCalledAddress());
                            System.out.println("!!! CALL. current calling address: "+call.getCurrentCallingAddress());
                            System.out.println("!!! CALL. last redirected address: "+call.getLastRedirectedAddress());
                            System.out.println("!!! CALL. modified called address: "+call.getModifiedCalledAddress());
                            System.out.println("!!! CALL. modified calling address: "+call.getModifiedCallingAddress());
                            System.out.println("!!! CALL. LastRedirectedPartyInfo: "+call.getLastRedirectedPartyInfo());
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    private IvrEndpointConversationListener createTransferListener3(final AtomicReference<String> transferAddress) {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            event.getConversation().transfer(transferAddress.get(), false, 0, 0);
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    private IvrEndpointConversationListener createParkListener(final AtomicReference<String> parkDN) {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            parkDN.set(event.getConversation().park());
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    private IvrEndpointConversationListener createUnParkListener(final AtomicReference<String> parkDN) {
        return new IvrEndpointConversationListenerAdapter(){
            @Override public void conversationStarted(final IvrEndpointConversationEvent event) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            event.getConversation().unpark(parkDN.get());
                        } catch (Exception ex) { }
                    }
                }).start();
            }
            @Override public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
                convStopped.set(true);
            }
        };        
    }

    public static IvrEndpointConversationStoppedEvent handleConversationStopped() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                System.out.println("  !!!  CONVERSATION STOPPED !!!");
                convStopped.set(true);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    public static IvrEndpointConversationEvent sendDTMF(final String digits) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                IvrEndpointConversationEvent event = (IvrEndpointConversationEvent) o;
                event.getConversation().sendDTMF(digits);
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }
    
    public static IvrEndpointConversationEvent sendMessage(final String message) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                IvrEndpointConversationEvent event = (IvrEndpointConversationEvent) o;
                event.getConversation().sendMessage(message, "windows-1251", SendMessageDirection.CALLED_PARTY);
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }

    private interface TestTerminal extends IvrMediaTerminal, Node {
    }
    
}