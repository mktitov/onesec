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

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueNodeTest extends RavenCoreTestCase
{
    private CallsQueueNode queue;
    private ExecutorServiceNode executor;
    
    
    @Before
    public void prepare()
    {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());
        
        queue = new CallsQueueNode();
        queue.setName("queue");
        tree.getRootNode().addAndSaveChildren(queue);
        queue.setExecutor(executor);
    }
    
    @Test
    public void successQueued()
    {
        executor.stop();
        
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();
        replay(req);
        
        assertTrue(queue.start());
        queue.queueCall(req);
        
        verify(req);
    }
    
    @Test
    public void rejectedByMaxQueueSize()
    {
        executor.stop();
        
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        CallQueueRequestWrapper req1 = createMock(CallQueueRequestWrapper.class);
        req.setCallsQueue(queue);
        expect(req.getCallsQueue()).andReturn(null);
//        req.setRequestId(1);
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getRequestId()).andReturn(1l).anyTimes();
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();
        
        expect(req1.getCallsQueue()).andReturn(null);
        req1.setCallsQueue(queue);
//        req1.setRequestId(2);
        expect(req1.getPriority()).andReturn(1).anyTimes();
        expect(req1.getRequestId()).andReturn(2l).anyTimes();
        req1.addToLog(eq("queue size was exceeded"));
        req1.fireRejectedQueueEvent();
        
        replay(req, req1);
        
        queue.setMaxQueueSize(1);
        assertTrue(queue.start());
        queue.queueCall(req);
        queue.queueCall(req1);
        
        verify(req, req1);
    }
    
    @Test(timeout=5000)
    public void rejectedByNotFoundPrioritySelector() throws InterruptedException
    {
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();
        req.addToLog("not found priority selector");
        req.fireRejectedQueueEvent();
        replay(req);

        queue.setLogLevel(LogLevel.NONE);
        assertTrue(queue.start());
        queue.queueCall(req);
//        queue.run();
        TimeUnit.MILLISECONDS.sleep(500);
        queue.stop();
        assertEquals(Node.Status.INITIALIZED, queue.getStatus());
        while (queue.processingThreadRunning.get())
            TimeUnit.MILLISECONDS.sleep(100);
        
        verify(req);
    }
    
    @Test
    public void rejectedByNoOperatorsNoOnBusyBehaviour() throws InterruptedException
    {
        executor.stop();
        
        CallsQueuePrioritySelectorNode selector = new CallsQueuePrioritySelectorNode();
        selector.setName("selector 1");
        queue.addAndSaveChildren(selector);
        selector.setPriority(1);
        assertTrue(selector.start());
        
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        expect(req.getRequestId()).andReturn(0l);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        expect(req.getPriority()).andReturn(1).anyTimes();
        req.fireCallQueuedEvent();
        expect(req.getOperatorIndex()).andReturn(-1).anyTimes();
        expect(req.getOnBusyBehaviour()).andReturn(null);
        req.setOnBusyBehaviour(isA(CallsQueueOnBusyBehaviour.class));
        expect(req.getOnBusyBehaviourStep()).andReturn(0);
        req.setOnBusyBehaviourStep(1);
        req.addToLog(CallsQueueOnBusyBehaviourNode.REACHED_THE_END_OF_SEQ);
        req.fireRejectedQueueEvent();
        replay(req);
        
        assertTrue(queue.start());
        queue.queueCall(req);
        assertEquals(1, queue.queue.size());
        queue.processRequest();
        assertEquals(0, queue.queue.size());
        
        verify(req);
    }
    
    @Test
    public void processedByOperatorTest() throws Exception
    {
        executor.stop();
        TestPrioritySelector selector = addPrioritySelector("selector 1", 1, null);
        
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        CallsQueueOperatorRef operatorRef = createMock(CallsQueueOperatorRef.class);
        CallsQueueOperator operator = createMock(CallsQueueOperator.class);
        
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        expect(req.getRequestId()).andReturn(0l);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();        
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getOperatorIndex()).andReturn(-1);
        
        expect(operatorRef.processRequest(queue, req)).andReturn(Boolean.TRUE);
        req.setOperatorIndex(0);

        replay(req, operatorRef, operator);
        
        assertTrue(queue.start());
        selector.addOperatorRef(operatorRef);
        queue.queueCall(req);
        assertEquals(1, queue.queue.size());
        queue.processRequest();
        assertEquals(0, queue.queue.size());
        
        verify(req, operatorRef, operator);
    }
    
    @Test
    public void leaveInQueueTest() throws Exception
    {
        executor.stop();
        
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        CallsQueueOnBusyBehaviour onBusyBehaviour = createMock(CallsQueueOnBusyBehaviour.class);
        
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        expect(req.getRequestId()).andReturn(0l);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getOperatorIndex()).andReturn(-1);
        expect(req.getOnBusyBehaviour()).andReturn(null);
        req.setOnBusyBehaviour(onBusyBehaviour);
        expect(onBusyBehaviour.handleBehaviour(
                isA(CallsQueue.class), isA(CallQueueRequestWrapper.class))).andReturn(Boolean.TRUE);

        replay(req, onBusyBehaviour);
        
        addPrioritySelector("selector 1", 1, onBusyBehaviour);
        assertTrue(queue.start());
        queue.queueCall(req);
        assertEquals(1, queue.queue.size());
        queue.processRequest();
        assertEquals(1, queue.queue.size());
        
        verify(req, onBusyBehaviour);
    }
    
    @Test
    public void orderChangeAfterProcessTest() throws Exception
    {
        executor.stop();
        
        CallQueueRequestWrapper req = createMock("req", CallQueueRequestWrapper.class);
        CallQueueRequestWrapper req1 = createMock("req1", CallQueueRequestWrapper.class);
        CallsQueueOnBusyBehaviour onBusyBehaviour = createMock(CallsQueueOnBusyBehaviour.class);
        
        expect(req.getCallsQueue()).andReturn(null);
        req.setCallsQueue(queue);
//        expect(req.getRequestId()).andReturn(0l);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        expectLastCall().anyTimes();
        req.fireCallQueuedEvent();        
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getOperatorIndex()).andReturn(-1);
        expect(req.getOnBusyBehaviour()).andReturn(null);
        req.setOnBusyBehaviour(onBusyBehaviour);
        expect(onBusyBehaviour.handleBehaviour(
                isA(CallsQueue.class), isA(CallQueueRequestWrapper.class))).andReturn(Boolean.FALSE);
        
        expect(req1.getCallsQueue()).andReturn(null);
        req1.setCallsQueue(queue);
//        expect(req1.getRequestId()).andReturn(0l);
//        req1.setRequestId(2);
        expect(req1.getPriority()).andReturn(2).anyTimes();
        req1.setPositionInQueue(2);
        req1.fireCallQueuedEvent();
        req1.setPositionInQueue(1);

        replay(req, req1, onBusyBehaviour);
        
        addPrioritySelector("selector 1", 1, onBusyBehaviour);
        assertTrue(queue.start());
        queue.queueCall(req);
        queue.queueCall(req1);
        assertEquals(2, queue.queue.size());
        queue.processRequest();
        assertEquals(1, queue.queue.size());
        
        verify(req, req1, onBusyBehaviour);
    }

    @Test
    public void operatorIndexTest() throws Exception
    {
        executor.stop();
        TestPrioritySelector selector = addPrioritySelector("selector 1", 1, null);

        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        CallsQueueOperatorRef operatorRef = createMock("operatorRef1", CallsQueueOperatorRef.class);
        CallsQueueOperatorRef operatorRef1 = createMock("operatorRef2", CallsQueueOperatorRef.class);
        CallsQueueOperator operator = createMock(CallsQueueOperator.class);

        expect(req.getCallsQueue()).andReturn(null).andReturn(queue);
        req.setCallsQueue(queue);
        expectLastCall().times(2);
//        expect(req.getRequestId()).andReturn(0l);
//        expect(req.getRequestId()).andReturn(1l);
//        req.setRequestId(1);
        req.setPositionInQueue(1);
        expectLastCall().times(2);
        req.fireCallQueuedEvent();
        expectLastCall().times(2);
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getOperatorIndex()).andReturn(-1);
        expect(req.getOperatorIndex()).andReturn(0);

        expect(operatorRef.processRequest(queue, req)).andReturn(Boolean.TRUE);
        req.setOperatorIndex(0);
        req.setOperatorIndex(1);
        //expecting after second queueCall
        expect(operatorRef1.processRequest(queue, req)).andReturn(Boolean.TRUE);

        replay(req, operatorRef, operator, operatorRef1);

        assertTrue(queue.start());
        selector.addOperatorRef(operatorRef);
        selector.addOperatorRef(operatorRef1);
        queue.queueCall(req);
        assertEquals(1, queue.queue.size());
        queue.processRequest();
        assertEquals(0, queue.queue.size());

        //return request to the queue
        queue.queueCall(req);
        assertEquals(1, queue.queue.size());
        queue.processRequest();
        assertEquals(0, queue.queue.size());


        verify(req, operatorRef, operator, operatorRef1);
    }
    
    private TestPrioritySelector addPrioritySelector(
            String name, int priority, CallsQueueOnBusyBehaviour onBusyBehaviour)
    {
        TestPrioritySelector selector = new TestPrioritySelector(name, priority, onBusyBehaviour);
        queue.addAndSaveChildren(selector);
        assertTrue(selector.start());
        return selector;
    }
    
}
