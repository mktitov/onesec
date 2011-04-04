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
import org.onesec.raven.ivr.queue.CallQueueRequest;
import static org.easymock.EasyMock.*;
import org.raven.ds.RecordException;

/**
 *
 * @author Mikhail Titov
 */
public class CallQueueRequestWrapperImplTest extends OnesecRavenTestCase
{
    private CallsQueuesNode callsQueues;
    private CallQueueCdrRecordSchemaNode schema;

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
        assertTrue(callsQueues.start());
    }

    @Test
    public void test() throws RecordException
    {
        CallQueueRequest req = createMock(CallQueueRequest.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        expect(req.getConversation()).andReturn(conv).atLeastOnce();
        expect(conv.getCallingNumber()).andReturn("1234").atLeastOnce();
        expect(conv.getObjectName()).andReturn("[1]").atLeastOnce();

        replay(req, conv);

        CallQueueRequestWrapperImpl wrapper = new CallQueueRequestWrapperImpl(callsQueues, req);

        verify(req, conv);
    }
}