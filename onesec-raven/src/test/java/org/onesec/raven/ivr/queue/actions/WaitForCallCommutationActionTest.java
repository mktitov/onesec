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

import java.util.concurrent.atomic.AtomicReference;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.CompletedFuture;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class WaitForCallCommutationActionTest extends ActionTestCase
{
//    private static String executedKey = "executed_org.onesec.raven.ivr.queue.actions.WaitForCallCommutationAction_1";
    private WaitForCallCommutationActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new WaitForCallCommutationActionNode();
        actionNode.setName("action node");
        testsNode.addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void withoutCommutationManagerTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
    ) throws Exception {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }

    @Test
    public void normalTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CommutationManagerCall commutationManager
    ) throws Exception {
        final TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        final DataProcessorFacade action = createAction(actionExecutor, actionNode);
        final AtomicReference<CallsCommutationManagerListener> commutationListener = new AtomicReference<>();
        new Expectations() {{
            bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING); result = commutationManager;
            commutationManager.addListener((CallsCommutationManagerListener)withNotNull()); result = new Delegate() {
                public void addListener(final CallsCommutationManagerListener listener) {
                    commutationListener.set(listener);
                    new Thread() {
                        @Override public void run() {
                            listener.stateChanged(CommutationManagerCall.State.INVITING);
                        }
                    }.start();
                }
            };
            state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);
            state.disableDtmfProcessing(); times = 2;
            commutationManager.operatorReadyToCommutate(conv); result = new Delegate() {
                public void operatorReadyToCommutate(IvrEndpointConversation conv) {
                    new Thread() {
                        @Override public void run() {
                            try {
                                //ждем 2 бипа и только потом шлем ABONENT_READY
                                Thread.sleep(WaitForCallCommutationAction.PAUSE_BETWEEN_BEEPS+100);
                                commutationListener.get().stateChanged(CommutationManagerCall.State.ABONENT_READY);
                                //∆дем немного и шлем COMMUTATION_INVALIDATED
                                Thread.sleep(100);
                                commutationListener.get().stateChanged(CommutationManagerCall.State.INVALID);
                            } catch (InterruptedException ex) { }
                        }
                    }.start();
                }
            };
            //beeping phase
            Cacheable beepCacheInfo = WaitForCallCommutationAction.BEEP_CACHE_INFO;
            audioStream.addSource(beepCacheInfo.getCacheKey(), beepCacheInfo.getCacheChecksum(), (InputStreamSource)withNotNull()); times = 2;
                result = new CompletedFuture(null, executor);
            //abonent ready phase
            state.enableDtmfProcessing();
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            commutationManager.removeListener((CallsCommutationManagerListener)withNotNull());
        }};
        
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(WaitForCallCommutationAction.PAUSE_BETWEEN_BEEPS+300));
        action.stop();
        Thread.sleep(100);
    }
    
    @Test
    public void executeOnAbonentReadyPhase(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CommutationManagerCall commutationManager
    ) throws Exception {
        final TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        final DataProcessorFacade action = createAction(actionExecutor, actionNode);
        final AtomicReference<CallsCommutationManagerListener> commutationListener = new AtomicReference<>();
        new Expectations() {{
            bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING); result = commutationManager;
            commutationManager.addListener((CallsCommutationManagerListener)withNotNull()); result = new Delegate() {
                public void addListener(final CallsCommutationManagerListener listener) {
                    commutationListener.set(listener);
                    new Thread() {
                        @Override public void run() {
                            try {
                                listener.stateChanged(CommutationManagerCall.State.ABONENT_READY);
                                Thread.sleep(100);
                                listener.stateChanged(CommutationManagerCall.State.INVALID);
                            } catch (InterruptedException ex) { }
                        }
                    }.start();
                }
            };
            state.enableDtmfProcessing();
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            commutationManager.removeListener((CallsCommutationManagerListener)withNotNull());
        }};
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(200));
        action.stop();
        Thread.sleep(100);
    }

    @Test
    public void executeOnCommutatedPhase(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CommutationManagerCall commutationManager
    ) throws Exception {
        final TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        final DataProcessorFacade action = createAction(actionExecutor, actionNode);
        final AtomicReference<CallsCommutationManagerListener> commutationListener = new AtomicReference<>();
        new Expectations() {{
            bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING); result = commutationManager;
            commutationManager.addListener((CallsCommutationManagerListener)withNotNull()); result = new Delegate() {
                public void addListener(final CallsCommutationManagerListener listener) {
                    commutationListener.set(listener);
                    new Thread() {
                        @Override public void run() {
                            try {
//                                listener.stateChanged(CommutationManagerCall.State.ABONENT_READY);
                                Thread.sleep(100);
                                listener.stateChanged(CommutationManagerCall.State.INVALID);
                            } catch (InterruptedException ex) { }
                        }
                    }.start();
                }
            };
//            state.enableDtmfProcessing();
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);
            commutationManager.removeListener((CallsCommutationManagerListener)withNotNull());
        }};
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(200));
        action.stop();
        Thread.sleep(100);
    }

////    @Test
//    public void errorTest()
//    {
//        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        Node owner = createMock(Node.class);
//        
////        expect(owner.getId()).andReturn(1).anyTimes();
//        expect(conv.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(null);
////        expect(bindings.containsKey(executedKey)).andReturn(false);
////        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);
//
//        replay(conv, state, bindings, owner);
//
//        WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
//        try {
//            action.doExecute(conv);
//            fail();
//        } catch (Exception ex) {
//        }
//
//        verify(conv, state, bindings, owner);
//    }
//
//    @Test
//    public void normalTest() throws Exception {
//        final IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        CommutationManagerCall manager = createMock(CommutationManagerCall.class);
//        Node owner = createMock(Node.class);
//        final WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
//        
//        expect(owner.getId()).andReturn(1).anyTimes();
//        expect(conv.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(manager);
//        manager.operatorReadyToCommutate(conv);
//        manager.addListener(action);
//        expect(manager.isCommutationValid()).andReturn(Boolean.TRUE).anyTimes();
//        expect(bindings.size()).andReturn(1).anyTimes();
//        expect(bindings.containsKey(executedKey)).andReturn(false);
//        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);
//        
//        replay(conv, state, bindings, manager, owner);
//
//
//        Thread actionThread = new Thread(){
//            @Override public void run() {
//                try {
//                    action.doExecute(conv);
//                } catch (Exception ex) {
//                    fail();
//                }
//            }
//        };
//
//        actionThread.start();
////        TimeUnit.MILLISECONDS.sleep(500);
//        TimeUnit.MILLISECONDS.sleep(150000);
//        assertTrue(actionThread.isAlive());
//        action.cancel();
//        TimeUnit.MILLISECONDS.sleep(150);
//        assertFalse(actionThread.isAlive());
//
//        verify(conv, state, bindings, manager, owner);
//    }
//
////    @Test
//    public void normalTest2() throws Exception {
//        final IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        CommutationManagerCall manager = createMock(CommutationManagerCall.class);
//        Node owner = createMock(Node.class);
//        final WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
//        
//        expect(owner.getId()).andReturn(1).anyTimes();
//        expect(conv.getConversationScenarioState()).andReturn(state);
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(manager);
//        manager.addListener(action);
//        manager.operatorReadyToCommutate(conv);
//        expect(manager.isCommutationValid()).andReturn(Boolean.TRUE).times(5).andReturn(Boolean.FALSE);
//        expect(bindings.containsKey(executedKey)).andReturn(false);
//        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);
//
//        replay(conv, state, bindings, manager, owner);
//
//
//        Thread actionThread = new Thread(){
//            @Override
//            public void run() {
//                try {
//                    action.doExecute(conv);
//                } catch (Exception ex) {
//                    fail();
//                }
//            }
//        };
//
//        actionThread.start();
//        TimeUnit.MILLISECONDS.sleep(400);
//        assertTrue(actionThread.isAlive());
//        TimeUnit.MILLISECONDS.sleep(200);
//        assertFalse(actionThread.isAlive());
//
//        verify(conv, state, bindings, manager, owner);
//    }
}