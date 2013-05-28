/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.conference.impl;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.StateToNodeLogger;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.IfDtmfActionNode;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceException;
import org.onesec.raven.ivr.conference.actions.ConferenceEventHandlerNode;
import org.onesec.raven.ivr.conference.actions.ConferenceSessionStatus;
import org.onesec.raven.ivr.conference.actions.JoinToConferenceActionNode;
import org.onesec.raven.ivr.conference.actions.MuteConferenceParticipantActionNode;
import org.onesec.raven.ivr.conference.actions.UnMuteConferenceParticipantActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrMultichannelEndpointNode;
import org.onesec.raven.ivr.impl.RtpAddressNode;
import org.onesec.raven.ivr.impl.RtpStreamManagerNode;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.impl.GotoNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceManagerRealTest extends OnesecRavenTestCase {
    private final static String RP_TEST_NUMBER = "631616";
    
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private RtpStreamManagerNode rtpManager;
    private ContainerNode termNode;
    private IvrEndpointConversation conv;
    private ConferenceManagerNode manager;
    private Conference conference;
    private IvrMultichannelEndpointNode endpoint;

    
    @Before
    public void prepare() throws Exception {
        rtpManager = new RtpStreamManagerNode();
        rtpManager.setName("rtpManager");
        testsNode.addAndSaveChildren(rtpManager);
        assertTrue(rtpManager.start());

        RtpAddressNode address = new RtpAddressNode();
        address.setName(getInterfaceAddress().getHostAddress());
        rtpManager.addAndSaveChildren(address);
        address.setStartingPort(18384);
        assertTrue(address.start());

        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        testsNode.addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("631609 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88000);
        provider.setToNumber(631799);
        provider.setHost("10.0.137.125");
        provider.setPassword(privateProperties.getProperty("ccm_dialer_proxy_kom"));
        provider.setUser("ccm_dialer_proxy_kom");
        assertTrue(provider.start());
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setMaximumPoolSize(300);
        executor.setCorePoolSize(200);
        executor.setMaximumQueueSize(1);
        assertTrue(executor.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        testsNode.addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        termNode = new ContainerNode("term node");
        testsNode.addAndSaveChildren(termNode);
        termNode.setLogLevel(LogLevel.TRACE);
        assertTrue(termNode.start());

        providerRegistry = registry.getService(ProviderRegistry.class);
        stateListenersCoordinator = registry.getService(StateListenersCoordinator.class);
        StateToNodeLogger stateLogger = registry.getService(StateToNodeLogger.class);
        stateLogger.setLoggerNode(termNode);        
        
        createConferenceManager();
        createScenario();
        waitForProvider();
        createEndpoint();
    }
    
    @Test
    public void test() throws InterruptedException {
        Thread.sleep(5*60*1000);
    }
    
    private void createConferenceManager() throws ConferenceException {
        manager = new ConferenceManagerNode();
        manager.setName("Conference manager");
        testsNode.addAndSaveChildren(manager);
        manager.setChannelsCount(10);
        manager.setExecutor(executor);
        assertTrue(manager.start());
        conference = manager.createConference("test", addToCur(5), addToCur(60*5), 10, null);        
        manager.setLogLevel(LogLevel.TRACE);
        ((BaseNode)conference).setLogLevel(LogLevel.TRACE);
    }
    
    private void createScenario() {
        scenario = new IvrConversationScenarioNode();
        scenario.setName("Conference scenario");
        testsNode.addAndSaveChildren(scenario);
        scenario.setValidDtmfs("12");
        scenario.setLogLevel(LogLevel.TRACE);
        assertTrue(scenario.start());
        createJoinToConferenceAction();
//        ConferenceEventHandlerNode onConnectedHandler = createConnectedEventHandler();
        createOnStopEventHandler();
        createMuteAction(createEventHandler(ConferenceSessionStatus.UNMUTED));
        createUnmuteAction(createEventHandler(ConferenceSessionStatus.MUTED));
        createPauseActionNode(scenario, 60000l);
        createGotoNode(scenario, scenario);
    }
    
    private void createJoinToConferenceAction() {
        JoinToConferenceActionNode action = new JoinToConferenceActionNode();
        action.setName("join to test conference");
        scenario.addAndSaveChildren(action);
        action.setAutoConnect(Boolean.TRUE);
        action.setAutoUnmute(Boolean.TRUE);
        action.setConferenceId(""+conference.getId());
        action.setAccessCode(conference.getAccessCode());
        action.setConferenceManager(manager);
        assertTrue(action.start());
    }
    
    private ConferenceEventHandlerNode createConnectedEventHandler() {
        ConferenceEventHandlerNode handler = new ConferenceEventHandlerNode();
        handler.setName("on connected");
        scenario.addAndSaveChildren(handler);
        handler.setEventType(ConferenceSessionStatus.CONNECTED);
        assertTrue(handler.start());
        return handler;
    }
    
    private ConferenceEventHandlerNode createEventHandler(ConferenceSessionStatus eventType) {
        ConferenceEventHandlerNode handler = new ConferenceEventHandlerNode();
        handler.setName(eventType.toString());
        scenario.addAndSaveChildren(handler);
        handler.setEventType(eventType);
        assertTrue(handler.start());
        return handler;
    }
    
    private void createOnStopEventHandler() {
        createStopConversationAction(createEventHandler(ConferenceSessionStatus.STOPPED));
    }
    
    private void createMuteAction(Node owner) {
        IfDtmfActionNode ifnode = createIfDtmf(owner, "1");
        MuteConferenceParticipantActionNode mute = new MuteConferenceParticipantActionNode();
        mute.setName("mute");
        ifnode.addAndSaveChildren(mute);
        assertTrue(mute.start());
    }
    
    private void createUnmuteAction(Node owner) {
        IfDtmfActionNode ifnode = createIfDtmf(owner, "2");
        UnMuteConferenceParticipantActionNode unmute = new UnMuteConferenceParticipantActionNode();
        unmute.setName("mute");
        ifnode.addAndSaveChildren(unmute);
        assertTrue(unmute.start());
    }
    
    public IfDtmfActionNode createIfDtmf(Node owner, String dtmf) {
        IfDtmfActionNode ifDtmf = new IfDtmfActionNode();
        ifDtmf.setName("if dtmf=="+dtmf);
        owner.addAndSaveChildren(ifDtmf);
        ifDtmf.setDtmf(dtmf);
        assertTrue(ifDtmf.start());
        return ifDtmf;
    }
    
    private void createEndpoint() {
        endpoint = new IvrMultichannelEndpointNode();
        endpoint.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(endpoint);
        endpoint.setLogLevel(LogLevel.TRACE);
        endpoint.setAddress(RP_TEST_NUMBER);
        endpoint.setConversationScenario(scenario);
        endpoint.setExecutor(executor);
        endpoint.setRtpStreamManager(rtpManager);
        endpoint.setLogLevel(LogLevel.TRACE);
        endpoint.setEnableIncomingRtp(Boolean.TRUE);
        assertTrue(endpoint.start());
    }
    
    private Date addToCur(long secs) {
        return new Date(System.currentTimeMillis()+secs*1000);
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
    
    private void createGotoNode(Node owner, ConversationScenarioPoint transition) {
        GotoNode gotoNode = new GotoNode();
        gotoNode.setName("goto");
        owner.addAndSaveChildren(gotoNode);
        gotoNode.setConversationPoint(transition);
        assertTrue(gotoNode.start());
    }
    
    private StopConversationActionNode createStopConversationAction(Node owner) {
        StopConversationActionNode stopAction = new StopConversationActionNode();
        stopAction.setName("stop");
        owner.addAndSaveChildren(stopAction);
        assertTrue(stopAction.start());
        return stopAction;
    }
}
