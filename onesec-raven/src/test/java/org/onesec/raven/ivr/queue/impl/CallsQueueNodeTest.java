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

import javax.print.attribute.standard.QueuedJobCount;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.event.CallQueuedEvent;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
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
        req.setCallsQueue(queue);
        req.setRequestId(1);
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
        req.setRequestId(1);
        expect(req.getPriority()).andReturn(1).anyTimes();
        expect(req.getRequestId()).andReturn(1l).anyTimes();
        req.setPositionInQueue(1);
        req.fireCallQueuedEvent();
        
        req1.setCallsQueue(queue);
        req1.setRequestId(2);
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
}
