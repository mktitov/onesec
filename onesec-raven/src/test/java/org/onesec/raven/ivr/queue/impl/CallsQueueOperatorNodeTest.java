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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.easymock.EasyMock.*;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrEndpointConversationStoppedEventImpl;
import org.onesec.raven.ivr.queue.*;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueOperatorNodeTest extends OnesecRavenTestCase {
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
        
        CallsQueuesNode callsQueues = new CallsQueuesNode();
        callsQueues.setName("calls queues");
        tree.getRootNode().addAndSaveChildren(callsQueues);
        assertTrue(callsQueues.start());

        operator = new CallsQueueOperatorNode();
        operator.setName("operator");
        callsQueues.getOperatorsNode().addAndSaveChildren(operator);
        operator.setConversationsBridgeManager(bridgeManager);
        operator.setEndpointPool(endpointPool);
        operator.setPhoneNumbers("88024");
        operator.setExecutor(executor);
//        operator.setLogLevel(LogLevel.DEBUG);
    }

    @Test
    public void requestOnStoppedNode()
    {
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        CallsQueue queue = createMock(CallsQueue.class);
        replay(request, queue);

        assertFalse(operator.processRequest(queue, request, scenario, null, null, null));

        verify(request, queue);
    }
    
    @Test
    public void requestOnNotActiveNode()
    {
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        CallsQueue queue = createMock(CallsQueue.class);
        replay(request, queue);

        operator.setActive(Boolean.FALSE);
        assertTrue(operator.start());
        assertFalse(operator.processRequest(queue, request, scenario, null, null, null));

        verify(request, queue);
    }
    
    @Test
    public void busyTimerTest() throws Exception {
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        CallsQueue queue = createMock(CallsQueue.class);
        AudioFile audioFile = createMock(AudioFile.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        
        request.addToLog("handling by operator (operator)");
        expectLastCall().times(2);
        request.fireOperatorGreetingQueueEvent(audioFile);
        expectLastCall().times(2);
        request.fireOperatorQueueEvent(operator.getName(), null, null);
        expectLastCall().times(2);
//        request.addRequestWrapperListener(isA(RequestControllerListener.class));
//        request.removeRequestWrapperListener(isA(RequestControllerListener.class));
//        expectLastCall().times(2);
        expect(request.isValid()).andReturn(true).times(2);
        expect(request.isHandlingByOperator()).andReturn(false).times(2);
        expect(request.logMess(isA(String.class), isA(String.class))).andReturn("log mess").anyTimes();
//        request.addToLog("NOT handled by op.(operator)");
//        expectLastCall().times(2);
        pool.requestEndpoint(isA(EndpointRequest.class));
        expectLastCall().times(2);
//        request.addToLog("no free endpoints in the pool");
//        queue.queueCall(request);

        replay(request, queue, pool, audioFile);

        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        operator.setBusyTimer(1);
        operator.processRequest(queue, request, scenario, audioFile, null, null);
        operator.doRequestProcessed(operator.getCommutationManager(), true);
        assertFalse(operator.getBusy());
        assertFalse(operator.processRequest(queue, request, scenario, audioFile, null, null));
        Thread.sleep(1100);
        assertTrue(operator.processRequest(queue, request, scenario, audioFile, null, null));
        Thread.sleep(200);
        
        verify(request, queue, pool, audioFile);
        
    }

    @Test
    public void noFreeEndpointInThePoolTest() throws ExecutorServiceException, InterruptedException {
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        CallsQueue queue = createMock(CallsQueue.class);
        AudioFile audioFile = createMock(AudioFile.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        
        request.addToLog("handling by operator (operator)");
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.fireOperatorQueueEvent(operator.getName(), null, null);
//        request.addRequestWrapperListener(isA(RequestControllerListener.class));
        request.removeRequestWrapperListener(isA(RequestControllerListener.class));
        expect(request.isValid()).andReturn(Boolean.TRUE);
        expect(request.isHandlingByOperator()).andReturn(false);
        expect(request.logMess(isA(String.class), isA(String.class))).andReturn("log mess").anyTimes();
        request.addToLog("NOT handled by op.(operator)");
        pool.requestEndpoint(sendNullEndpoint());
        request.addToLog("no free endpoints in the pool");
        queue.queueCall(request);

        replay(request, queue, pool, audioFile);

        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        operator.processRequest(queue, request, scenario, audioFile, null, null);
        Thread.sleep(200);
        verify(request, queue, pool, audioFile);
    }

    @Test(timeout=10000)
    public void inviteTimeoutTest() throws Exception {
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        AudioFile audioFile = createMock(AudioFile.class);
        CallsQueue queue = createMock(CallsQueue.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint endpoint = createMock(IvrEndpoint.class);
        IvrEndpointState endpointState = createMock(IvrEndpointState.class);
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        StateWaitResult stateWaitResult = createMock(StateWaitResult.class);

        request.fireOperatorQueueEvent(operator.getName(), null, null);
        request.fireOperatorGreetingQueueEvent(audioFile);
//        request.fireOperatorNumberQueueEvent("88024");
        request.addToLog("handling by operator (operator)");
        pool.requestEndpoint(sendEndpoint(endpoint));
        pool.releaseEndpoint(endpoint);
        request.addRequestWrapperListener(isA(RequestControllerListener.class));
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(request.isHandlingByOperator()).andReturn(false).anyTimes();
        expect(request.getConversation()).andReturn(conversation);
        expect(conversation.getCallingNumber()).andReturn("12345");
        endpoint.invite(eq("88024"), geq(4), eq(0), checkConvListener(), same(scenario)
                , checkBindings(operator, request), eq("12345"));
        request.removeRequestWrapperListener(isA(RequestControllerListener.class));
        request.addToLog("no answer from (88024)");
        request.addToLog("NOT handled by op.(operator)");
        queue.queueCall(checkRequest(request));

        replay(request, queue, pool, endpoint, endpointState, stateWaitResult, audioFile, conversation);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        long startTime = System.currentTimeMillis();
        flag.set(false);
        operator.processRequest(queue, request, scenario, audioFile, null, null);
        while (!flag.get())
            Thread.sleep(100);
        long diff = System.currentTimeMillis() - startTime;
        System.out.println("diff:"+diff);

        verify(request, queue, pool, endpoint, endpointState, stateWaitResult, audioFile, conversation);
    }

    @Test(timeout=100000)
    public void commutateTest() throws Exception {
        final CallQueueRequestController request = createMock(CallQueueRequestController.class);
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

        //INIT STEP
        request.fireOperatorQueueEvent(operator.getName(), null, null);
        request.fireOperatorGreetingQueueEvent(audioFile);
        request.addToLog("handling by operator (operator)");
        expect(request.isValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(request.isHandlingByOperator()).andReturn(false).anyTimes();
        pool.requestEndpoint(sendEndpoint(operatorEndpoint));
        
        //INVITING STEP
//        request.fireOperatorNumberQueueEvent("88024");
        expect(request.getConversation()).andReturn(abonentConversation);
        expect(abonentConversation.getCallingNumber()).andReturn("12345");
        operatorEndpoint.invite(eq("88024"), geq(4), eq(0), handleConversationListener(convListener)
                , same(scenario), checkBindings(operator, request, operatorConversation, commListener), 
                eq("12345"));
        
        //OPERATOR READY TO COMMUTATE
        request.addRequestWrapperListener(isA(RequestControllerListener.class));
        request.addToLog("op. number (88024) ready to commutate");
        expect(request.fireReadyToCommutateQueueEvent(
                processReadyToCommutate(operatorConversation, abonentConversation))
        ).andReturn(Boolean.TRUE);
        
        //ABONENT READY TO COMMUTATE
        request.addToLog("abonent ready to commutate");
        commListener.stateChanged(CommutationManagerCall.State.ABONENT_READY);
        
        //COMMUTATED
        expect(request.getConversation()).andReturn(abonentConversation);
        expect(request.logMess("Operator (operator). ")).andReturn("prefix");
        expect(bManager.createBridge(abonentConversation, operatorConversation, "prefix")).andReturn(bridge);
        bridge.addBridgeListener(checkBridgeListener(convListener));
        bridge.activateBridge();
        request.fireCommutatedEvent();

        //HANDLED
        request.addToLog("conv. for op.num. (88024) completed (COMPLETED_BY_OPPONENT)");
        request.fireDisconnectedQueueEvent("");
        
        //INVALID
        request.removeRequestWrapperListener(isA(RequestControllerListener.class));
        pool.releaseEndpoint(operatorEndpoint);
        final AtomicBoolean stopFlag = new AtomicBoolean(false);
        expectLastCall().andAnswer(new IAnswer() {
            public Object answer() throws Throwable {
                stopFlag.set(true);
                return null;
            }
        });
        
        replay(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint,
            endpointState, bManager, bridge, audioFile);

        operator.setInviteTimeout(5);
        assertTrue(operator.start());
        endpointPool.setEndpointPool(pool);
        bridgeManager.setManager(bManager);
        operator.processRequest(queue, request, scenario, audioFile, null, null);
        Thread.sleep(10);
        //check for busy
        assertFalse(operator.processRequest(queue, request, scenario, audioFile, null, null));
        while(!stopFlag.get()) 
            TimeUnit.MILLISECONDS.sleep(100);
        TimeUnit.MILLISECONDS.sleep(1000);

        verify(request, abonentConversation, operatorConversation, queue, pool, operatorEndpoint,
            endpointState, bManager, bridge);
    }

    public static IvrConversationsBridgeListener checkBridgeListener(
            final AtomicReference<IvrEndpointConversationListener> conv) 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                final IvrConversationsBridgeListener listener = (IvrConversationsBridgeListener) arg;
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(100);
                            listener.bridgeActivated(null);
                            Thread.sleep(100);
                            conv.get().conversationStopped(new IvrEndpointConversationStoppedEventImpl(
                                    null, CompletionCode.COMPLETED_BY_OPPONENT));
                        } catch (InterruptedException ex) { }
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static Map<String, Object> checkBindings(
            final CallsQueueOperator operator, final CallQueueRequestController request
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
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                        }
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
            final CallsQueueOperator operator, final CallQueueRequestController request)
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

    public static CallQueueRequestController checkRequest(final CallQueueRequestController request) {
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
            , final IvrEndpointConversation abonentConversation)
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
