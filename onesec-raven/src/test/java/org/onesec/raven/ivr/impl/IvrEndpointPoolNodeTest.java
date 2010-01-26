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

import java.util.concurrent.TimeUnit;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrEndpointState;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointPoolNodeTest extends OnesecRavenTestCase
{
    private IvrEndpointPoolNode pool;
    private ExecutorServiceNode executor;
    private IvrEndpointNode endpoint;
    private Node requestOwner;
    private Node requestOwner2;

    @Before
    public void prepare() throws Exception
    {
        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("88013 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88013);
        provider.setToNumber(88024);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());
        waitForProvider();

        requestOwner = new ContainerNode("requestOwner");
        tree.getRootNode().addAndSaveChildren(requestOwner);
        assertTrue(requestOwner.start());

        requestOwner2 = new ContainerNode("requestOwner2");
        tree.getRootNode().addAndSaveChildren(requestOwner2);
        assertTrue(requestOwner2.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(4);
        assertTrue(executor.start());

        pool = new IvrEndpointPoolNode();
        pool.setName("endpoint pools");
        tree.getRootNode().addAndSaveChildren(pool);
        pool.setExecutor(executor);
        pool.setLogLevel(LogLevel.TRACE);
        assertTrue(pool.start());

        endpoint = createEndpoint("88013", 1234);
        
    }

    @Test
    public void simpleTest() throws InterruptedException
    {
        EndpointRequest req = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        req.processRequest(endpoint);
        replay(req);

        pool.requestEndpoint(req);
        TimeUnit.SECONDS.sleep(1);

        verify(req);
    }

    @Test(timeout=25000)
    public void endpointRealeseTest() throws Exception
    {
        EndpointRequest req = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        req.processRequest(endpoint);
        expectLastCall().times(2);
        replay(req);

        pool.requestEndpoint(req);
        TimeUnit.SECONDS.sleep(1);
        pool.requestEndpoint(req);
        TimeUnit.SECONDS.sleep(1);

        verify(req);
    }

    @Test(timeout=25000)
    public void endpointTimeoutTest() throws Exception
    {
        EndpointRequest req = createMock(EndpointRequest.class);
        EndpointRequest req2 = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req2.getOwner()).andReturn(requestOwner2).anyTimes();
        req.processRequest(endpoint);
        expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer() throws Throwable {
                TimeUnit.SECONDS.sleep(2);
                return null;
            }
        });
        req2.processRequest(null);
        expect(req2.getWaitTimeout()).andReturn(1000l);
        replay(req, req2);
        
        pool.requestEndpoint(req);
        TimeUnit.MILLISECONDS.sleep(100);
        pool.requestEndpoint(req2);
        TimeUnit.SECONDS.sleep(3);

        verify(req, req2);
    }

    @Test(timeout=25000)
    public void asyncTest() throws Exception
    {
        createEndpoint("88014", 1236);

        EndpointRequest req = createMock(EndpointRequest.class);
        EndpointRequest req2 = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req2.getOwner()).andReturn(requestOwner2).anyTimes();
        req.processRequest(isA(IvrEndpointNode.class));
        expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer() throws Throwable {
                TimeUnit.SECONDS.sleep(2);
                return null;
            }
        });
        req2.processRequest(isA(IvrEndpointNode.class));
        replay(req, req2);

        pool.requestEndpoint(req);
        TimeUnit.MILLISECONDS.sleep(100);
        pool.requestEndpoint(req2);
        TimeUnit.MILLISECONDS.sleep(2100);

        verify(req, req2);
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 10000);
        assertFalse(res.isWaitInterrupted());
    }

    private IvrEndpointNode createEndpoint(String number, int port)
    {
        IvrEndpointNode endpoint = new IvrEndpointNode();
        endpoint.setName(number);
        pool.addAndSaveChildren(endpoint);
        endpoint.setExecutorService(executor);
        endpoint.setAddress(number);
        endpoint.setIp("10.50.1.134");
        endpoint.setPort(port);
        endpoint.setLogLevel(LogLevel.TRACE);
        assertTrue(endpoint.start());

        StateWaitResult res = endpoint.getEndpointState().waitForState(
            new int[]{IvrEndpointState.IN_SERVICE}, 2000);
       return endpoint;
    }
}