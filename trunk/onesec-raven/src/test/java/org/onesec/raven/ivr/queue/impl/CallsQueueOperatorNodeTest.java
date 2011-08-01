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

import java.util.Map;
import org.apache.poi.hssf.record.ScenarioProtectRecord;
import org.easymock.IArgumentMatcher;
import org.onesec.raven.ivr.EndpointRequest;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import static org.easymock.EasyMock.*;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueOperatorNodeTest extends OnesecRavenTestCase
{
    private CallsQueueOperatorNode operator;
    private TestConversationsBridgeManager bridgeManager;
    private TestEndpointPool endpointPool;
    private IvrConversationScenarioNode scenario;

    @Before
    public void prepare()
    {
        bridgeManager = new TestConversationsBridgeManager();
        bridgeManager.setName("conversations bridge");
        tree.getRootNode().addAndSaveChildren(bridgeManager);
        assertTrue(bridgeManager.start());
        
        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        endpointPool = new TestEndpointPool();
        endpointPool.setName("endpoint pool");
        tree.getRootNode().addAndSaveChildren(endpointPool);
        assertTrue(endpointPool.start());

        operator = new CallsQueueOperatorNode();
        operator.setName("operator");
        tree.getRootNode().addAndSaveChildren(operator);
        operator.setConversationScenario(scenario);
        operator.setConversationsBridgeManager(bridgeManager);
        operator.setEndpointPool(endpointPool);
        operator.setPhoneNumbers("88024");
    }

    @Test
    public void requestOnStoppedNode()
    {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        replay(request, queue);

        assertFalse(operator.processRequest(queue, request));

        verify(request, queue);
    }
    
    @Test
    public void noFreeEndpointInThePoolTest() throws ExecutorServiceException
    {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        
        request.addToLog("handling by operator (operator)");
        pool.requestEndpoint(sendNullEndpoint());
        request.addToLog("no free endpoints in the pool");
        queue.queueCall(request);

        replay(request, queue, pool);

        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        operator.processRequest(queue, request);
        
        verify(request, queue, pool);
    }

    @Test
    public void inviteTimeoutTest() throws Exception
    {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint endpoint = createMock(IvrEndpoint.class);
        IvrEndpointState endpointState = createMock(IvrEndpointState.class);
        StateWaitResult stateWaitResult = createMock(StateWaitResult.class);

        request.addToLog("handling by operator (operator)");
        pool.requestEndpoint(sendEndpoint(endpoint));
        endpoint.invite(eq("88024"), same(scenario), same(operator), checkBindings(operator, request));
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(endpoint.getEndpointState()).andReturn(endpointState).anyTimes();
        expect(endpointState.getId()).andReturn(IvrEndpointState.INVITING).anyTimes();
        request.addToLog("number (88024) not answer");
        endpoint.stop();
        expect(endpoint.start()).andReturn(Boolean.TRUE);
        expect(endpointState.waitForState(aryEq(new int[]{IvrEndpointState.IN_SERVICE}), eq(10000l)))
                .andReturn(stateWaitResult);
        expect(stateWaitResult.isWaitInterrupted()).andReturn(Boolean.FALSE);
        request.addToLog("operator (operator) didn't handle a call");
        queue.queueCall(request);

        replay(request, queue, pool, endpoint, endpointState, stateWaitResult);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        long startTime = System.currentTimeMillis();
        operator.processRequest(queue, request);
        long endTime = System.currentTimeMillis();
        assertTrue(endTime-startTime>5000);
        assertTrue(endTime-startTime<7000);

        verify(request, queue, pool, endpoint, endpointState, stateWaitResult);
    }

    public static Map<String, Object> checkBindings(
            final CallsQueueOperator operator, final CallQueueRequestWrapper request)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Map<String, Object> bindings = (Map<String, Object>) argument;
                assertSame(bindings.get(CallsQueueOperator.CALL_QUEUE_OPERATOR_BINDING), operator);
                assertSame(bindings.get(CallsQueueOperator.CALL_QUEUE_REQUEST_BINDING), request);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static EndpointRequest sendNullEndpoint()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                EndpointRequest req = (EndpointRequest) arg;
                req.processRequest(null);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static EndpointRequest sendEndpoint(final IvrEndpoint endpoint)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                EndpointRequest req = (EndpointRequest) arg;
                req.processRequest(endpoint);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}