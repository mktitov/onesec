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

import org.easymock.IArgumentMatcher;
import org.onesec.raven.ivr.queue.event.impl.CallQueuedEventImpl;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.onesec.raven.ivr.queue.event.impl.CommutatedQueueEventImpl;
import org.onesec.raven.ivr.queue.event.impl.DisconnectedQueueEventImpl;
import org.onesec.raven.ivr.queue.event.impl.OperatorQueueEventImpl;
import org.onesec.raven.ivr.queue.event.impl.RejectedQueueEventImpl;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.impl.DataContextImpl;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestControllerImplTest extends OnesecRavenTestCase
{
    private CallsQueuesNode callsQueues;
    private CallQueueCdrRecordSchemaNode schema;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        schema = new CallQueueCdrRecordSchemaNode();
        schema.setName("cdr schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        callsQueues = new CallsQueuesNode();
        callsQueues.setName("queues");
        tree.getRootNode().addAndSaveChildren(callsQueues);
        callsQueues.setCdrRecordSchema(schema);
        callsQueues.setLogLevel(LogLevel.DEBUG);
        assertTrue(callsQueues.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(callsQueues);
        assertTrue(collector.start());
    }

    @Test
    public void validRequestTest() throws RecordException
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        IvrEndpointConversationState state = createMock(IvrEndpointConversationState.class);
        CallsQueue queue = createMock(CallsQueue.class);
        
        expect(req.getConversationInfo()).andReturn("").anyTimes();
        req.addRequestListener(isA(CallQueueRequestListener.class));
        expect(req.isCanceled()).andReturn(false).anyTimes();
        expect(req.getPriority()).andReturn(1);
        expect(conv.getObjectName()).andReturn("[7000]").anyTimes();
        expect(state.getId()).andReturn(IvrEndpointConversationState.TALKING);

        CallQueuedEventImpl queuedEvent = new CallQueuedEventImpl(queue, 1, "q1");
        req.callQueueChangeEvent(queuedEvent);

        replay(req, conv, queue);

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(queuedEvent);

        verify(req, conv, queue);
    }
    
    @Test
    public void sendQueuedEventTest() throws RecordException
    {
        callsQueues.stop();
        callsQueues.setPermittedEventTypes(CallsQueuesNode.QUEUED_EVENT);
        assertTrue(callsQueues.start());
        
        EventTestHelper control = new EventTestHelper();

        CallQueuedEventImpl queuedEvent = new CallQueuedEventImpl(control.queue, 1, "q1");
        control.req.callQueueChangeEvent(queuedEvent);
        
        control.control.replay();

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, control.req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(queuedEvent);

        control.control.verify();
        
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertTrue(rec.containsTag("eventType"));
        assertEquals(CallsQueuesNode.QUEUED_EVENT, rec.getTag("eventType"));
    }
    
    @Test
    public void assignedToOperatorEventTest() throws RecordException {
        callsQueues.stop();
        callsQueues.setPermittedEventTypes(CallsQueuesNode.ASSIGNED_TO_OPERATOR_EVENT);
        assertTrue(callsQueues.start());
        
        EventTestHelper control = new EventTestHelper();
        
        OperatorQueueEventImpl operatorEvent = new OperatorQueueEventImpl(control.queue, 1, "operator", 
            "person", "person desc");
        control.req.callQueueChangeEvent(operatorEvent);

        control.control.replay();

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, control.req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(operatorEvent);

        control.control.verify();
        
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertTrue(rec.containsTag("eventType"));
        assertEquals(CallsQueuesNode.ASSIGNED_TO_OPERATOR_EVENT, rec.getTag("eventType"));
    }
    
    @Test
    public void conversationStartedEventTest() throws RecordException {
        callsQueues.stop();
        callsQueues.setPermittedEventTypes(CallsQueuesNode.CONVERSATION_STARTED_EVENT);
        assertTrue(callsQueues.start());
        
        EventTestHelper control = new EventTestHelper();
        
        CommutatedQueueEventImpl event = new CommutatedQueueEventImpl(control.queue, 1);
        control.req.callQueueChangeEvent(event);

        control.control.replay();

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, control.req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(event);

        control.control.verify();
        
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertTrue(rec.containsTag("eventType"));
        assertEquals(CallsQueuesNode.CONVERSATION_STARTED_EVENT, rec.getTag("eventType"));
    }
    
    @Test
    public void callFinishedEventTest() throws RecordException {
        callsQueues.stop();
        callsQueues.setPermittedEventTypes(CallsQueuesNode.CALL_FINISHED_EVENT);
        assertTrue(callsQueues.start());
        
        EventTestHelper control = new EventTestHelper();
       
        DisconnectedQueueEventImpl event = new DisconnectedQueueEventImpl(control.queue, 1);
        control.req.callQueueChangeEvent(event);
        expectLastCall().times(2);

        control.control.replay();

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, control.req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(event);
        wrapper.callQueueChangeEvent(event);

        control.control.verify();
        
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertTrue(rec.containsTag("eventType"));
        assertEquals(CallsQueuesNode.CALL_FINISHED_EVENT, rec.getTag("eventType"));
    }
    
    @Test
    public void callFinishedEvent2Test() throws RecordException {
        callsQueues.stop();
        callsQueues.setPermittedEventTypes(CallsQueuesNode.CALL_FINISHED_EVENT);
        assertTrue(callsQueues.start());
        
        EventTestHelper control = new EventTestHelper();
       
        RejectedQueueEventImpl event = new RejectedQueueEventImpl(control.queue, 1);
        control.req.callQueueChangeEvent(event);

        control.control.replay();

        CallQueueRequestControllerImpl wrapper = new CallQueueRequestControllerImpl(callsQueues, control.req, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(event);

        control.control.verify();
        
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertTrue(rec.containsTag("eventType"));
        assertEquals(CallsQueuesNode.CALL_FINISHED_EVENT, rec.getTag("eventType"));
    }
    
    @Test
    public void invalidTest() throws Exception {
        CallQueueRequest request = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock((IvrEndpointConversation.class));
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointConversationState state = createMock(IvrEndpointConversationState.class);
        
        expect(request.getConversationInfo()).andReturn("").anyTimes();
        request.addRequestListener(isA(CallQueueRequestListener.class));
        expect(request.isCanceled()).andReturn(true).anyTimes();
        expect(request.getPriority()).andReturn(1);
        expect(conv.getObjectName()).andReturn("[7000]").anyTimes();
        
        replay(request, conv, queue, state);
        
        CallQueueRequestController wrapper = new CallQueueRequestControllerImpl(callsQueues, request, 1);
        assertFalse(wrapper.isValid());
        
        verify(request, conv, queue, state);
    }
    
    public static IvrEndpointConversationListener callListenerAdded()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                ((IvrEndpointConversationListener)argument).listenerAdded(null);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    private class EventTestHelper  {
        private final IMocksControl control;
        private final CallQueueRequest req;
        private final IvrEndpointConversation conv;
        private final IvrEndpointConversationState state;
        private final CallsQueue queue;
        private final DataContext context;
        
        public EventTestHelper() {
            control = createControl();
            context = new DataContextImpl();
            req = control.createMock(CallQueueRequest.class);
            conv = control.createMock(IvrEndpointConversation.class);
            state = control.createMock(IvrEndpointConversationState.class);
            queue = control.createMock(CallsQueue.class);
            expect(req.getConversationInfo()).andReturn("").anyTimes();
            req.addRequestListener(isA(CallQueueRequestListener.class));
            expect(req.isCanceled()).andReturn(false).anyTimes();
            expect(req.getPriority()).andReturn(1);
            expect(conv.getObjectName()).andReturn("[7000]").anyTimes();
//            expect(state.getId()).andReturn(IvrEndpointConversationState.TALKING);
            expect(req.getContext()).andReturn(context);
       }
    }
}