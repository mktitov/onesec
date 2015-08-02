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
import java.util.concurrent.atomic.AtomicReference;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.ActionStopListener;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
import org.onesec.raven.ivr.actions.PauseAction;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class IvrActionExecutorFacadeTest extends OnesecRavenTestCase {
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
        tree.getRootNode().addAndSaveChildren(executor);
//        executor.setMaximumPoolSize(1);
        executor.setCorePoolSize(4);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        new Expectations(){{
            conversation.getExecutorService(); result = executor;
        }};
    }
    
    @Test
    public void executeTest(
            @Mocked final IvrAction action1,
            @Mocked final IvrAction action2,
            @Mocked final IvrAction action3
    ) throws Exception 
    {
        new Expectations() {{
            action1.getName(); result = "action1";
            action1.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = createExecuteDelegate(action1);
            action2.getName(); result = "action2";
            action2.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = createExecuteDelegate(action2);
            action3.getName(); result = "action3";
            action3.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = createExecuteDelegate(action3);
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(action1, action2));
        Thread.sleep(1000);
        actionsExecutor.executeActions(Arrays.asList(action3));
        Thread.sleep(500);
    }
    
    @Test
    public void executeWithErrorTest(
            @Mocked final IvrAction action1,
            @Mocked final IvrAction action2
    ) throws Exception 
    {
        new Expectations() {{
            action1.getName(); result = "action1";
            action1.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = new Exception("Test action exceptions");
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(action1, action2));
        Thread.sleep(500);
    }
    
    
    
    @Test(timeout = IvrActionExecutorFacade.CANCEL_TIMEOUT*2)
    public void cancelTimeoutTest(
            @Mocked final IvrAction action
    ) throws Exception 
    {
        new Expectations(){{
            action.getName(); result="action";
            action.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = new Delegate() {
                void execute(IvrEndpointConversation conversation, ActionStopListener listener, LoggerHelper logger) {
                }
            };
            action.cancel();
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(action));
        Thread.sleep(100); //дождемся что бы действие запустилось
        final long ts = System.currentTimeMillis();
        actionsExecutor.cancelActionsExecution();
        assertTrue(System.currentTimeMillis()-ts>=IvrActionExecutorFacade.CANCEL_TIMEOUT);
    }
    
    @Test(timeout = IvrActionExecutorFacade.CANCEL_TIMEOUT*2)
    public void normalCancelTest(
            @Mocked final IvrAction action
    ) throws Exception 
    {
        new Expectations(){{
            final AtomicReference<ActionStopListener> stopListener = new AtomicReference<>();
            action.getName(); result="action";
            action.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = new Delegate() {
                void execute(IvrEndpointConversation conversation, ActionStopListener listener, LoggerHelper logger) {
                    stopListener.set(listener);
                }
            };
            action.cancel(); result = new Delegate() {
              void cancel() {
                    stopListener.get().actionExecuted(action);
              }  
            };
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(action));
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
        new Expectations() {{
            conversation.continueConversation('-');
            conversation.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.put(IvrEndpointConversation.DTMFS_BINDING, Arrays.asList('2', '1')); result = null;
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        List<IvrAction> actions = new ArrayList<>();
        actions.add(new PauseAction(1000));
        actions.add(new DtmfProcessPointAction("12"));
        actions.add(new PauseAction(1000));
        actionsExecutor.executeActions(actions);
        assertTrue(actionsExecutor.hasDtmfProcessPoint('2'));
        assertTrue(actionsExecutor.hasDtmfProcessPoint('1'));
        assertFalse(actionsExecutor.hasDtmfProcessPoint('-'));
        Thread.sleep(1100);
        assertFalse(actionsExecutor.hasDtmfProcessPoint('1'));        
    }
    
    @Test
    public void flowControlTest(
            @Mocked final IvrAction action1,
            @Mocked final IvrAction action2,
            @Mocked final IvrAction action3
    ) throws Exception 
    {
        new Expectations() {{
            action1.getName(); result = "action1";
            action1.isFlowControlAction(); result = true;
            action1.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = createExecuteDelegate(action1);
            //
            //action2 not used
            //
            action3.getName(); result = "action3";
            action3.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=1;
            result = createExecuteDelegate(action3);
        }};
        IvrActionExecutorFacade actionsExecutor = new IvrActionExecutorFacade(conversation, logger);
        actionsExecutor.executeActions(Arrays.asList(action1,action2));
        Thread.sleep(100);
        actionsExecutor.executeActions(Arrays.asList(action3));
        Thread.sleep(100);
        new Verifications(){{
            action2.execute(conversation, (ActionStopListener)any, (LoggerHelper)any); times=0;
        }};
    }
    
    private Delegate createExecuteDelegate(final IvrAction action) {
        return new Delegate() {
                void execute(IvrEndpointConversation conversation, ActionStopListener listener, LoggerHelper logger) {
                    listener.actionExecuted(action);
                }
            };
    }
}
