/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
import org.onesec.raven.ivr.actions.PauseAction;
import org.raven.conv.ConversationScenarioState;
import static org.raven.dp.DataProcessor.VOID;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class ActionExecutorFacadeTest extends OnesecRavenTestCase {
    private ExecutorServiceNode executor;
    private LoggerHelper logger;
    
    @Mocked private IvrEndpointConversation conversation;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        super.configureRegistry(builder); 
    }

    @Before
    public void prepare() {
        testsNode.setLogLevel(LogLevel.TRACE);
        logger = new LoggerHelper(testsNode, null);
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
//        executor.setMaximumPoolSize(1);
        executor.setCorePoolSize(4);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        new Expectations(){{
            conversation.getExecutorService(); result = executor;
            executor.getPath(); result= "/actions executor/";
        }};
    }
    
    @Test
    public void executeTest(
            @Mocked final Handler handler1,
            @Mocked final Handler handler2,
            @Mocked final Handler handler3
    ) throws Exception 
    {
        new Expectations() {{
            handler1.processData(withInstanceOf(Action.Execute.class)); result = AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT;
            handler2.processData(withInstanceOf(Action.Execute.class)); result = AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT;
            handler3.processData(withInstanceOf(Action.Execute.class)); result = AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT;
        }};
        
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(
                (Action)new SimpleAction(handler1, "action1"), 
                new SimpleAction(handler2, "action2")));
        Thread.sleep(1000);
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler3, "action3")));
        Thread.sleep(500);
    }
    
//    @Test
//    public void executeWithErrorTest(
//            @Mocked final IvrAction action1,
//            @Mocked final IvrAction action2
//    ) throws Exception 
//    {
//        new Expectations() {{
//            action1.getName(); result = "action1";
//            action1.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
//            result = new Exception("Test action exceptions");
//        }};
//        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
//        actionsExecutor.executeActions(Arrays.asList(action1, action2));
//        Thread.sleep(500);
//    }
    
    
    
    @Test(timeout = ActionExecutorFacade.CANCEL_TIMEOUT*2)
    public void cancelTimeoutTest(@Mocked final Handler handler) 
            throws Exception 
    {
        new StrictExpectations() {{
            handler.processData(withInstanceOf(Action.Execute.class)); result = VOID;
            handler.processData(Action.CANCEL); result = VOID;
        }};
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler, "action1")));
        Thread.sleep(100); //дождемся что бы действие запустилось
        final long ts = System.currentTimeMillis();
        actionsExecutor.cancelActionsExecution();
        assertTrue(System.currentTimeMillis()-ts>=ActionExecutorFacade.CANCEL_TIMEOUT);
    }
    
    @Test(timeout = ActionExecutorFacade.CANCEL_TIMEOUT*2)
    public void normalCancelTest(@Mocked final Handler handler) 
            throws Exception 
    {
        new StrictExpectations() {{
            handler.processData(withInstanceOf(Action.Execute.class)); result = VOID;
            handler.processData(Action.CANCEL); result = AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT;
        }};
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler, "action1")));
        Thread.sleep(100); //дождемся что бы действие запустилось
        final long ts = System.currentTimeMillis();
        actionsExecutor.cancelActionsExecution();
        assertTrue(System.currentTimeMillis()-ts<100);
    }
    
    @Test
    public void dtmfProcessPointTest(
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
    ) throws Exception {
        final List<Character> collectedDtmfs = Arrays.asList('2', '1');
        new Expectations() {{
            conversation.continueConversation('-');
            conversation.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.put(IvrEndpointConversation.DTMFS_BINDING, collectedDtmfs); result = null;
//            bindings.get(IvrEndpointConversation.DTMFS_BINDING); result = collectedDtmfs;
        }};
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        List<Action> actions = new ArrayList<>();
        actions.add(new PauseAction(1000, TimeUnit.MILLISECONDS));
        actions.add(new DtmfProcessPointAction("12"));
        actions.add(new PauseAction(1000, TimeUnit.MILLISECONDS));
        actionsExecutor.executeActions(actions);
        assertTrue(actionsExecutor.hasDtmfProcessPoint('2'));
        assertTrue(actionsExecutor.hasDtmfProcessPoint('1'));
        assertFalse(actionsExecutor.hasDtmfProcessPoint('-'));
        Thread.sleep(1100);
        assertFalse(actionsExecutor.hasDtmfProcessPoint('1'));        
    }
    
    @Test
    public void flowControlTest(
            @Mocked final Handler handler1,
            @Mocked final Handler handler2,
            @Mocked final Handler handler3
    ) throws Exception 
    {
        new Expectations() {{
            handler1.processData(withInstanceOf(Action.Execute.class)); result = AbstractAction.ACTION_EXECUTED_then_STOP;
            //
            //handler2 not used
            //
            handler3.processData(withInstanceOf(Action.Execute.class)); result = AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT;
        }};
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(
                (Action)new SimpleAction(handler1, "action 1"),
                (Action)new SimpleAction(handler2, "action 2")
        ));
        Thread.sleep(100);
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler3, "action3")));
        Thread.sleep(100);
        new Verifications(){{
            handler2.processData(any); times=0;
        }};
    }
    
    @Test
    public void throttleExecutionTest(
            @Mocked final Handler handler1
    ) throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final AtomicReference<String> lastName = new AtomicReference<>();
        new Expectations() {{
            handler1.processData(withInstanceOf(Action.Execute.class)); result = new Delegate() {
                public Object processData(Action.Execute action) {
                    counter.incrementAndGet();
                    return AbstractAction.ACTION_EXECUTED_then_STOP;
                }                
            };
            handler1.setActionName(anyString); result = new Delegate() {
                public void actionName(String name) {
                    lastName.set(name);
                }
            };
        }};
        ActionExecutorFacade actionsExecutor = new ActionExecutorFacade(conversation, logger);
        int i;
        for (i=0; i<ActionExecutorDP.CHECK_SPEED_AT_CYCLE+1; i++) {
            actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler1, "action"+(i+1))));
            Thread.sleep(5);
//            actionsExecutor.cancelActionsExecution();            
        }        
        Thread.sleep(20);
        assertEquals(ActionExecutorDP.CHECK_SPEED_AT_CYCLE, counter.get());
        Thread.sleep(200l);
        assertEquals(ActionExecutorDP.CHECK_SPEED_AT_CYCLE+1, counter.get());
        
        //every next execution will be delayed on 200ms
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler1, "action"+(i++))));
        Thread.sleep(20);
        assertEquals(ActionExecutorDP.CHECK_SPEED_AT_CYCLE+1, counter.get());
        Thread.sleep(200l);
        assertEquals(ActionExecutorDP.CHECK_SPEED_AT_CYCLE+2, counter.get());
        
        //checking that delayed actions will be ignored if actionsExecutor cancels action executing
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler1, "action"+(i++))));
        Thread.sleep(5);
        actionsExecutor.executeActions(Arrays.asList((Action)new SimpleAction(handler1, "last action")));
        Thread.sleep(210);
        assertEquals(ActionExecutorDP.CHECK_SPEED_AT_CYCLE+3, counter.get());
        assertEquals("last action", lastName.get());
    }
    
    private interface Handler {
        public Object processData(Object message);
        public void setActionName(String name);
    }    
    
    private class SimpleAction extends AbstractAction {
        private final Handler handler;

        public SimpleAction(Handler handler, String name) {
            super(name);
            this.handler = handler;
        }

        public SimpleAction(String name) {
            super(name);
            handler = null;
        }

        @Override
        public Object processData(Object dataPackage) throws Exception {
            Object result = null;
            if (handler!=null) {
                result = handler.processData(dataPackage);
                handler.setActionName(getName());
            }
            if (result instanceof Action.ActionExecuted)
                getFacade().sendTo(getContext().getParent(), result);            
            return VOID;
        }        

        @Override
        protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void processCancelMessage() throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
