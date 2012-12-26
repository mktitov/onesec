/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.StateToNodeLogger;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.raven.tree.Node;

/**
 *
 * @author Mihail Titov
 */
public class CiscoJtapiRouteTerminalTest extends OnesecRavenTestCase {
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private ExecutorServiceNode executor;
    private CiscoJtapiRouteTerminal terminal;
//    private CiscoCallsRouterNode termNode;
    private ContainerNode termNode;
    private IMocksControl mockControl;
    
    @Before
    public void prepare() {
        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("88191 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88000);
        provider.setToNumber(631799);
        provider.setHost("10.16.15.1");
        provider.setPassword(privateProperties.getProperty("ccm_dialer_proxy_kom"));
        provider.setUser("ccm_dialer_proxy_kom");
        assertTrue(provider.start());
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(10);
        assertTrue(executor.start());

        termNode = new ContainerNode("term node");
//        termNode = new CiscoCallsRouterNode();
//        termNode.setName("calls router node");
        tree.getRootNode().addAndSaveChildren(termNode);
        termNode.setLogLevel(LogLevel.TRACE);
//        termNode.setAddress("88191");
        assertTrue(termNode.start());
        
        providerRegistry = registry.getService(ProviderRegistry.class);
        stateListenersCoordinator = registry.getService(StateListenersCoordinator.class);
        StateToNodeLogger stateLogger = registry.getService(StateToNodeLogger.class);
        stateLogger.setLoggerNode(termNode);
        
    }
    
//    @Test
    public void startStopTest() throws Exception {
        waitForProvider();
        IvrTerminal term = trainTerminal("88191");
        mockControl.replay();
        terminal = new CiscoJtapiRouteTerminal(providerRegistry, stateListenersCoordinator, term);
        IvrTerminalState state = terminal.getState();
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        terminal.start();
        state.waitForState(new int[]{IvrTerminalState.IN_SERVICE}, 5000);
        assertEquals(IvrTerminalState.IN_SERVICE, state.getId());
        Thread.sleep(100);
        terminal.stop();
        state.waitForState(new int[]{IvrTerminalState.OUT_OF_SERVICE}, 5000);
        assertEquals(IvrTerminalState.OUT_OF_SERVICE, state.getId());
        mockControl.verify();
    }
    
//    @Test
    public void routeTest() throws Exception {
        waitForProvider();
        IvrTerminal term = trainTerminal("88191");
        mockControl.replay();
        terminal = new CiscoJtapiRouteTerminal(providerRegistry, stateListenersCoordinator, term);
        terminal.start();
        terminal.getState().waitForState(new int[]{IvrTerminalState.IN_SERVICE}, 5000);
        assertEquals(0, terminal.getRoutes().size());
        terminal.registerRoute(new CallRouteRuleImpl("88024", "0325532", "9111110000", false));
        assertEquals(1, terminal.getRoutes().size());
        Thread.sleep(30000);
        assertEquals(0, terminal.getRoutes().size());
        terminal.stop();        
    }
    
    @Test
    public void realRouteTest() throws Exception {
        waitForProvider();
        CiscoCallsRouterNode router = new CiscoCallsRouterNode();
        router.setName("router: 88191");
        tree.getRootNode().addAndSaveChildren(router);        
        router.setAddress("88191");
        assertTrue(router.start());
        router.getTerminalState().waitForState(new int[]{IvrTerminalState.IN_SERVICE}, 5000);
        router.setData(null, new CallRouteRuleImpl("88024", "0325532", "9111110000", false), null);
        Thread.sleep(30000);
        router.stop();
    }
    
    private void waitForProvider() throws Exception {
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 70000);
        assertFalse(res.isWaitInterrupted());
    }
    
    private IvrTerminal trainTerminal(String address) {
        mockControl = createControl();
        TestTerminal term = mockControl.createMock(TestTerminal.class);
        expect(term.getLogger()).andReturn(termNode).anyTimes();
        expect(term.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(term.isLogLevelEnabled(isA(LogLevel.class))).andReturn(true).anyTimes();
        expect(term.getObjectName()).andReturn(termNode.getName()).anyTimes();
        expect(term.getObjectDescription()).andReturn("Terminal").anyTimes();
        expect(term.getName()).andReturn("RouteTerminal: "+address).anyTimes();
        expect(term.getAddress()).andReturn(address).anyTimes();

        return term;
    }
    
    private interface TestTerminal extends IvrTerminal {        
    }
}
