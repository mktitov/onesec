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

import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.actions.PauseActionNode;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;
import org.easymock.IArgumentMatcher;
import org.raven.tree.Node;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.junit.Before;
import org.junit.Test;
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
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;

/**
 * @author Mikhail Titov
 */
public class CiscoJtapiTerminalTest extends OnesecRavenTestCase {
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private RtpStreamManagerNode manager;
    private CiscoJtapiTerminal endpoint;
    private ContainerNode termNode;
    private static AtomicBoolean convStopped = new AtomicBoolean();

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
        provider.setName("88013 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88013);
        provider.setToNumber(88049);
//        provider.setFromNumber(68050);
//        provider.setToNumber(68050);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(10);
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
        IvrTerminal term = trainTerminal("88049", scenario, true, true);
        replay(term);
        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
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

    //В данном тесте необходимо самому позвонить на номер, указанный в тесте. Должны услышать:
    //  Пароли не совпадают
//    @Test(timeout=20000)
    public void incomingCallTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        endpoint.addConversationListener(listener);
        startEndpoint(endpoint);
        waitForConversationStop();

        stopEndpoint(endpoint);
        verify(term, listener);
    }

    //В данном тесте система позвонит, на указанный адрес. Необходимо взять трубку. Должны услышать:
    //  Пароли не совпадают
    @Test(timeout=20000)
    public void inviteTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrTerminal term = trainTerminal("88014", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88024", 0, 0, listener, scenario, null);
//        endpoint.invite("88027", 0, 0, listener, scenario, null);
        waitForConversationStop();
        stopEndpoint(endpoint);
        
        verify(term, listener);
    }

    //В данном тесте система позвонит на указанный адрес но трубку брать не надо
//    @Test(timeout=20000)
    public void inviteTimeoutTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainInviteTimeoutListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 5, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //В данном тесте система позвонит на указанный адрес и нужно взять трубку. Должны услышать:
    //  Пароли не совпадают -> Пауза 5 сек -> Пароли не совпадают
//    @Test(timeout=30000)
    public void inviteTimeoutTest2() throws Exception {
        waitForProvider();
        createSimpleScenario2(5000);

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 5, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //В данном тесте система позвонит на указанный адрес и нужно взять трубку в течении 5 сек!.
    //Услышать ничего не должы, система сразу положит трубку
//    @Test(timeout=30000)
    public void inviteTimeoutTest3() throws Exception {
        waitForProvider();
        createSimpleScenario3();

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 8, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        Thread.sleep(9000);
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    //Задача теста проверить, что не сработает INVITE TIMEOUT при постановке вызова на удержании
    //В данном тесте система позвонит на указанный адрес и нужно взять трубку в течении 5 сек!.
    //Далее необходимо поставить вызов на удержании. 
    //Через 10 сек необходимо снять вызов с удержания
    //Должны услышать: Пароли не совпадают
//    @Test(timeout=30000)
    public void inviteTimeoutTest4() throws Exception {
        waitForProvider();
        createSimpleScenario4();

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener2();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 8, 0, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
//        Thread.sleep(9000);
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    
    //В данном тесте система позвонит на указанный адрес и нужно взять трубку. 
    //Сценарий информирования следующий:
    //  Пароли не совпадают -> Пауза 5 сек  -> Пароли не совпадают
    //А должны услышать:
    //  Пароли не совпадают -> Пауза ~5 сек 
//    @Test(timeout=30000)
    public void maxCallDurationTest() throws Exception {
        waitForProvider();
        createSimpleScenario2(5000);

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 0, 5, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }

    //В данном тесте система позвонит на указанный адрес и нужно взять трубку в течении 5 сек. 
    //В тесте используется и INVITE_TIMEOUT и MAX_CONVERSATION_DUR, но вызовов должен "отвалиться"
    //по MAX_CONVERSATION_DUR
    //Сценарий информирования следующий:
    //  Пароли не совпадают -> Пауза 10 сек  -> Пароли не совпадают
    //А должны услышать:
    //  Пароли не совпадают -> Пауза ~10 сек 
//    @Test(timeout=30000)
    public void maxCallDurationTest2() throws Exception {
        waitForProvider();
        createSimpleScenario2(10000);

        IvrTerminal term = trainTerminal("88013", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("88027", 5, 10, listener, scenario, null);
        waitForConversationStop();
        assertEquals(0, endpoint.getCallsCount());
        stopEndpoint(endpoint);

        verify(term, listener);
    }
    
    //Цель теста послушать качество 
    //  Пароли не совпадают
//    @Test
    public void longCallTest() throws Exception {
        waitForProvider();
        createSimpleScenario();

        IvrTerminal term = trainTerminal("88014", scenario, true, true);
        IvrEndpointConversationListener listener = trainListener();
        replay(term, listener);

        endpoint = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, term);
        startEndpoint(endpoint);
        endpoint.invite("089128672947", 0, 0, listener, scenario, null);
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

    private TestTerminal trainTerminal(String address, IvrConversationScenario scenario, boolean enableInCalls
            , boolean enableInRtp)
    {
        TestTerminal term = createMock((TestTerminal.class));
        expect(term.getLogger()).andReturn(termNode).anyTimes();
        expect(term.isLogLevelEnabled(isA(LogLevel.class))).andReturn(true).anyTimes();
        expect(term.getObjectName()).andReturn(termNode.getName()).anyTimes();
        expect(term.getObjectDescription()).andReturn("Terminal").anyTimes();
        expect(term.getAddress()).andReturn(address);
        expect(term.getCodec()).andReturn(Codec.AUTO);
        expect(term.getConversationScenario()).andReturn(scenario);
        expect(term.getEnableIncomingCalls()).andReturn(enableInCalls);
        expect(term.getEnableIncomingRtp()).andReturn(enableInRtp);
        expect(term.getExecutor()).andReturn(executor);
        expect(term.getRtpMaxSendAheadPacketsCount()).andReturn(0);
        expect(term.getRtpPacketSize()).andReturn(null);
        expect(term.getRtpStreamManager()).andReturn(manager);

        return term;
    }

    private void waitForProvider() throws Exception {
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 30000);
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

    public static IvrEndpointConversationStoppedEvent handleConversationStopped() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                convStopped.set(true);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    private interface TestTerminal extends IvrTerminal, Node {
    }
}