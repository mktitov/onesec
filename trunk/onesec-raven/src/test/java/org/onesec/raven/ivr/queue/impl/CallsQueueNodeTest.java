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
import java.util.Set;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.queue.*;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import mockit.integration.junit4.JMockit;
import mockit.*;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenModuleTest;
import org.raven.RavenUtils;
import org.raven.table.Table;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class CallsQueueNodeTest extends OnesecRavenModuleTest
{
    
    private CallsQueueNode queue;
    private CallsQueueNode queuesManager;
    private ExecutorServiceNode executor;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        super.configureRegistry(builder); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    @Before
    public void prepare()
    {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(21);
        executor.setMaximumQueueSize(100);
        executor.setLogLevel(LogLevel.TRACE);
        assertTrue(executor.start());
        
        queue = new CallsQueueNode();
        queue.setName("queue");
        tree.getRootNode().addAndSaveChildren(queue);
        queue.setExecutor(executor);
        queue.setLogLevel(LogLevel.TRACE);
    }
    
    @Test
    public void successQueued(@Mocked final CallQueueRequestController req) throws InterruptedException {        
        trainRequest(req);
        new Expectations() {{
            req.getCallsQueue(); result = null;
        }};   
        assertTrue(queue.start());
        queue.queueCall(req);
        Thread.sleep(100);
        new Verifications() {{
            req.getCallsQueue(); 
            req.setCallsQueue(queue);
            req.setPositionInQueue(1);
            req.fireCallQueuedEvent();
        }};
    }    
    
    @Test
    public void getViewableObjects(
            @Mocked final CallQueueRequestController req,
            @Mocked final CallsQueue targetQueue) 
        throws Exception 
    {
        
        trainRequest(req);
        new Expectations() {{
            targetQueue.getName(); result = "target queue";
            req.isValid(); result = true;
            req.getRequestId(); result = 1;
            req.getPriority(); result = 2;
            req.getLastQueuedTime(); result = System.currentTimeMillis();
            req.getTargetQueue(); result = targetQueue;
            req.getOnBusyBehaviourStep(); result = 0;
            req.getOperatorIndex(); result = 1;
        }};
                
        assertTrue(queue.start());
        queue.queueCall(req);
        Thread.sleep(50);
        
        new Verifications() {{
            req.fireCallQueuedEvent();
        }};
        
        List<ViewableObject> vos = queue.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(4, vos.size());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(2).getMimeType());
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(3).getMimeType());
        Table tab = (Table) vos.get(1).getData();
        List<Object[]> rows = RavenUtils.tableAsList(tab);
        assertEquals(1, rows.size());
    }
    
    
//    @Test
//    public void getViewableObjects() throws Exception {
//        
//        CallQueueRequestController req = mock(CallQueueRequestController.class);
//        CallsQueue targetQueue = mock(CallsQueue.class);
//        
//        trainRequest(req);
//        when(targetQueue.getName()).thenReturn("target queue");
////        expect(req.getCallsQueue()).andReturn(null);
////        req.setCallsQueue(queue);
////        req.setRequestId(1);
////        req.setPositionInQueue(1);
////        req.fireCallQueuedEvent();
//        when(req.isValid()).thenReturn(Boolean.TRUE);
//        
//        when(req.getRequestId()).thenReturn(1l);
//        when(req.getPriority()).thenReturn(2);
//        when(req.getLastQueuedTime()).thenReturn(System.currentTimeMillis());
//        when(req.getTargetQueue()).thenReturn(targetQueue);
//        when(req.getOnBusyBehaviourStep()).thenReturn(0);
//        when(req.getOperatorIndex()).thenReturn(1);
//        
////        replay(req, targetQueue);
//        
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        verify(req, timeout(100)).fireCallQueuedEvent();
//        
//        List<ViewableObject> vos = queue.getViewableObjects(null);
//        assertNotNull(vos);
//        assertEquals(4, vos.size());
//        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
//        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());
//        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(2).getMimeType());
//        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(3).getMimeType());
//        Table tab = (Table) vos.get(1).getData();
//        List<Object[]> rows = RavenUtils.tableAsList(tab);
//        assertEquals(1, rows.size());
//        
//        verify(req, targetQueue);
//    }
//    
//    @Test
//    public void rejectedByMaxQueueSize() throws Exception {
////        executor.stop();
//        
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallQueueRequestController req1 = createMock(CallQueueRequestController.class);
//        req.setCallsQueue(queue);
//        expect(req.getCallsQueue()).andReturn(null);
////        req.setRequestId(1);
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        expect(req.getRequestId()).andReturn(1l).anyTimes();
//        req.setPositionInQueue(1);
//        req.fireCallQueuedEvent();
//        
//        expect(req1.getCallsQueue()).andReturn(null);
//        req1.setCallsQueue(queue);
////        req1.setRequestId(2);
//        expect(req1.getPriority()).andReturn(1).anyTimes();
//        expect(req1.getRequestId()).andReturn(2l).anyTimes();
//        req1.addToLog(eq("queue size was exceeded"));
//        req1.fireRejectedQueueEvent();
//        
//        replay(req, req1);
//        queue.setMaxQueueSize(1);
//        assertTrue(queue.start());
//        queue.queueCall(req);
////        executor.start();
//        queue.queueCall(req1);
//        Thread.sleep(500);
//        verify(req, req1);
//    }
    
//    @Test(timeout=5000)
//    public void rejectedByNotFoundPrioritySelector() throws InterruptedException {
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        expect(req.getCallsQueue()).andReturn(null);
//        req.setCallsQueue(queue);
////        req.setRequestId(1);
//        req.setPositionInQueue(1);
//        req.fireCallQueuedEvent();
//        req.addToLog("not found priority selector");
//        req.fireRejectedQueueEvent();
//        expect(req.isValid()).andReturn(Boolean.TRUE);
//        expect(req.logMess("Processing request...")).andReturn("");
//        replay(req);
//
//        queue.setLogLevel(LogLevel.NONE);
//        assertTrue(queue.start());
//        queue.queueCall(req);
////        queue.run();
//        TimeUnit.MILLISECONDS.sleep(500);
//        queue.stop();
//        assertEquals(Node.Status.INITIALIZED, queue.getStatus());
//        while (queue.processingThreadRunning.get())
//            TimeUnit.MILLISECONDS.sleep(100);
//        
//        verify(req);
//    }
//    
//    @Test
//    public void rejectedByNoOperatorsNoOnBusyBehaviour() throws InterruptedException {
//        executor.stop();
//        
//        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
//        selector.setName("selector 1");
//        queue.addAndSaveChildren(selector);
//        selector.setPriority(1);
//        assertTrue(selector.start());
//        
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        expect(req.getCallsQueue()).andReturn(null);
//        req.setCallsQueue(queue);
////        expect(req.getRequestId()).andReturn(0l);
////        req.setRequestId(1);
//        req.setPositionInQueue(1);
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        req.fireCallQueuedEvent();
//        expect(req.isValid()).andReturn(Boolean.TRUE);
//        expect(req.logMess("Processing request...")).andReturn("");
//        expect(req.getOperatorIndex()).andReturn(-1).anyTimes();
//        expect(req.getOnBusyBehaviour()).andReturn(null);
//        req.setOnBusyBehaviour(isA(CallsQueueOnBusyBehaviour.class));
//        expect(req.getOnBusyBehaviourStep()).andReturn(0);
//        req.setOnBusyBehaviourStep(1);
//        req.addToLog(CallsQueueOnBusyBehaviourNode.REACHED_THE_END_OF_SEQ);
//        req.fireRejectedQueueEvent();
//        replay(req);
//        
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(0, queue.queue.size());
//        
//        verify(req);
//    }
//    
//    @Test
//    public void processedByOperatorTest() throws Exception
//    {
//        executor.stop();
//        TestPrioritySelector selector = addPrioritySelector("selector 1", 1, null);
//        
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOperatorRef operatorRef = createMock(CallsQueueOperatorRef.class);
//        CallsQueueOperator operator = createMock(CallsQueueOperator.class);
//        
//        expect(req.getCallsQueue()).andReturn(null);
//        req.setCallsQueue(queue);
////        expect(req.getRequestId()).andReturn(0l);
////        req.setRequestId(1);
//        req.setPositionInQueue(1);
//        req.fireCallQueuedEvent();
//        expect(req.isValid()).andReturn(Boolean.TRUE);
//        expect(req.logMess("Processing request...")).andReturn("");
//        req.incOperatorHops(); expectLastCall().anyTimes();
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        expect(req.getOperatorIndex()).andReturn(-1);
//        
//        expect(operatorRef.processRequest(queue, req)).andReturn(Boolean.TRUE);
//        req.setOperatorIndex(0);
//
//        replay(req, operatorRef, operator);
//        
//        assertTrue(queue.start());
//        selector.addOperatorRef(operatorRef);
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(0, queue.queue.size());
//        
//        verify(req, operatorRef, operator);
//    }
//    
//    @Test
//    public void leaveInQueueTest() throws Exception {
//        executor.stop();
//        
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOnBusyBehaviour onBusyBehaviour = createMock(CallsQueueOnBusyBehaviour.class);
//        
//        expect(req.getCallsQueue()).andReturn(null);
//        req.setCallsQueue(queue);
////        expect(req.getRequestId()).andReturn(0l);
////        req.setRequestId(1);
//        req.setPositionInQueue(1);
//        req.fireCallQueuedEvent();
//        expect(req.isValid()).andReturn(Boolean.TRUE);
//        expect(req.logMess("Processing request...")).andReturn("");
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        expect(req.getOperatorIndex()).andReturn(-1);
//        expect(req.getOnBusyBehaviour()).andReturn(null);
//        req.setOnBusyBehaviour(onBusyBehaviour);
//        expect(onBusyBehaviour.handleBehaviour(
//                isA(CallsQueue.class), isA(CallQueueRequestController.class))).andReturn(Boolean.TRUE);
//
//        replay(req, onBusyBehaviour);
//        
//        addPrioritySelector("selector 1", 1, onBusyBehaviour);
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(1, queue.queue.size());
//        
//        verify(req, onBusyBehaviour);
//    }
//    
//    @Test
//    public void orderChangeAfterProcessTest() throws Exception {
//        executor.stop();
//        
//        CallQueueRequestController req = createMock("req", CallQueueRequestController.class);
//        CallQueueRequestController req1 = createMock("req1", CallQueueRequestController.class);
//        CallsQueueOnBusyBehaviour onBusyBehaviour = createMock(CallsQueueOnBusyBehaviour.class);
//        
//        expect(req.getCallsQueue()).andReturn(null);
//        req.setCallsQueue(queue);
////        expect(req.getRequestId()).andReturn(0l);
////        req.setRequestId(1);
//        req.setPositionInQueue(1);
//        expectLastCall().anyTimes();
//        req.fireCallQueuedEvent();        
//        expect(req.isValid()).andReturn(Boolean.TRUE);
//        expect(req.logMess("Processing request...")).andReturn("");
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        expect(req.getOperatorIndex()).andReturn(-1);
//        expect(req.getOnBusyBehaviour()).andReturn(null);
//        req.setOnBusyBehaviour(onBusyBehaviour);
//        expect(onBusyBehaviour.handleBehaviour(
//                isA(CallsQueue.class), isA(CallQueueRequestController.class))).andReturn(Boolean.FALSE);
//        
//        expect(req1.getCallsQueue()).andReturn(null);
//        req1.setCallsQueue(queue);
////        expect(req1.getRequestId()).andReturn(0l);
////        req1.setRequestId(2);
//        expect(req1.getPriority()).andReturn(2).anyTimes();
//        req1.setPositionInQueue(2);
//        req1.fireCallQueuedEvent();
//        req1.setPositionInQueue(1);
//
//        replay(req, req1, onBusyBehaviour);
//        
//        addPrioritySelector("selector 1", 1, onBusyBehaviour);
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        queue.queueCall(req1);
//        assertEquals(2, queue.queue.size());
//        queue.processRequest();
//        assertEquals(1, queue.queue.size());
//        
//        verify(req, req1, onBusyBehaviour);
//    }
//
//    @Test
//    public void operatorIndexTest() throws Exception {
//        executor.stop();
//
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOperatorRef operatorRef = createMock("operatorRef1", CallsQueueOperatorRef.class);
//        CallsQueueOperatorRef operatorRef1 = createMock("operatorRef2", CallsQueueOperatorRef.class);
//        CallsQueueOperator operator = createMock(CallsQueueOperator.class);
//        CallsQueueOnBusyBehaviour onBusyBehaviour = createMock(CallsQueueOnBusyBehaviour.class);
//
//        expect(req.getCallsQueue()).andReturn(null).andReturn(queue).times(2);
//        req.setCallsQueue(queue); expectLastCall().times(3);
////        expect(req.getRequestId()).andReturn(0l);
////        expect(req.getRequestId()).andReturn(1l);
////        req.setRequestId(1);
//        req.setPositionInQueue(1); expectLastCall().times(3);
//        req.fireCallQueuedEvent(); expectLastCall().times(3);
//        expect(req.isValid()).andReturn(Boolean.TRUE).times(3);
//        expect(req.logMess("Processing request...")).andReturn("").times(3);
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        expect(req.getOperatorIndex()).andReturn(-1).andReturn(0).andReturn(1);
//        req.incOperatorHops(); expectLastCall().anyTimes();
//
//        expect(onBusyBehaviour.handleBehaviour(queue, req)).andReturn(Boolean.TRUE);
//
//        expect(operatorRef.processRequest(queue, req)).andReturn(Boolean.TRUE);
//        req.setOperatorIndex(0);
//        req.setOperatorIndex(1);
//        expect(req.getOnBusyBehaviour()).andReturn(onBusyBehaviour);
//        //expecting after second queueCall
//        expect(operatorRef1.processRequest(queue, req)).andReturn(Boolean.TRUE);
//
//        replay(req, operatorRef, operator, operatorRef1, onBusyBehaviour);
//
//        TestPrioritySelector selector = addPrioritySelector("selector 1", 1, onBusyBehaviour);
//        assertTrue(queue.start());
//        selector.addOperatorRef(operatorRef);
//        selector.addOperatorRef(operatorRef1);
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(0, queue.queue.size());
//
//        //return request to the queue
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(0, queue.queue.size());
//
//        //return request to the queue
//        queue.queueCall(req);
//        assertEquals(1, queue.queue.size());
//        queue.processRequest();
//        assertEquals(1, queue.queue.size());
//
//
//        verify(req, operatorRef, operator, operatorRef1, onBusyBehaviour);
//    }
//    
//    @Test
//    public void uniformOperatorUsageTest() throws Exception {
//        executor.stop();
//
//        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
//        selector.setName("selector 1");
//        queue.addAndSaveChildren(selector);
//        selector.setPriority(0);
//        selector.setOperatorsUsagePolicy(OperatorsUsagePolicy.UNIFORM_USAGE);
//        assertTrue(selector.start());
//
//        TestOperatorRef ref1 = new TestOperatorRef();
//        ref1.setName("ref1");
//        selector.addAndSaveChildren(ref1);
//        assertTrue(ref1.start());
//
//        TestOperatorRef ref2 = new TestOperatorRef();
//        ref2.setName("ref2");
//        selector.addAndSaveChildren(ref2);
//        assertTrue(ref2.start());
//
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOperatorRef operatorRef1 = createMock("operatorRef1", CallsQueueOperatorRef.class);
//        CallsQueueOperatorRef operatorRef2 = createMock("operatorRef2", CallsQueueOperatorRef.class);
//
//        expect(req.getCallsQueue()).andReturn(null).andReturn(queue).anyTimes();
//        req.setCallsQueue(queue); expectLastCall().anyTimes();
//        req.setPositionInQueue(1); expectLastCall().anyTimes();
//        req.fireCallQueuedEvent(); expectLastCall().anyTimes();
//        expect(req.isValid()).andReturn(Boolean.TRUE).anyTimes();
//        expect(req.logMess("Processing request...")).andReturn("").anyTimes();
//        req.setOperatorIndex(anyInt()); expectLastCall().anyTimes();
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        req.incOperatorHops(); expectLastCall().anyTimes();
//        expect(req.getOperatorHops()).andReturn(0).anyTimes();
////        expect(req.getOperatorIndex()).andReturn(-1).andReturn(0).andReturn(1);
//
//        expect(operatorRef1.processRequest(queue, req)).andReturn(Boolean.TRUE);
//        expect(operatorRef2.processRequest(queue, req)).andReturn(Boolean.TRUE);
//
//        replay(req, operatorRef1, operatorRef2);
//
//        ref1.setOperatorRef(operatorRef1);
//        ref2.setOperatorRef(operatorRef2);
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        queue.processRequest();
//        queue.queueCall(req);
//        queue.processRequest();
//
//        verify(req, operatorRef1, operatorRef2);
//    }
//
//    @Test
//    public void uniformOperatorUsageTest2() throws Exception {
//        executor.stop();
//
//        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
//        selector.setName("selector 1");
//        queue.addAndSaveChildren(selector);
//        selector.setPriority(0);
//        selector.setOperatorsUsagePolicy(OperatorsUsagePolicy.UNIFORM_USAGE);
//        assertTrue(selector.start());
//
//        TestOperatorRef ref1 = new TestOperatorRef();
//        ref1.setName("ref1");
//        selector.addAndSaveChildren(ref1);
//        assertTrue(ref1.start());
//
//        TestOperatorRef ref2 = new TestOperatorRef();
//        ref2.setName("ref2");
//        selector.addAndSaveChildren(ref2);
//        assertTrue(ref2.start());
//
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOperatorRef operatorRef1 = createMock("operatorRef1", CallsQueueOperatorRef.class);
//        CallsQueueOperatorRef operatorRef2 = createMock("operatorRef2", CallsQueueOperatorRef.class);
//
//        expect(req.getCallsQueue()).andReturn(null).andReturn(queue).anyTimes();
//        req.setCallsQueue(queue); expectLastCall().anyTimes();
//        req.setPositionInQueue(1); expectLastCall().anyTimes();
//        req.fireCallQueuedEvent(); expectLastCall().anyTimes();
//        expect(req.isValid()).andReturn(Boolean.TRUE).anyTimes();
//        expect(req.logMess("Processing request...")).andReturn("").anyTimes();
//        req.setOperatorIndex(anyInt()); expectLastCall().anyTimes();
//        req.fireRejectedQueueEvent();
//        expect(req.getOnBusyBehaviour()).andReturn(null);
//        expect(req.getOnBusyBehaviourStep()).andReturn(0);
//        req.addToLog("reached the end of the \"on busy behaviour steps\" sequence");
//        req.setOnBusyBehaviour(isA(CallsQueueOnBusyBehaviour.class));
//        req.setOnBusyBehaviourStep(1);
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        req.incOperatorHops(); expectLastCall().anyTimes();
//        expect(req.getOperatorHops()).andReturn(0).anyTimes();
////        expect(req.getOperatorIndex()).andReturn(-1).andReturn(0).andReturn(1);
//
//        expect(operatorRef1.processRequest(queue, req)).andReturn(Boolean.FALSE);
//        expect(operatorRef2.processRequest(queue, req)).andReturn(Boolean.FALSE);
//
//        replay(req, operatorRef1, operatorRef2);
//
//        ref1.setOperatorRef(operatorRef1);
//        ref2.setOperatorRef(operatorRef2);
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        queue.processRequest();
//
//        verify(req, operatorRef1, operatorRef2);
//    }
//
//    @Test
//    public void uniformOperatorUsageTest3() throws Exception {
//        executor.stop();
//
//        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
//        selector.setName("selector 1");
//        queue.addAndSaveChildren(selector);
//        selector.setPriority(0);
//        selector.setOperatorsUsagePolicy(OperatorsUsagePolicy.UNIFORM_USAGE);
//        assertTrue(selector.start());
//
//        TestOperatorRef ref1 = new TestOperatorRef();
//        ref1.setName("ref1");
//        selector.addAndSaveChildren(ref1);
//        assertTrue(ref1.start());
//
//        TestOperatorRef ref2 = new TestOperatorRef();
//        ref2.setName("ref2");
//        selector.addAndSaveChildren(ref2);
//        assertTrue(ref2.start());
//
//        CallQueueRequestController req = createMock(CallQueueRequestController.class);
//        CallsQueueOperatorRef operatorRef1 = createMock("operatorRef1", CallsQueueOperatorRef.class);
//        CallsQueueOperatorRef operatorRef2 = createMock("operatorRef2", CallsQueueOperatorRef.class);
//
//        expect(req.getCallsQueue()).andReturn(null).andReturn(queue).anyTimes();
//        req.setCallsQueue(queue); expectLastCall().anyTimes();
//        req.setPositionInQueue(1); expectLastCall().anyTimes();
//        req.fireCallQueuedEvent(); expectLastCall().anyTimes();
//        expect(req.isValid()).andReturn(Boolean.TRUE).anyTimes();
//        expect(req.logMess("Processing request...")).andReturn("").anyTimes();
//        req.setOperatorIndex(anyInt()); expectLastCall().anyTimes();
//        req.fireRejectedQueueEvent();
//        expect(req.getOnBusyBehaviour()).andReturn(null);
//        expect(req.getOnBusyBehaviourStep()).andReturn(0);
//        req.addToLog("reached the end of the \"on busy behaviour steps\" sequence");
//        req.setOnBusyBehaviour(isA(CallsQueueOnBusyBehaviour.class));
//        req.setOnBusyBehaviourStep(1);
//        expect(req.getPriority()).andReturn(1).anyTimes();
//        req.incOperatorHops(); expectLastCall().anyTimes();
//        expect(req.getOperatorHops()).andReturn(0).andReturn(1).andReturn(2);
////        expect(req.getOperatorIndex()).andReturn(-1).andReturn(0).andReturn(1);
//
//        expect(operatorRef1.processRequest(queue, req)).andReturn(Boolean.TRUE);
//        expect(operatorRef2.processRequest(queue, req)).andReturn(Boolean.TRUE);
//
//        replay(req, operatorRef1, operatorRef2);
//
//        ref1.setOperatorRef(operatorRef1);
//        ref2.setOperatorRef(operatorRef2);
//        assertTrue(queue.start());
//        queue.queueCall(req);
//        queue.processRequest();
//        queue.queueCall(req);
//        queue.processRequest();
//        queue.queueCall(req);
//        queue.processRequest();
//
//        verify(req, operatorRef1, operatorRef2);
//    }

    private TestPrioritySelector addPrioritySelector(
            String name, int priority, CallsQueueOnBusyBehaviour onBusyBehaviour)
    {
        TestPrioritySelector selector = new TestPrioritySelector(name, priority, onBusyBehaviour);
        queue.addAndSaveChildren(selector);
        assertTrue(selector.start());
        return selector;
    }
    
    private void trainRequest(final CallQueueRequestController req) {
        new Expectations() {{
            req.logMess(anyString, any); minTimes = 0; result = new Delegate<String>() {
                public String logMess(String format, Object... args) {
                    System.out.println("DELIGATING!!!");
                    return String.format(format, args);
                }
            };
            req.logMess(anyString); minTimes = 0; result = new Delegate<String>() {
                public String logMess(String format, Object... args) {
                    System.out.println("DELIGATING!!!");
                    return format;
                }
            };
        }};
    }
    
//    private void trainRequest(CallQueueRequestController req) {
//        Answer<String> logAnswer = new Answer<String>() {
//            @Override
//            public String answer(InvocationOnMock inv) throws Throwable {
//                
//                System.out.println("ARGS SIZE: "+inv.getArguments().length);
////                if (inv.getArguments().length==2)
//                    return String.format(inv.getArgumentAt(0, String.class), inv.getArgumentAt(1, Object.class));
////                else
////                    return inv.getArgumentAt(0, String.class);
//            }
//        };
////        for (int i=0; i<100; i++)
//            when(req.logMess(anyString(), any())).thenAnswer(new Answer<String>() {
//            @Override
//            public String answer(InvocationOnMock inv) throws Throwable {
//                
//                System.out.println("ARGS SIZE: "+inv.getArguments().length);
////                if (inv.getArguments().length==2)
//                    return String.format(inv.getArgumentAt(0, String.class), inv.getArgumentAt(1, Object.class));
////                else
////                    return inv.getArgumentAt(0, String.class);
//            }
//        }).thenAnswer(logAnswer);
//        
//    }
    
}
