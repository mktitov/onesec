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

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.ds.impl.RecordSchemaNode;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
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
    public void nullQueueIdTest()
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getQueueId()).andReturn(null);
        expect(req.getConversation()).andReturn(conv);
        expect(conv.getObjectName()).andReturn("call info");
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
        expect(req.getConversation()).andReturn(conv);
        expect(conv.getObjectName()).andReturn("call info");
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        assertTrue(queues.start());
        
        ds.pushData(req);
        
        verify(req, conv);
    }
    
    @Test
    public void queueNotFound2()
    {
        TestCallsQueueNode queue = new TestCallsQueueNode();
        queue.setName("queue");
        queues.addAndSaveChildren(queue);
        
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        
        expect(req.getQueueId()).andReturn("queue").anyTimes();
        expect(req.getConversation()).andReturn(conv);
        expect(conv.getObjectName()).andReturn("call info");
        req.callQueueChangeEvent(isA(RejectedQueueEvent.class));
        
        replay(req, conv);
        
        assertTrue(queues.start());
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
        queues.addAndSaveChildren(queue);
        assertTrue(queue.start());
        
        CallQueueRequest req = createMock(CallQueueRequest.class);
        
        expect(req.getQueueId()).andReturn("queue").anyTimes();
        
        replay(req);
        
        ds.pushData(req);
        assertNotNull(queue.lastRequest);
        assertSame(req, queue.lastRequest.getWrappedRequest());
        
        verify(req);
    }
}