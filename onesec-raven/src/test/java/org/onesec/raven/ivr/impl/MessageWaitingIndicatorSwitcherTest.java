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
import org.raven.log.LogLevel;
import org.raven.test.PushDataSource;
import static org.raven.RavenUtils.*;
import static org.onesec.raven.ivr.impl.MessageWaitingIndicatorSwitcher.*;
/**
 *
 * @author Mikhail Titov
 */
public class MessageWaitingIndicatorSwitcherTest extends OnesecRavenTestCase {
    private final static String TEST_NUMBER = "88024";
    
    private ProviderRegistry providerRegistry;
    private StateListenersCoordinator stateListenersCoordinator;
    private MessageWaitingIndicatorSwitcher switcher;
    private PushDataSource ds;

    @Before
    public void prepare() {
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
        
        ds = new PushDataSource();
        ds.setName("dataSource");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        switcher = new MessageWaitingIndicatorSwitcher();
        switcher.setName("switcher");
        testsNode.addAndSaveChildren(switcher);
        switcher.setDataSource(ds);
        switcher.setLogLevel(LogLevel.TRACE);
        assertTrue(switcher.start());
        
        providerRegistry = registry.getService(ProviderRegistry.class);
        stateListenersCoordinator = registry.getService(StateListenersCoordinator.class);
        StateToNodeLogger stateLogger = registry.getService(StateToNodeLogger.class);
        stateLogger.setLoggerNode(callOperator);
    }
    
    /*
     * В этом тесте message waiting indiciator должен загореться на телефоне TEST_NUMBER
     */
//    @Test
    public void switchOn() throws Exception {
        waitForProvider();
        ds.pushData(asMap(
                pair(ADDRESS_FIELD, (Object)TEST_NUMBER), 
                pair(INDICATOR_FIELD, (Object)true)));
        Thread.sleep(1000);
    }
    
    /*
     * В этом тесте message waiting indiciator должен погаснуть на телефоне TEST_NUMBER
     */
    @Test
    public void switchOff() throws Exception {
        waitForProvider();
        ds.pushData(asMap(
                pair(ADDRESS_FIELD, (Object)TEST_NUMBER), 
                pair(INDICATOR_FIELD, (Object)false)));
        Thread.sleep(1000);
    }
    
    private void waitForProvider() throws Exception {
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 70000);
        assertFalse(res.isWaitInterrupted());
    }
}
