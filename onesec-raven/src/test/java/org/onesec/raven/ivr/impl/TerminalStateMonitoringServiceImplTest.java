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

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.impl.StateLogger;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.StateToNodeLogger;
import org.onesec.raven.ivr.IvrMediaTerminal;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.test.InThreadExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class TerminalStateMonitoringServiceImplTest extends OnesecRavenTestCase {
    
    @Before
    public void prepare() {
        Node services = tree.getRootNode().getNode(SystemNode.NAME).getNode(ServicesNode.NAME);
        TerminalStateMonitoringServiceNode node = (TerminalStateMonitoringServiceNode) services.getNode(
                TerminalStateMonitoringServiceNode.NAME);
        
        InThreadExecutorService executor = new InThreadExecutorService();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        assertTrue(executor.start());
        node.setExecutor(executor);
        assertTrue(node.start());
    }
    
    @Test
    public void startTerminalAndFilterTest() {
        IvrMediaTerminal term = createMock(IvrMediaTerminal.class);
        IvrMediaTerminal term2 = createMock(IvrMediaTerminal.class);
        IvrMediaTerminal term3 = createMock(IvrMediaTerminal.class);
        ProviderControllerState state = createMock(ProviderControllerState.class);
        ProviderController controller = createMock(ProviderController.class);

        expect(term.getAddress()).andReturn("10");
//        expect(term.getStatus()).andReturn(Status.INITIALIZED);
        expect(term.isStarted()).andReturn(Boolean.FALSE);
        expect(term.isAutoStart()).andReturn(true);
        expect(term.start()).andReturn(Boolean.TRUE);

        expect(term2.getAddress()).andReturn("11");
        expect(term3.getAddress()).andReturn("9");

        expect(state.getId()).andReturn(ProviderControllerState.IN_SERVICE).atLeastOnce();
        expect(state.getObservableObject()).andReturn(controller).atLeastOnce();

        expect(controller.getFromNumber()).andReturn(10);
        expect(controller.getToNumber()).andReturn(10);

        replay(term, state, controller, term2, term3);

        TerminalStateMonitoringServiceImpl stateMon = new TerminalStateMonitoringServiceImpl();
        stateMon.treeReloaded(tree);
        stateMon.addTerminal(term);
        stateMon.addTerminal(term2);
        stateMon.addTerminal(term3);
        stateMon.stateChanged(state);

        verify(term, state, controller, term2, term3);
    }

    @Test
    public void restartTerminalTest(){
        IvrMediaTerminal term = createMock(IvrMediaTerminal.class);
        ProviderControllerState state = createMock(ProviderControllerState.class);
        ProviderController controller = createMock(ProviderController.class);

        expect(term.getAddress()).andReturn("10");
        expect(term.isStarted()).andReturn(Boolean.TRUE);
        expect(term.isAutoStart()).andReturn(true);
        term.stop();
        expect(term.start()).andReturn(Boolean.TRUE);

        expect(state.getId()).andReturn(ProviderControllerState.IN_SERVICE).atLeastOnce();
        expect(state.getObservableObject()).andReturn(controller).atLeastOnce();

        expect(controller.getFromNumber()).andReturn(10);
        expect(controller.getToNumber()).andReturn(10);

        replay(term, state, controller);

        TerminalStateMonitoringServiceImpl stateMon = new TerminalStateMonitoringServiceImpl();
        stateMon.treeReloaded(tree);
        stateMon.addTerminal(term);
        stateMon.stateChanged(state);

        verify(term, state, controller);
    }

    @Test
    public void stopStartedTerminalTest() {
        IvrMediaTerminal term = createMock(IvrMediaTerminal.class);
        ProviderControllerState state = createMock(ProviderControllerState.class);
        ProviderController controller = createMock(ProviderController.class);

        expect(term.getAddress()).andReturn("10");
//        expect(term.getStatus()).andReturn(Status.STARTED);
        expect(term.isStarted()).andReturn(Boolean.TRUE);
        term.stop();

        expect(state.getId()).andReturn(ProviderControllerState.OUT_OF_SERVICE).atLeastOnce();
        expect(state.getObservableObject()).andReturn(controller).atLeastOnce();

        expect(controller.getFromNumber()).andReturn(10);
        expect(controller.getToNumber()).andReturn(10);

        replay(term, state, controller);

        TerminalStateMonitoringServiceImpl stateMon = new TerminalStateMonitoringServiceImpl();
        stateMon.treeReloaded(tree);
        stateMon.addTerminal(term);
        stateMon.stateChanged(state);

        verify(term, state, controller);
    }

    @Test
    public void stopStoppedTerminalTest(){
        IvrMediaTerminal term = createMock(IvrMediaTerminal.class);
        ProviderControllerState state = createMock(ProviderControllerState.class);
        ProviderController controller = createMock(ProviderController.class);

        expect(term.getAddress()).andReturn("10");
//        expect(term.getStatus()).andReturn(Status.INITIALIZED);
        expect(term.isStarted()).andReturn(Boolean.FALSE);

        expect(state.getId()).andReturn(ProviderControllerState.OUT_OF_SERVICE).atLeastOnce();
        expect(state.getObservableObject()).andReturn(controller).atLeastOnce();

        expect(controller.getFromNumber()).andReturn(10);
        expect(controller.getToNumber()).andReturn(10);

        replay(term, state, controller);

        TerminalStateMonitoringServiceImpl stateMon = new TerminalStateMonitoringServiceImpl();
        stateMon.treeReloaded(tree);
        stateMon.addTerminal(term);
        stateMon.stateChanged(state);

        verify(term, state, controller);
    }

    @Test
    public void serviceTest() {
        TerminalStateMonitoringService stateMon = registry.getService(TerminalStateMonitoringService.class);
        assertNotNull(stateMon);
        Node serviceNode = tree.getRootNode().getNode(SystemNode.NAME).getNode(ServicesNode.NAME)
                .getNode(TerminalStateMonitoringServiceNode.NAME);
        assertNotNull(serviceNode);
        assertTrue(serviceNode instanceof TerminalStateMonitoringServiceNode);
    }

    @Test
    public void stateListenerCoordinatorTest() {
        StateListenersCoordinator listenersCoordinator = registry.getService(StateListenersCoordinator.class);
        assertNotNull(listenersCoordinator);
        
        ProviderControllerState state = createMock(ProviderControllerState.class);
        state.addStateListener(isA(TerminalStateMonitoringService.class));
        state.addStateListener(isA(StateToNodeLogger.class));
        state.addStateListener(isA(StateLogger.class));
        replay(state);

        listenersCoordinator.addListenersToState(state, ProviderControllerState.class);

        verify(state);
    }
}