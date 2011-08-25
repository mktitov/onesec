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
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import static org.easymock.EasyMock.*;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestWrapperImplTest extends OnesecRavenTestCase
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
        expect(req.getConversation()).andReturn(conv).atLeastOnce();
        expect(conv.getCallingNumber()).andReturn("1234").atLeastOnce();
        expect(conv.getObjectName()).andReturn("[7000]").anyTimes();
        expect(state.getId()).andReturn(IvrEndpointConversationState.TALKING);
        conv.addConversationListener(isA(IvrEndpointConversationListener.class));

        CallQueuedEventImpl queuedEvent = new CallQueuedEventImpl(queue, 1, "q1");
        req.callQueueChangeEvent(queuedEvent);

        replay(req, conv, queue);

        CallQueueRequestWrapperImpl wrapper = new CallQueueRequestWrapperImpl(callsQueues, req, null, 1);
        assertTrue(wrapper.isValid());
        wrapper.callQueueChangeEvent(queuedEvent);

        verify(req, conv, queue);
    }
    
    @Test
    public void invalidTest() throws Exception
    {
        CallQueueRequest request = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock((IvrEndpointConversation.class));
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointConversationState state = createMock(IvrEndpointConversationState.class);
        
        expect(request.getConversation()).andReturn(conv).times(2);
        expect(conv.getCallingNumber()).andReturn("1234").atLeastOnce();
        expect(conv.getObjectName()).andReturn("[7000]").anyTimes();
        conv.addConversationListener(callListenerAdded());
        expect(conv.getState()).andReturn(state);
        expect(state.getId()).andReturn(IvrEndpointConversationState.INVALID);
        request.callQueueChangeEvent(isA(DisconnectedQueueEvent.class));
        
        replay(request, conv, queue, state);
        
        CallQueueRequestWrapper wrapper = new CallQueueRequestWrapperImpl(callsQueues, request, null, 1);
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
}