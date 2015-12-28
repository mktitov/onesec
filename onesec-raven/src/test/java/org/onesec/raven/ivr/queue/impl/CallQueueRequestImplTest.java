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

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
//import static org.easymock.EasyMock.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.event.CommutatedQueueEvent;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.impl.LoggerHelper;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class CallQueueRequestImplTest
{
    @Test
    public void cancelTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final CallQueueRequestListener listener,
            @Mocked final CommutatedQueueEvent commutatedEvent,
            @Mocked final LoggerHelper logger
    ) throws Exception
    {
        CallQueueRequestImpl req = new CallQueueRequestImpl(conv, 1, "1", null, true, true, null, logger);
        req.addRequestListener(listener);
        req.cancel();
        new Verifications() {{
            listener.requestCanceled("CANCELED"); times = 1;
        }};
    }    
    
    @Test
    public void commutatedTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ExecutorService executor,
            @Mocked final CallQueueRequestListener listener,
            @Mocked final CommutatedQueueEvent commutatedEvent,
            @Mocked final LoggerHelper logger
    ) throws Exception
    {
        trainExecutor(executor);
        CallQueueRequestImpl req = new CallQueueRequestImpl(conv, 1, "1", null, true, true, null, logger);
        req.addRequestListener(listener);
        req.callQueueChangeEvent(commutatedEvent);
        new Verifications() {{
            listener.commutated(); times = 1;
        }};
    }
    
    @Test
    public void alreadyCommutatedTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ExecutorService executor,
            @Mocked final CallQueueRequestListener listener,
            @Mocked final CommutatedQueueEvent commutatedEvent,
            @Mocked final LoggerHelper logger
    ) throws Exception
    {
        trainExecutor(executor);
        CallQueueRequestImpl req = new CallQueueRequestImpl(conv, 1, "1", null, true, true, null, logger);
        req.callQueueChangeEvent(commutatedEvent);
        req.addRequestListener(listener);
        new Verifications() {{
            listener.commutated(); times = 1;
        }};
    }
    
    @Test
    public void disconnectedTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ExecutorService executor,
            @Mocked final CallQueueRequestListener listener,
            @Mocked final DisconnectedQueueEvent disconnectedEvent,
            @Mocked final LoggerHelper logger
    ) throws Exception
    {
        trainExecutor(executor);
        CallQueueRequestImpl req = new CallQueueRequestImpl(conv, 1, "1", null, true, true, null, logger);
        req.callQueueChangeEvent(disconnectedEvent);
        req.addRequestListener(listener);
        new Verifications() {{
            listener.disconnected(); times = 1;
        }};
    }
    
    @Test
    public void alreadyDisconnectedTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ExecutorService executor,
            @Mocked final CallQueueRequestListener listener,
            @Mocked final DisconnectedQueueEvent disconnectedEvent,
            @Mocked final LoggerHelper logger
    ) throws Exception
    {
        trainExecutor(executor);
        CallQueueRequestImpl req = new CallQueueRequestImpl(conv, 1, "1", null, true, true, null, logger);
        req.addRequestListener(listener);
        req.callQueueChangeEvent(disconnectedEvent);
        new Verifications() {{
            listener.disconnected(); times = 1;
        }};
    }
    
    private void trainExecutor(final ExecutorService executor) {
        new Expectations() {{
            executor.executeQuietly((Task) any); result = new Delegate() {
                public boolean executeQuietly(Task task) {
                    task.run();
                    return true;
                }
            };
        }};
    }
}