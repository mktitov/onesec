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

package org.onesec.raven.ivr.queue.actions;

import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import static org.onesec.raven.ivr.queue.actions.QueueCallAction.QUEUED_CALL_STATUS_BINDING;
import org.onesec.raven.ivr.queue.impl.CallQueueRequestImpl;
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.CompletedFuture;
import org.raven.ds.DataContext;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class QueueCallActionTest extends ActionTestCase {
    
    @Test public void createRequestTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallQueueRequestSender requestSender,
            @Mocked final DataContext context
    ) throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(QUEUED_CALL_STATUS_BINDING); result = null;
            bindings.get(BindingNames.DATA_CONTEXT_BINDING); result = null;
            requestSender.createDataContext(); result = context;
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new QueueCallAction(requestSender, true, false, 1, "queue_1", true, "1122"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
        
        new Verifications() {{
            CallQueueRequestImpl req1, req2;
            state.setBinding(QUEUED_CALL_STATUS_BINDING, req1 = withCapture(), BindingScope.POINT);
            requestSender.sendCallQueueRequest(req2 = withCapture(), context);            
            assertNotNull(req1);
            assertSame(req1, req2);
            assertEquals(1, req1.getPriority());
            assertEquals("queue_1", req1.getQueueId());
            assertTrue(req1.isContinueConversationOnReadyToCommutate());
            assertFalse(req1.isContinueConversationOnReject());
            assertTrue(req1.isQueueing());
            assertEquals("1122", req1.getOperatorPhoneNumbers());
        }};
    }
    
    @Test public void createRequestWithExternalDataContextTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallQueueRequestSender requestSender,
            @Mocked final DataContext context
    ) throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(QUEUED_CALL_STATUS_BINDING); result = null;
            bindings.get(BindingNames.DATA_CONTEXT_BINDING); result = context;
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new QueueCallAction(requestSender, true, true, 1, "queue_1", true, "1122"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
        
        new Verifications() {{
            CallQueueRequestImpl req1, req2;
            state.setBinding(QUEUED_CALL_STATUS_BINDING, req1 = withCapture(), BindingScope.POINT);
            requestSender.sendCallQueueRequest(req2 = withCapture(), context);            
            assertNotNull(req1);
            assertSame(req1, req2);
        }};
    }
    
    @Test public void commutationPhaseWithGreetingTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus request,
            @Mocked final AudioFile greeting,
            @Mocked final AudioStream audioStream
    ) throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(QUEUED_CALL_STATUS_BINDING); result = request;
            request.isReadyToCommutate(); result = true;
            request.getOperatorGreeting(); result = greeting;
            conv.getAudioStream(); result = audioStream;
            final List<CallQueueRequestListener> listeners = new LinkedList<>();
            audioStream.addSource((InputStreamSource) withNotNull()); result = new CompletedFuture(null, executor);
            request.addRequestListener(withCapture(listeners));
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
            request.replayToReadyToCommutate(); result = new Delegate() {
                void replayToReadyToCommutate() {
                    listeners.get(0).commutated();
                }
            };
            state.disableDtmfProcessing(); result = new Delegate() {
                void disableDtmfProcessing() {
                    listeners.get(0).disconnected();
                }
            };
            state.enableDtmfProcessing();
            state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new QueueCallAction(null, true, true, 1, "queue_1", true, "1122"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
    @Test public void commutationPhaseWithoutGreetingTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus request
    ) throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(QUEUED_CALL_STATUS_BINDING); result = request;
            request.isReadyToCommutate(); result = true;
            final List<CallQueueRequestListener> listeners = new LinkedList<>();
            request.addRequestListener(withCapture(listeners));
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
            request.replayToReadyToCommutate(); result = new Delegate() {
                void replayToReadyToCommutate() {
                    listeners.get(0).commutated();
                }
            };
            state.disableDtmfProcessing(); result = new Delegate() {
                void disableDtmfProcessing() {
                    listeners.get(0).disconnected();
                }
            };
            state.enableDtmfProcessing();
            state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new QueueCallAction(null, true, true, 1, "queue_1", false, "1122"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
//    @Test
//    public void createRequestTest() throws Exception
//    {
//        CallQueueRequestSender requestSender = createMock(CallQueueRequestSender.class);
//        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
//        DataContext context = createMock(DataContext.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Node owner = createMock(Node.class);
//        Bindings bindings = createMock(Bindings.class);
//
//        expect(conversation.getOwner()).andReturn(owner).anyTimes();
//        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(false).anyTimes();
//        expect(conversation.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings).times(2);
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(null);
//        expect(bindings.get(BindingNames.DATA_CONTEXT_BINDING)).andReturn(null);
//        state.setBinding(eq(QueueCallAction.QUEUED_CALL_STATUS_BINDING), checkCallQueueRequest()
//                , eq(BindingScope.POINT));
//        expect(requestSender.createDataContext()).andReturn(context);
//        requestSender.sendCallQueueRequest(isA(CallQueueRequest.class), isA(DataContext.class));
//
//        replay(requestSender, conversation, state, owner, bindings, context);
//
//        QueueCallAction action = new QueueCallAction(requestSender, true, false, 10, "test queue", false, "1001");
//        action.doExecute(conversation);
//
//        verify(requestSender, conversation, state, owner, bindings, context);
//    }
//
//    @Test
//    public void createRequestWithExternalDataContextTest() throws Exception
//    {
//        CallQueueRequestSender requestSender = createMock(CallQueueRequestSender.class);
//        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
//        DataContext context = createMock(DataContext.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Node owner = createMock(Node.class);
//        Bindings bindings = createMock(Bindings.class);
//
//        expect(conversation.getOwner()).andReturn(owner).anyTimes();
//        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(false).anyTimes();
//        expect(conversation.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings).times(2);
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(null);
//        expect(bindings.get(BindingNames.DATA_CONTEXT_BINDING)).andReturn(context);
//        state.setBinding(eq(QueueCallAction.QUEUED_CALL_STATUS_BINDING), checkCallQueueRequest()
//                , eq(BindingScope.POINT));
////        expect(requestSender.createDataContext()).andReturn(context);
//        requestSender.sendCallQueueRequest(isA(CallQueueRequest.class), isA(DataContext.class));
//
//        replay(requestSender, conversation, state, owner, bindings, context);
//
//        QueueCallAction action = new QueueCallAction(requestSender, true, false, 10, "test queue", false, "1001");
//        action.doExecute(conversation);
//
//        verify(requestSender, conversation, state, owner, bindings, context);
//    }
//
//    @Test
//    public void commutationPhaseTest() throws Exception
//    {
//        CallQueueRequestSender requestSender = createMock(CallQueueRequestSender.class);
//        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Node owner = createMock(Node.class);
//        Bindings bindings = createMock(Bindings.class);
//        QueuedCallStatus callStatus = createStrictMock(QueuedCallStatus.class);
//
//        expect(conversation.getOwner()).andReturn(owner).anyTimes();
//        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(false).anyTimes();
//        expect(conversation.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings);
//        state.disableDtmfProcessing();
//        state.enableDtmfProcessing();
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
//        expect(callStatus.isReadyToCommutate()).andReturn(true).anyTimes();
//        expect(callStatus.getOperatorGreeting()).andReturn(null);
//        callStatus.replayToReadyToCommutate();
//        expect(callStatus.isCommutated()).andReturn(Boolean.TRUE).anyTimes();
//        expect(callStatus.isDisconnected()).andReturn(Boolean.TRUE);
//
//        replay(requestSender, conversation, state, owner, bindings, callStatus);
//
//        QueueCallAction action = new QueueCallAction(requestSender, true, true, 10, "test queue", true, "1001");
//        action.doExecute(conversation);
//
//        verify(requestSender, conversation, state, owner, bindings, callStatus);
//    }
//
//    public static Object checkCallQueueRequest() 
//    {
//        reportMatcher(new IArgumentMatcher() {
//            public boolean matches(Object argument)
//            {
//                assertTrue(argument instanceof CallQueueRequestImpl);
//                CallQueueRequestImpl request = (CallQueueRequestImpl) argument;
//                assertEquals(10, request.getPriority());
//                assertEquals("test queue", request.getQueueId());
//                assertTrue(request.isContinueConversationOnReadyToCommutate());
//                assertFalse(request.isContinueConversationOnReject());
//                assertTrue(request.isQueueing());
//                assertEquals("1001", request.getOperatorPhoneNumbers());
//                return true;
//            }
//            public void appendTo(StringBuffer buffer) {
//            }
//        });
//        return null;
//    }
}