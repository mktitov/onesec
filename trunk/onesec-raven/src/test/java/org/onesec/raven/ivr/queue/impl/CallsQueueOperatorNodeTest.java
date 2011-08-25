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
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.onesec.raven.ivr.EndpointRequest;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.sched.ExecutorServiceException;
import static org.easymock.EasyMock.*;

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
        endpoint.invite(eq("88024"), same(scenario), isA(CallsCommutationManagerImpl.class)
                , checkBindings(operator, request));
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

    @Test
    public void commutateTest() throws Exception
    {
        final CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        IvrEndpointConversation abonentConversation = createMock(
                "abonentConversation", IvrEndpointConversation.class);
        IvrEndpointConversation operatorConversation = createMock(
                "operatorConversation", IvrEndpointConversation.class);
        final CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint operatorEndpoint = createMock(IvrEndpoint.class);
        IvrEndpointState endpointState = createMock(IvrEndpointState.class);
        IvrConversationsBridgeManager manager = createMock(IvrConversationsBridgeManager.class);
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        ConversationResult conversationResult = createMock(ConversationResult.class);

        request.addToLog("handling by operator (operator)");
        pool.requestEndpoint(sendEndpoint(operatorEndpoint));
        operatorEndpoint.invite(eq("88024"), same(scenario), isA(CallsCommutationManagerImpl.class)
                , checkBindings(operator, request));
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(operatorEndpoint.getEndpointState()).andReturn(endpointState).anyTimes();
        expect(endpointState.getId()).andReturn(IvrEndpointState.TALKING).anyTimes();
        request.addToLog("op. number (88024) ready to commutate");
        request.fireReadyToCommutateQueueEvent(isA(CallsCommutationManager.class));
        request.addToLog("abonent ready to commutate");
        expect(request.getConversation()).andReturn(abonentConversation);
        expect(manager.createBridge(abonentConversation, operatorConversation, null)).andReturn(bridge);
        bridge.addBridgeListener(checkBridgeListener());
        bridge.activateBridge();
        request.fireCommutatedEvent();
        expect(conversationResult.getCompletionCode()).andReturn(CompletionCode.COMPLETED_BY_OPPONENT);
        request.addToLog("conv. for op. number (88024) completed (COMPLETED_BY_OPPONENT)");
        request.fireDisconnectedQueueEvent();
        
        replay(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint, 
            endpointState, manager, bridge, conversationResult);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        bridgeManager.setManager(manager);
        new Thread(){
            @Override
            public void run() {
                operator.processRequest(queue, request);
            }
        }.start();
        Thread.sleep(1000);
        //check for busy
        assertFalse(operator.processRequest(queue, request));
        CallsCommutationManagerImpl commutationManager = operator.getCommutationManager();
        commutationManager.operatorReadyToCommutate(operatorConversation);
        Thread.sleep(100);
        commutationManager.abonentReadyToCommutate(abonentConversation);
        Thread.sleep(100);
        commutationManager.conversationCompleted(conversationResult);
        Thread.sleep(100);

        verify(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint,
            endpointState, manager, bridge, conversationResult);
    }

    public static IvrConversationsBridgeListener checkBridgeListener()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                IvrConversationsBridgeListener listener = (IvrConversationsBridgeListener) arg;
                listener.bridgeActivated(null);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static Map<String, Object> checkBindings(
            final CallsQueueOperator operator, final CallQueueRequestWrapper request)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Map<String, Object> bindings = (Map<String, Object>) argument;
                assertTrue(bindings.get(CallsCommutationManager.CALLS_COMMUTATION_MANAGER_BINDING)
                        instanceof CallsCommutationManager);
                assertSame(bindings.get(CallsCommutationManager.CALL_QUEUE_REQUEST_BINDING), request);
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