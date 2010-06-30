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

import java.util.List;
import java.util.ArrayList;
import org.onesec.raven.ivr.IvrEndpoint;
import org.easymock.IMocksControl;
import java.util.concurrent.TimeUnit;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrEndpointState;
import org.raven.log.LogLevel;
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
    private TestIvrEndpoint endpoint;
    private Node requestOwner;
    private Node requestOwner2;

    @Before
    public void prepare() throws Exception
    {
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

        endpoint = createEndpoint("88013");
        
    }

//    @Test
    public void simpleTest() throws InterruptedException
    {
        EndpointRequest req = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        req.processRequest(endpoint);
        replay(req);

        pool.requestEndpoint(req);
        TimeUnit.SECONDS.sleep(2);

        verify(req);
    }

//    @Test(timeout=25000)
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
        TimeUnit.SECONDS.sleep(2);

        verify(req);
    }

//    @Test(timeout=25000)
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

//    @Test(timeout=25000)
    public void asyncTest() throws Exception
    {
        createEndpoint("88014");

        EndpointRequest req = createMock(EndpointRequest.class);
        EndpointRequest req2 = createMock(EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req2.getOwner()).andReturn(requestOwner2).anyTimes();
        req.processRequest(isA(IvrEndpoint.class));
        expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer() throws Throwable {
                TimeUnit.SECONDS.sleep(2);
                return null;
            }
        });
        req2.processRequest(isA(IvrEndpoint.class));
        replay(req, req2);

        pool.requestEndpoint(req);
        TimeUnit.MILLISECONDS.sleep(100);
        pool.requestEndpoint(req2);
        TimeUnit.MILLISECONDS.sleep(2100);

        verify(req, req2);
    }

//    @Test(timeout=20000)
    public void watchdogTest() throws Exception
    {
        EndpointRequest req = createMock(EndpointRequest.class);

        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req.getWaitTimeout()).andReturn(60000l).anyTimes();
        req.processRequest(endpoint);

        replay(req);

        endpoint.stop();
        pool.requestEndpoint(req);
        TimeUnit.SECONDS.sleep(1);
        pool.executeScheduledJob(null);
        TimeUnit.SECONDS.sleep(6);

        verify(req);
    }

//    @Test
    public void priorityTest() throws InterruptedException
    {
        pool.setLogLevel(LogLevel.ERROR);
        endpoint.stop();
        IMocksControl control = createControl();
        EndpointRequest req = control.createMock("req", EndpointRequest.class);
        EndpointRequest req1 = control.createMock("req1", EndpointRequest.class);
        EndpointRequest req2 = control.createMock("req2", EndpointRequest.class);
        expect(req.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req1.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req2.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req1.getPriority()).andReturn(10).anyTimes();
        expect(req2.getPriority()).andReturn(1).anyTimes();
        expect(req.getWaitTimeout()).andReturn(1000l).anyTimes();
        expect(req1.getWaitTimeout()).andReturn(5000l).anyTimes();
        expect(req2.getWaitTimeout()).andReturn(5000l).anyTimes();
        List<String> order = new ArrayList<String>(3);
//        req.processRequest(checkEndpoint(order, "req"));
        req.processRequest(null);
        req2.processRequest(checkEndpoint(order, "req2"));
        req1.processRequest(checkEndpoint(order, "req1"));

        control.replay();

        pool.requestEndpoint(req);
        TimeUnit.MILLISECONDS.sleep(500);
        pool.requestEndpoint(req1);
        pool.requestEndpoint(req2);
//        TimeUnit.MILLISECONDS.sleep(501);
        endpoint.start();
        TimeUnit.SECONDS.sleep(5);

        control.verify();

        assertEquals("req2", order.get(0));
        assertEquals("req1", order.get(1));
    }

    @Test
    public void auxiliaryPoolTest() throws Exception
    {
        IvrEndpointPoolNode pool2 = new IvrEndpointPoolNode();
        pool2.setName("pool2");
        tree.getRootNode().addAndSaveChildren(pool2);
        pool2.setExecutor(executor);
        pool2.setLogLevel(LogLevel.TRACE);
        TestIvrEndpoint endpoint2 = createEndpoint(pool2, "88014");
        assertTrue(pool2.start());
        pool.setAuxiliaryPool(pool2);

        EndpointRequest req1 = createMock("req1", EndpointRequest.class);
        EndpointRequest req2 = createMock("req2", EndpointRequest.class);
        expect(req1.getOwner()).andReturn(requestOwner).anyTimes();
        expect(req2.getOwner()).andReturn(requestOwner).anyTimes();
        req1.processRequest(endpoint);
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Thread.sleep(1000);
                return null;
            }
        });
        req2.processRequest(endpoint2);

        replay(req1, req2);

        pool.requestEndpoint(req1);
        Thread.sleep(100);
        pool.requestEndpoint(req2);
        Thread.sleep(1000);

        verify(req1, req2);
    }

    public static IvrEndpoint checkEndpoint(final List<String> order, final String req)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                try {
                    order.add(req);
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
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

    private TestIvrEndpoint createEndpoint(IvrEndpointPoolNode pool, String number)
    {
        TestIvrEndpoint endpoint = new TestIvrEndpoint();
        endpoint.setName(number);
        pool.addAndSaveChildren(endpoint);
        endpoint.setLogLevel(LogLevel.TRACE);
        assertTrue(endpoint.start());

       return endpoint;
    }
    
    private TestIvrEndpoint createEndpoint(String number)
    {
        return createEndpoint(pool, number);
    }
}