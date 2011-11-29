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

import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.AudioFile;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.onesec.raven.ivr.EndpointRequest;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationCdr;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrEndpointConversationStoppedEventImpl;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.RequestWrapperListener;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.ExecutorServiceNode;
import static org.easymock.EasyMock.*;
import org.onesec.raven.ivr.queue.*;

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
    private ExecutorServiceNode executor;
    private static final AtomicBoolean flag = new AtomicBoolean();

    @Before
    public void prepare() {
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

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(10);
        executor.setMaximumPoolSize(10);
        assertTrue(executor.start());

        operator = new CallsQueueOperatorNode();
        operator.setName("operator");
        tree.getRootNode().addAndSaveChildren(operator);
        operator.setConversationsBridgeManager(bridgeManager);
        operator.setEndpointPool(endpointPool);
        operator.setPhoneNumbers("88024");
        operator.setExecutor(executor);
    }

//    @Test
    public void requestOnStoppedNode()
    {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        replay(request, queue);

        assertFalse(operator.processRequest(queue, request, scenario, null));

        verify(request, queue);
    }
    
//    @Test
    public void requestOnNotActiveNode()
    {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        replay(request, queue);

        operator.setActive(Boolean.FALSE);
        assertTrue(operator.start());
        assertFalse(operator.processRequest(queue, request, scenario, null));

        verify(request, queue);
    }

//    @Test
    public void noFreeEndpointInThePoolTest() throws ExecutorServiceException, InterruptedException {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        CallsQueue queue = createMock(CallsQueue.class);
        AudioFile audioFile = createMock(AudioFile.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        
        request.addToLog("handling by operator (operator)");
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.fireOperatorQueueEvent(operator.getName());
        request.addRequestWrapperListener(isA(RequestWrapperListener.class));
        expect(request.isValid()).andReturn(Boolean.TRUE);
        expect(request.isHandlingByOperator()).andReturn(false);
        expect(request.logMess(isA(String.class), isA(String.class))).andReturn("log mess").anyTimes();
        pool.requestEndpoint(sendNullEndpoint());
        request.addToLog("no free endpoints in the pool");
        request.addToLog("operator (operator) didn't handle a call");
        queue.queueCall(request);

        replay(request, queue, pool, audioFile);

        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        operator.processRequest(queue, request, scenario, audioFile);
        Thread.sleep(200);
        verify(request, queue, pool, audioFile);
    }

//    @Test(timeout=10000)
    public void inviteTimeoutTest() throws Exception {
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        AudioFile audioFile = createMock(AudioFile.class);
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint endpoint = createMock(IvrEndpoint.class);
        IvrEndpointState endpointState = createMock(IvrEndpointState.class);
        StateWaitResult stateWaitResult = createMock(StateWaitResult.class);

        request.fireOperatorQueueEvent(operator.getName());
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.fireOperatorNumberQueueEvent("88024");
        request.addToLog("handling by operator (operator)");
        pool.requestEndpoint(sendEndpoint(endpoint));
        pool.releaseEndpoint(endpoint);
        request.addRequestWrapperListener(isA(RequestWrapperListener.class));
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(request.isHandlingByOperator()).andReturn(false).anyTimes();
        endpoint.invite(eq("88024"), eq(4), eq(0), checkConvListener(), same(scenario)
                , checkBindings(operator, request));
        request.addToLog("number (88024) not answer");
        request.addToLog("operator (operator) didn't handle a call");
        queue.queueCall(checkRequest(request));

        replay(request, queue, pool, endpoint, endpointState, stateWaitResult, audioFile);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        long startTime = System.currentTimeMillis();
        flag.set(false);
        operator.processRequest(queue, request, scenario, audioFile);
        while (!flag.get())
            Thread.sleep(100);
        long diff = System.currentTimeMillis() - startTime;
        System.out.println("diff:"+diff);

        verify(request, queue, pool, endpoint, endpointState, stateWaitResult, audioFile);
    }

    @Test(timeout=10000)
//    @Test()
    public void commutateTest() throws Exception {
        final CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        final AudioFile audioFile = createMock(AudioFile.class);
        IvrEndpointConversation abonentConversation = createMock(
                "abonentConversation", IvrEndpointConversation.class);
        IvrEndpointConversation operatorConversation = createMock(
                "operatorConversation", IvrEndpointConversation.class);
        final CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint operatorEndpoint = createMock(IvrEndpoint.class);
        IvrEndpointState endpointState = createMock(IvrEndpointState.class);
        IvrConversationsBridgeManager bManager = createMock(IvrConversationsBridgeManager.class);
        IvrConversationsBridge bridge = createMock(IvrConversationsBridge.class);
        CallsCommutationManagerListener commListener = createMock(CallsCommutationManagerListener.class);
        AtomicReference<IvrEndpointConversationListener> convListener = 
                new AtomicReference<IvrEndpointConversationListener>();
//        ConversationCdr conversationResult = createMock(ConversationCdr.class);

        //INIT STEP
        request.fireOperatorQueueEvent(operator.getName());
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.addToLog("handling by operator (operator)");
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(request.isHandlingByOperator()).andReturn(false).anyTimes();
        request.addRequestWrapperListener(isA(RequestWrapperListener.class));
        pool.requestEndpoint(sendEndpoint(operatorEndpoint));
        
        //INVITING STEP
        request.fireOperatorNumberQueueEvent("88024");
        operatorEndpoint.invite(eq("88024"), eq(4), eq(0), handleConversationListener(convListener)
                , same(scenario), checkBindings(operator, request, operatorConversation, commListener));
        
        //OPERATOR READY TO COMMUTATE
        expect(request.logMess("Operator (operator). Number (88024) ready to commutate")).andReturn("prefix");
        request.addToLog("op. number (88024) ready to commutate");
        expect(request.fireReadyToCommutateQueueEvent(
                processReadyToCommutate(operatorConversation, abonentConversation, conversationResult))
        ).andReturn(Boolean.TRUE);
        
        //ABONENT READY TO COMMUTATE
        expect(request.logMess("Operator (operator). Abonent ready to commutate")).andReturn("prefix");
        request.addToLog("abonent ready to commutate");
        commListener.abonentReady();
        
        //COMMUTATED
        expect(bManager.createBridge(abonentConversation, operatorConversation, "prefix")).andReturn(bridge);
        bridge.addBridgeListener(checkBridgeListener());
        bridge.activateBridge();
        request.fireCommutatedEvent();
        
        //HANDLED
        expect(request.logMess("Operator (operator). Operator's conversation completed")).andReturn("prefix");
        request.addToLog("conv. for op. number (88024) completed (COMPLETED_BY_OPPONENT)");
        request.fireDisconnectedQueueEvent();
        
        //INVALID
        pool.releaseEndpoint(operatorEndpoint);
        
        
        request.fireOperatorQueueEvent(operator.getName());
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.fireOperatorNumberQueueEvent("88024");
        request.addToLog("handling by operator (operator)");
        expect(operatorEndpoint.getEndpointState()).andReturn(endpointState).anyTimes();
//        expect(endpointState.getId()).andReturn(IvrEndpointState.TALKING).anyTimes();
        request.addToLog("abonent ready to commutate");
        expect(request.getConversation()).andReturn(abonentConversation);
        request.fireCommutatedEvent();
//        expect(conversationResult.getCompletionCode()).andReturn(CompletionCode.COMPLETED_BY_OPPONENT);

        replay(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint,
            endpointState, bManager, bridge, audioFile);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        bManager.setManager(manager);
        operator.processRequest(queue, request, scenario, audioFile);
        Thread.sleep(1000);
        //check for busy
        assertFalse(operator.processRequest(queue, request, scenario, audioFile));
//        flag.set(false);
//        while (!flag.get())
//            Thread.sleep(100);
        Thread.sleep(1000);
//        CallsCommutationManager commutationManager = operator.getCommutationManager();
//        commutationManager.operatorReadyToCommutate(operatorConversation);
//        Thread.sleep(100);
//        commutationManager.abonentReadyToCommutate(abonentConversation);
//        Thread.sleep(100);
//        commutationManager.conversationCompleted(conversationResult);
//        Thread.sleep(100);

        verify(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint,
            endpointState, bManager, bridge);
    }

    public static IvrConversationsBridgeListener checkBridgeListener() {
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
            final CallsQueueOperator operator, final CallQueueRequestWrapper request
            , final IvrEndpointConversation operatorConversation
            , final CallsCommutationManagerListener commListener)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Map<String, Object> bindings = (Map<String, Object>) argument;
                Object obj = bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);
                assertTrue(obj instanceof CommutationManagerCall);
                assertSame(bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING), request);
                final CommutationManagerCall call = (CommutationManagerCall)obj;
                new Thread(){
                    @Override public void run() {
                        call.addListener(commListener);
                        call.operatorReadyToCommutate(operatorConversation);
                    }
                }.start();
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
                Object obj = bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);
                assertTrue(obj instanceof CommutationManagerCall);
//                CommutationManagerCall call = (CommutationManagerCall)obj;
//                call.operatorReadyToCommutate(operatorConversation);
                assertSame(bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING), request);
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

    public static EndpointRequest sendEndpoint(final IvrEndpoint endpoint) {
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

    public static CallQueueRequestWrapper checkRequest(final CallQueueRequestWrapper request) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                flag.set(true);
                return request==argument;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static CommutationManagerCall processReadyToCommutate(
            final IvrEndpointConversation operatorConversation
            , final IvrEndpointConversation abonentConversation
            , final ConversationCdr conversationResult)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                final CommutationManagerCallImpl commutationManager = (CommutationManagerCallImpl) arg;
                new Thread(){
                    @Override public void run() {
                        try {
                            commutationManager.abonentReadyToCommutate(abonentConversation);
                            Thread.sleep(100);
//                            commutationManager.conversationCompleted(conversationResult);
                            Thread.sleep(100);
                            flag.set(true);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                return arg instanceof CommutationManagerCall;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static IvrEndpointConversationListener checkConvListener() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(final Object arg) {
                new Thread(new Runnable() {
                    public void run() {
                        ((IvrEndpointConversationListener)arg).conversationStopped(
                                new IvrEndpointConversationStoppedEventImpl(
                                        null, CompletionCode.OPPONENT_NOT_ANSWERED));
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }

    private IvrEndpointConversationListener handleConversationListener(
            final AtomicReference<IvrEndpointConversationListener> convListener) 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                convListener.set((IvrEndpointConversationListener)arg);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}