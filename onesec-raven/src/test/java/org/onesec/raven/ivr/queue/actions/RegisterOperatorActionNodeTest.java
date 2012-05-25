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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import org.junit.*;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.test.PushOnDemandDataSource;
import static org.onesec.raven.ivr.IvrEndpointConversation.*;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.onesec.raven.ivr.queue.impl.*;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.PushOnDemandDataSourceListener;

/**
 *
 * @author Mikhail Titov
 */
public class RegisterOperatorActionNodeTest extends OnesecRavenTestCase {
    private CallsQueuesNode callQueues;
    private PushOnDemandDataSource ds;
    private RegisterOperatorActionNode action;
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
        
        CallsQueueOperatorNode oper = new CallsQueueOperatorNode();
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
        
        action = new RegisterOperatorActionNode();
        action.setName("authenticate operator action");
        parent.addAndSaveChildren(action);
        action.setCallsQueues(callQueues);
        assertTrue(action.start());
    }
    
    @Test 
    public void unauthorized_nullDtmfs_Test() {
        assertNull(action.getEffectiveChildrens());
    }
    
    @Test 
    public void unauthorized_emptyDtmfs_Test() {
        parent.addBinding(DTMFS_BINDING, Collections.EMPTY_LIST);
        assertNull(action.getEffectiveChildrens());
    }
    
    @Test 
    public void unauthorized_byAuthenticator_Test() {
        parent.addBinding(DTMFS_BINDING, Arrays.asList("1","2","3"));
        parent.addBinding(NUMBER_BINDING, "0000");
        PushOnDemandDataSourceListener listener = createMock(PushOnDemandDataSourceListener.class);
        listener.onGatherDataForConsumer(isA(DataConsumer.class), checkDataContext());
        replay(listener);
        ds.setListener(listener);
        assertNull(action.getEffectiveChildrens());
        verify(listener);
    }
    
    @Test 
    public void successAuth_Test() {
        parent.addBinding(DTMFS_BINDING, Arrays.asList("1","2","3"));
        parent.addBinding(NUMBER_BINDING, "0000");
        
        IMocksControl control = createStrictControl();
        PushOnDemandDataSourceListener listener = control.createMock(PushOnDemandDataSourceListener.class);
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings bindings = control.createMock(Bindings.class);
        Bindings bindings2 = control.createMock(Bindings.class);
        
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(false);
        listener.onGatherDataForConsumer(isA(DataConsumer.class), checkDataContext());
        state.setBinding(eq(RegisterOperatorActionNode.OPERATOR_BINDING), checkOperator()
                , eq(BindingScope.POINT));
        //test formExpressionBindings
        bindings2.putAll(anyObject(Map.class));
        expect(bindings2.get(CONVERSATION_STATE_BINDING)).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.containsKey(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn(true);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(RegisterOperatorActionNode.OPERATOR_BINDING)).andReturn("oper");
        expect(bindings2.put(RegisterOperatorActionNode.OPERATOR_BINDING, "oper")).andReturn(null);
        control.replay();
        
        parent.addBinding(CONVERSATION_STATE_BINDING, state);
        Map map = new HashMap();
        map.put(OperatorRegistratorNode.OPERATOR_DESC_FIELD, "Pupkin");
        ds.addDataPortion(map);
        ds.setListener(listener);
        assertNull(action.getEffectiveChildrens());
        //test formExpressionBindings
        action.formExpressionBindings(bindings2);
        
        control.verify();
    }
    
    public static DataContext checkDataContext() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                assertNotNull(o);
                assertTrue(o instanceof DataContext);
                DataContext dataContext = (DataContext) o;
                assertEquals("0000", dataContext.getAt(OperatorRegistratorNode.OPERATOR_NUMBER_BINDING));
                assertEquals("123", dataContext.getAt(OperatorRegistratorNode.OPERATOR_CODE_BINDING));
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }
    
    public static OperatorDesc checkOperator() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object o) {
                assertTrue(o instanceof OperatorDesc);
                OperatorDesc oper = (OperatorDesc) o;
                assertEquals("123", oper.getId());
                assertEquals("Pupkin", oper.getDesc());
                return true;
            }
            public void appendTo(StringBuffer sb) {
            }
        });
        return null;
    }
}
