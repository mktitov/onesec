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

import java.util.List;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.raven.ds.DataContext;
import org.raven.ds.RecordException;
import org.raven.tree.Node;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrConversationsBridgeManagerNode;
import org.onesec.raven.ivr.impl.IvrEndpointNode;
import org.onesec.raven.ivr.impl.IvrEndpointPoolNode;
import org.onesec.raven.ivr.impl.IvrMultichannelEndpointNode;
import org.onesec.raven.ivr.impl.RtpAddressNode;
import org.onesec.raven.ivr.impl.RtpStreamManagerNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.actions.QueueCallActionNode;
import org.onesec.raven.ivr.queue.actions.QueuedCallEventHandlerNode;
import org.onesec.raven.ivr.queue.actions.WaitForCallCommutationActionNode;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
import org.raven.conv.impl.GotoNode;
import org.raven.ds.Record;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private DataCollector collector;
    private List<Node> endpoints;

    private QueueCallActionNode queueCallAction;

    @Before
    public void prepare() {
        endpoints = new ArrayList<Node>();
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
    
    @After
    public void clean() {
        for (Node endpoint: endpoints)
            endpoint.stop();
    }

//    @Test
    public void recordSchemaTest()
    {
        queues.setCdrRecordSchema(schema);
        assertTrue(queues.start());
    }

//    @Test
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

//    @Test
    public void badRecordSchemaTest()
    {
        RecordSchemaNode badSchema = new RecordSchemaNode();
        badSchema.setName("basSchema");
        tree.getRootNode().addAndSaveChildren(badSchema);
        assertTrue(badSchema.start());

        queues.setCdrRecordSchema(badSchema);
        assertFalse(queues.start());
    }

//    @Test
    public void withoutSchemaTest()
    {
        queues.setCdrRecordSchema(null);
        assertTrue(queues.start());
    }
    
//    @Test
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
    
//    @Test
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

//    @Test
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
    
//    @Test
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
    
//    @Test
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

        TimeUnit.SECONDS.sleep(120);

        Logger log = LoggerFactory.getLogger(Node.class);
        log.debug("!! Finising test !!");
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector.getDataList().get(0);
        writeRecord(rec, log);
    }

    private void writeRecord(Record rec, Logger log) throws RecordException
    {
        log.debug("CDR Record data: ");
        for (Map.Entry entry: rec.getValues().entrySet())
            log.debug("  {} : {}", entry.getKey(), entry.getValue());
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
        address.setStartingPort(16384);
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

        IvrConversationsBridgeManagerNode bridgeManager = new IvrConversationsBridgeManagerNode();
        bridgeManager.setName("conversation bridge manager");
        tree.getRootNode().addAndSaveChildren(bridgeManager);
        bridgeManager.setLogLevel(LogLevel.TRACE);
        assertTrue(bridgeManager.start());

        waitForProvider();
        //creating abonent endpoint
        IvrConversationScenarioNode abonentScenario = createAbonentScenario();
//        createEndpoint(tree.getRootNode(), executor, manager, abonentScenario, "88013");
        createMultichannelEndpoint(executor, manager, abonentScenario);
        createEndpoint(pool, executor, manager, abonentScenario/*в данном случае не важно что*/, "88013");
        
        assertTrue(pool.start());
        

        IvrConversationScenarioNode operatorScenarioNode = createOperatorScenario();
        RecordSchemaNode cdrSchema = createCdrRecordSchema();
        
        createCallsQueues(executor, operatorScenarioNode, pool, bridgeManager, cdrSchema);

    }

    private RecordSchemaNode createCdrRecordSchema()
    {
        CallQueueCdrRecordSchemaNode cdr = new CallQueueCdrRecordSchemaNode();
        cdr.setName("cdr schema");
        tree.getRootNode().addAndSaveChildren(cdr);
        assertTrue(cdr.start());
        return cdr;
    }

    private void createCallsQueues(ExecutorServiceNode executor, IvrConversationScenarioNode operatorScenario
            , IvrEndpointPoolNode pool, IvrConversationsBridgeManagerNode bridge, RecordSchemaNode schema) throws Exception
    {
        CallsQueuesNode queues = new CallsQueuesNode();
        queues.setName("call queues");
        tree.getRootNode().addAndSaveChildren(queues);
        queues.setDataSource(queueCallAction);
        queues.setLogLevel(LogLevel.TRACE);
        queues.setCdrRecordSchema(schema);
        assertTrue(queues.start());

        collector = new DataCollector();
        collector.setName("cdr record collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(queues);
        assertTrue(collector.start());

        AudioFileNode greeting = createAudioFileNode("operator greeting", "src/test/wav/greeting.wav");

        CallsQueueOperatorNode operator = new CallsQueueOperatorNode();
        operator.setName("Titov MK");
        queues.getOperatorsNode().addAndSaveChildren(operator);
        operator.setPhoneNumbers("88027");
//        operator.setPhoneNumbers("089128672947");
        operator.setEndpointPool(pool);
        operator.setConversationsBridgeManager(bridge);
        operator.setLogLevel(LogLevel.TRACE);
        operator.setGreeting(greeting);
        operator.setExecutor(executor);
        assertTrue(operator.start());

        CallsQueueNode queue = new CallsQueueNode();
        queue.setName("test");
        queues.getQueuesNode().addAndSaveChildren(queue);
        queue.setExecutor(executor);
        queue.setLogLevel(LogLevel.TRACE);
        assertTrue(queue.start());

        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
        selector.setName("default selector");
        queue.addAndSaveChildren(selector);
        selector.setPriority(0);
        selector.setLogLevel(LogLevel.TRACE);
        assertTrue(selector.start());

        CallsQueueOperatorRefNode operatorRef = new CallsQueueOperatorRefNode();
        operatorRef.setName("ref to Titov MK");
        selector.addAndSaveChildren(operatorRef);
        operatorRef.setOperator(operator);
        operatorRef.setConversationScenario(operatorScenario);
        operatorRef.setLogLevel(LogLevel.TRACE);
        assertTrue(operatorRef.start());        
    }

    private void createMultichannelEndpoint(ExecutorServiceNode executor, RtpStreamManagerNode manager
            , IvrConversationScenarioNode scenario)
    {
        IvrMultichannelEndpointNode endpoint = new IvrMultichannelEndpointNode();
        endpoint.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(endpoint);
        endpoint.setLogLevel(LogLevel.TRACE);
        endpoint.setAddress("88049");
        endpoint.setConversationScenario(scenario);
        endpoint.setExecutor(executor);
        endpoint.setRtpStreamManager(manager);
        endpoint.setEnableIncomingRtp(Boolean.TRUE);
        assertTrue(endpoint.start());
        endpoints.add(endpoint);
    }

    private void createEndpoint(Node owner, ExecutorServiceNode executor, RtpStreamManagerNode manager
            , IvrConversationScenarioNode scenario, String address)
    {
        IvrEndpointNode endpoint = new IvrEndpointNode();
        endpoint.setName(address);
        owner.addAndSaveChildren(endpoint);
        endpoint.setRtpStreamManager(manager);
        endpoint.setExecutor(executor);
        endpoint.setConversationScenario(scenario);
        endpoint.setAddress(address);
        endpoint.setLogLevel(LogLevel.TRACE);
        endpoint.setEnableIncomingRtp(Boolean.TRUE);
        assertTrue(endpoint.start());
        endpoints.add(endpoint);
    }

    private IvrConversationScenarioNode createAbonentScenario()
    {
        IvrConversationScenarioNode abonentScenario = new IvrConversationScenarioNode();
        abonentScenario.setName("abonent scenario");
        tree.getRootNode().addAndSaveChildren(abonentScenario);
        abonentScenario.setLogLevel(LogLevel.TRACE);
        assertTrue(abonentScenario.start());

        QueuedCallEventHandlerNode disconnectHandler = new QueuedCallEventHandlerNode();
        disconnectHandler.setName("disconnect handler");
        abonentScenario.addAndSaveChildren(disconnectHandler);
        disconnectHandler.setEventType(QueuedCallEventHandlerNode.Status.DISCONNECTED);
        disconnectHandler.setLogLevel(LogLevel.TRACE);
        assertTrue(disconnectHandler.start());

        StopConversationActionNode stopAction = new StopConversationActionNode();
        stopAction.setName("stop conversation");
        disconnectHandler.addAndSaveChildren(stopAction);
        assertTrue(stopAction.start());

        QueuedCallEventHandlerNode queueingHandler = new QueuedCallEventHandlerNode();
        queueingHandler.setName("queueing handler");
        abonentScenario.addAndSaveChildren(queueingHandler);
        queueingHandler.setEventType(QueuedCallEventHandlerNode.Status.QUEUING);
        assertTrue(queueingHandler.start());

        PauseActionNode pauseAction = new PauseActionNode();
        pauseAction.setName("pause 2 secs");
        queueingHandler.addAndSaveChildren(pauseAction);
        pauseAction.setInterval(2000l);
        assertTrue(pauseAction.start());

        queueCallAction = new QueueCallActionNode();
        queueCallAction.setName("queue call");
        abonentScenario.addAndSaveChildren(queueCallAction);
        queueCallAction.setContinueConversationOnReadyToCommutate(Boolean.TRUE);
        queueCallAction.setPriority(10);
        queueCallAction.setQueueId("test");
        queueCallAction.setPlayOperatorGreeting(Boolean.TRUE);
        queueCallAction.setLogLevel(LogLevel.TRACE);
        assertTrue(queueCallAction.start());

        GotoNode gotoAction = new GotoNode();
        gotoAction.setName("goto");
        abonentScenario.addAndSaveChildren(gotoAction);
        gotoAction.setConversationPoint(abonentScenario);
        assertTrue(gotoAction.start());

        return abonentScenario;
    }

    private IvrConversationScenarioNode createOperatorScenario()
    {
        IvrConversationScenarioNode operatorScenario = new IvrConversationScenarioNode();
        operatorScenario.setName("operator scenario");
        tree.getRootNode().addAndSaveChildren(operatorScenario);
        operatorScenario.setLogLevel(LogLevel.TRACE);
        assertTrue(operatorScenario.start());

        WaitForCallCommutationActionNode action = new WaitForCallCommutationActionNode();
        action.setName("commutation action");
        operatorScenario.addAndSaveChildren(action);
        action.setLogLevel(LogLevel.TRACE);
        assertTrue(action.start());

        StopConversationActionNode stopAction = new StopConversationActionNode();
        stopAction.setName("stop conversation");
        operatorScenario.addAndSaveChildren(stopAction);
        assertTrue(stopAction.start());

        return operatorScenario;
    }
    
    private AudioFileNode createAudioFileNode(String nodeName, String filename) throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName(nodeName);
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream(filename);
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());
        return audioFileNode;
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