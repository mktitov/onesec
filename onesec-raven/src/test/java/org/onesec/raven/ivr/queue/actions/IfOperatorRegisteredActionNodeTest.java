/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.queue.actions;

import java.util.Map;
import javax.script.Bindings;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import static org.onesec.raven.ivr.IvrEndpointConversation.CONVERSATION_STATE_BINDING;
import static org.onesec.raven.ivr.IvrEndpointConversation.NUMBER_BINDING;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.onesec.raven.ivr.queue.impl.CallsQueueOperatorNode;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.onesec.raven.ivr.queue.impl.TestConversationsBridgeManager;
import org.onesec.raven.ivr.queue.impl.TestEndpointPool;
import org.raven.BindingNames;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.PushOnDemandDataSource;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class IfOperatorRegisteredActionNodeTest extends OnesecRavenTestCase implements BindingNames {
    private CallsQueuesNode callQueues;
    private PushOnDemandDataSource ds;
    private CallsQueueOperatorNode oper;
    private IfOperatorRegisteredActionNode action;
    private BindingSourceNode parent;
    
    @Before
    public void prepare(){
        callQueues = new CallsQueuesNode();
        callQueues.setName("call queues");
        tree.getRootNode().addAndSaveChildren(callQueues);
        assertTrue(callQueues.start());
        
        TestEndpointPool pool = new TestEndpointPool();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        assertTrue(pool.start());

        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());
        
        TestConversationsBridgeManager bridgeManager = new TestConversationsBridgeManager();
        bridgeManager.setName("conversation bridge");
        tree.getRootNode().addAndSaveChildren(bridgeManager);
        assertTrue(bridgeManager.start());
        
        oper = new CallsQueueOperatorNode();
        oper.setName("oper");
        callQueues.getOperatorsNode().addAndSaveChildren(oper);
        oper.setPhoneNumbers("0000");        
        oper.setEndpointPool(pool);
        oper.setExecutor(executor);
        oper.setConversationsBridgeManager(bridgeManager);
        assertTrue(oper.start());
        
        ds = new PushOnDemandDataSource();
        ds.setName("auth ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        callQueues.getOperatorRegistrator().setDataSource(ds);
        assertTrue(callQueues.getOperatorRegistrator().start());
        
        parent = new BindingSourceNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);
        assertTrue(parent.start());
        
        action = new IfOperatorRegisteredActionNode();
        action.setName("if perator registered");
        parent.addAndSaveChildren(action);
        action.setCallsQueues(callQueues);
        assertTrue(action.start());
        
        BaseNode child = new BaseNode("child");
        action.addAndSaveChildren(child);
        assertTrue(child.start());
    }
    
    
    @Test
    public void unsuccessTest() {
        parent.addBinding(NUMBER_BINDING, "0000");
        
        IMocksControl control = createStrictControl();
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings stateBindings = control.createMock(Bindings.class);
        Bindings bindings2 = control.createMock(Bindings.class);
        
        expect(state.getBindings()).andReturn(stateBindings);
        expect(stateBindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(false);
        state.setBinding(eq(RegisterOperatorActionNode.OPERATOR_BINDING), isNull(), eq(BindingScope.CONVERSATION));
        //test formExpressionBindings
        bindings2.putAll(anyObject(Map.class));
        expect(bindings2.get(CONVERSATION_STATE_BINDING)).andReturn(state);
        expect(state.getBindings()).andReturn(stateBindings);
        expect(stateBindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(false);
//        expect(state.getBindings()).andReturn(stateBindings);
//        expect(stateBindings.get(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn("oper");
//        expect(bindings2.put(RegisterOperatorActionNode.OPERATOR_BINDING, "oper")).andReturn(null);
        control.replay();
        
        parent.addBinding(CONVERSATION_STATE_BINDING, state);
        assertNull(action.getEffectiveChildrens());
        //test formExpressionBindings
        action.formExpressionBindings(bindings2);
        
        control.verify();
        
    }
    
    @Test
    public void successTest() {
        parent.addBinding(NUMBER_BINDING, "0000");
        oper.setPersonDesc("Person desc");
        oper.setPersonId("Person id");
        
        IMocksControl control = createStrictControl();
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings stateBindings = control.createMock("stateBindings", Bindings.class);
        Bindings bindings2 = control.createMock("nodeBindigns", Bindings.class);
        
        expect(state.getBindings()).andReturn(stateBindings);
        expect(stateBindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(false);
        state.setBinding(eq(RegisterOperatorActionNode.OPERATOR_BINDING), checkOperator(), eq(BindingScope.CONVERSATION));
        //test formExpressionBindings
        bindings2.putAll(anyObject(Map.class));
        expect(bindings2.get(CONVERSATION_STATE_BINDING)).andReturn(state);
        expect(state.getBindings()).andReturn(stateBindings);
        expect(stateBindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(true);
        expect(state.getBindings()).andReturn(stateBindings);
        expect(stateBindings.get(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn("oper");
        expect(bindings2.put(RegisterOperatorActionNode.OPERATOR_BINDING, "oper")).andReturn(null);
        control.replay();
        
        parent.addBinding(CONVERSATION_STATE_BINDING, state);
        assertNotNull(action.getEffectiveChildrens());
        //test formExpressionBindings
        action.formExpressionBindings(bindings2);
        
        control.verify();
    }
    
    public static OperatorDesc checkOperator() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                assertTrue(o instanceof OperatorDesc);
                OperatorDesc oper = (OperatorDesc) o;
                assertEquals("Person id", oper.getId());
                assertEquals("Person desc", oper.getDesc());
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }
}
