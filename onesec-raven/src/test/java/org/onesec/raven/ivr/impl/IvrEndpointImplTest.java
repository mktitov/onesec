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

import org.junit.Before;
import org.junit.Test;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrTerminal;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import static org.easymock.EasyMock.*;

/**
 * @author Mikhail Titov
 */
public class IvrEndpointImplTest extends OnesecRavenTestCase {
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private RtpStreamManagerNode manager;
    private IvrEndpointImpl endpoint;

    @Before
    public void prepare() throws Exception {
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
        provider.setToNumber(88037);
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

        providerRegistry = registry.getService(ProviderRegistry.class);
        stateListenersCoordinator = registry.getService(StateListenersCoordinator.class);
    }

    @Test
    public void test() throws Exception {
        IvrTerminal term = trainTerminal("88049", scenario, true, true);
        replay(term);
        endpoint = new IvrEndpointImpl(providerRegistry, stateListenersCoordinator, term);
        endpoint.start();
        Thread.sleep(10000);
        verify(term);
    }

    private IvrTerminal trainTerminal(String address, IvrConversationScenario scenario, boolean enableInCalls
            , boolean enableInRtp)
    {
        IvrTerminal term = createMock((IvrTerminal.class));
        expect(term.isLogLevelEnabled(isA(LogLevel.class))).andReturn(Boolean.TRUE).anyTimes();
        expect(term.getLogger()).andReturn(scenario.getLogger());
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
}