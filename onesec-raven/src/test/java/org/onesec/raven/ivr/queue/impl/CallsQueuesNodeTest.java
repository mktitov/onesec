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

package org.onesec.raven.ivr.queue.impl;

import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import java.util.concurrent.TimeUnit;
import org.raven.ds.DataContext;
import org.raven.tree.Node;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrEndpointNode;
import org.onesec.raven.ivr.impl.IvrEndpointPoolNode;
import org.onesec.raven.ivr.impl.RtpAddressNode;
import org.onesec.raven.ivr.impl.RtpStreamManagerNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.actions.QueueCallActionNode;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.PushDataSource;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueuesNodeTest extends OnesecRavenTestCase
{
    private CallsQueuesNode queues;
    private CallQueueCdrRecordSchemaNode schema;
    private PushDataSource ds;

    private QueueCallActionNode queueCallAction;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        queues = new CallsQueuesNode();
        queues.setName("queues");
        tree.getRootNode().addAndSaveChildren(queues);
        queues.setDataSource(ds);

        schema = new CallQueueCdrRecordSchemaNode();
        schema.setName("queue cdr");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());
    }

    @Test
    public void recordSchemaTest()
    {
        queues.setCdrRecordSchema(schema);
        assertTrue(queues.start());
    }

    @Test
    public void initNodesTest()
    {
        assertTrue(queues.start());
        
        Node operatorsNode = queues.getChildren(CallsQueueOperatorsNode.NAME);
        assertNotNull(operatorsNode);
        assertStarted(operatorsNode);
        assertTrue(operatorsNode instanceof CallsQueueOperatorsNode);
        
        Node queuesNode = queues.getChildren(CallsQueuesContainerNode.NAME);
        assertNotNull(queuesNode);
        assertTrue(queuesNode instanceof CallsQueuesContainerNode);
        assertStarted(queuesNode);
    }

    @Test
    public void badRecordSchemaTest()
    {
        RecordSchemaNode badSchema = new RecordSchemaNode();
        badSchema.setName("basSchema");
        tree.getRootNode().addAndSaveChildren(badSchema);
        assertTrue(badSchema.start());

        queues.setCdrRecordSchema(badSchema);
        assertFalse(queues.start());
    }

    @Test
    public void withoutSchemaTest()
    {
        queues.setCdrRecordSchema(null);
        assertTrue(queues.start());
    }
    
    @Test
    public void queueCallOnStoppedNode()
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getConversation()).andReturn(conv).anyTimes();
        expect(conv.getObjectName()).andReturn("call info").anyTimes();
        conv.addConversationListener(isA(IvrEndpointConversationListener.class));
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        ds.pushData(req);
        
        verify(req, conv);
    }
    
    @Test
    public void nullQueueIdTest()
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getQueueId()).andReturn(null);
        expect(req.getConversation()).andReturn(conv).anyTimes();
        expect(conv.getObjectName()).andReturn("call info").anyTimes();
        conv.addConversationListener(isA(IvrEndpointConversationListener.class));
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        assertTrue(queues.start());
        
        ds.pushData(req);
        
        verify(req, conv);
    }

    @Test
    public void queueNotFoundTest() 
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getQueueId()).andReturn("queue").anyTimes();
        expect(req.getConversation()).andReturn(conv).anyTimes();
        expect(conv.getObjectName()).andReturn("call info").anyTimes();
        conv.addConversationListener(isA(IvrEndpointConversationListener.class));
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        assertTrue(queues.start());
        
        ds.pushData(req);
        
        verify(req, conv);
    }
    
    @Test
    public void queueNotFound2()
    {
        assertTrue(queues.start());
        TestCallsQueueNode queue = new TestCallsQueueNode();
        queue.setName("queue");
        queues.getQueuesNode().addAndSaveChildren(queue);
        
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getQueueId()).andReturn("queue").anyTimes();
        expect(req.getConversation()).andReturn(conv).anyTimes();
        expect(conv.getObjectName()).andReturn("call info").anyTimes();
        conv.addConversationListener(isA(IvrEndpointConversationListener.class));
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        assertNull(queue.lastRequest);
        
        ds.pushData(req);
        
        verify(req, conv);
    }
    
    @Test
    public void foundTest()
    {
        assertTrue(queues.start());
        TestCallsQueueNode queue = new TestCallsQueueNode();
        queue.setName("queue");
        queues.getQueuesNode().addAndSaveChildren(queue);
        assertTrue(queue.start());
        
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        DataContext context = createMock(DataContext.class);
        
        expect(req.getQueueId()).andReturn("queue").anyTimes();
        expect(req.getConversation()).andReturn(conv).anyTimes();
        expect(conv.getObjectName()).andReturn("call info").anyTimes();
        
        replay(req, context);
        
        ds.pushData(req, context);
        assertNotNull(queue.lastRequest);
        assertSame(req, queue.lastRequest.getWrappedRequest());
        assertSame(context, queue.lastRequest.getContext());
        
        verify(req, context);
    }

    @Test
    public void realTest() throws Exception
    {
        prepareRealTest();
        TimeUnit.SECONDS.sleep(30);
    }

    private void prepareRealTest() throws Exception
    {
        RtpStreamManagerNode manager = new RtpStreamManagerNode();
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
        provider.setToNumber(88024);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());

        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(15);
        assertTrue(executor.start());

        IvrEndpointPoolNode pool = new IvrEndpointPoolNode();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        pool.setExecutor(executor);
        pool.setLogLevel(LogLevel.TRACE);
        assertTrue(pool.start());

        waitForProvider();
        //creating abonent endpoint
        IvrConversationScenarioNode abonentScenario = createAbonentScenario();
        createEndpoint(tree.getRootNode(), executor, manager, abonentScenario, "88013");
    }

    private void createCallsQueues(ExecutorServiceNode executor)
    {
        CallsQueuesNode queues = new CallsQueuesNode();
        queues.setName("call queues");
        tree.getRootNode().addAndSaveChildren(queues);
        queues.setDataSource(queueCallAction);
        assertTrue(queues.start());

        CallsQueueOperatorNode operator = new CallsQueueOperatorNode();
        operator.setName("Titov MK");
        queues.getOperatorsNode().addAndSaveChildren(operator);
        operator.setPhoneNumbers("089128672947");
        assertTrue(operator.start());

        CallsQueueNode queue = new CallsQueueNode();
        queue.setName("test");
        queues.getQueuesNode().addAndSaveChildren(queue);
        queue.setExecutor(executor);
        assertTrue(queue.start());

        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
        selector.setName("");
    }

    private void createEndpoint(Node owner, ExecutorServiceNode executor, RtpStreamManagerNode manager
            , IvrConversationScenarioNode scenario, String address)
    {
        IvrEndpointNode endpoint = new IvrEndpointNode();
        endpoint.setName(address);
        owner.addAndSaveChildren(endpoint);
        endpoint.setRtpStreamManager(manager);
        endpoint.setExecutorService(executor);
        endpoint.setConversationScenario(scenario);
        endpoint.setAddress(address);
        endpoint.setLogLevel(LogLevel.TRACE);
        assertTrue(endpoint.start());
    }

    private IvrConversationScenarioNode createAbonentScenario()
    {
        IvrConversationScenarioNode abonentScenario = new IvrConversationScenarioNode();
        abonentScenario.setName("abonent scenario");
        tree.getRootNode().addAndSaveChildren(abonentScenario);
        assertTrue(abonentScenario.start());

        queueCallAction = new QueueCallActionNode();
        queueCallAction.setName("queue call");
        abonentScenario.addAndSaveChildren(queueCallAction);
        queueCallAction.setContinueConversationOnReadyToCommutate(Boolean.TRUE);
        queueCallAction.setPriority(10);
        queueCallAction.setQueueId("test");
        assertTrue(queueCallAction.start());

        return abonentScenario;
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