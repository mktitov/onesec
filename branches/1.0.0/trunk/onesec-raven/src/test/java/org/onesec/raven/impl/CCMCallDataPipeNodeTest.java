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

package org.onesec.raven.impl;

import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
/**
 *
 * @author Mikhail Titov
 */
public class CCMCallDataPipeNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test() throws Exception
    {
        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("88013 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88013);
        provider.setToNumber(88037);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());
        waitForProvider();
        
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        CCMCallDataPipeNode call = new CCMCallDataPipeNode();
        call.setName("caller");
        tree.getRootNode().addAndSaveChildren(call);
        call.setDataSource(ds);
        assertTrue(call.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(call);
        assertTrue(collector.start());

        Map params = new HashMap();
        params.put("num_a", "88024");
        params.put("num_b", "88027");
        ds.pushData(params);

        assertEquals(1, collector.getDataListSize());
        assertSame(params, collector.getDataList().get(0));

        Thread.sleep(2000);
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 30000);
        assertFalse(res.isWaitInterrupted());
    }
}