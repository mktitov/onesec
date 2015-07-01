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
package org.onesec.raven.ivr.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.script.Bindings;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class CollectDtmfsActionNodeTest extends OnesecRavenTestCase {
    
    private CollectDtmfsActionNode collector;
    private BindingSourceNode parent;
    private BaseNode child;
    private BaseNode errorHandler;
    private String tempDtmfsKey;
    private String lastTsKey;
    //mocks
    private IMocksControl control;
    private ConversationScenarioState state;
    private Bindings bindings;
    private List dtmfs;
    
    @Before
    public void prepare() {
        parent = new BindingSourceNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);
        assertTrue(parent.start());
        
        collector = new CollectDtmfsActionNode();
        collector.setName("dtmf collector");
        parent.addAndSaveChildren(collector);
        assertTrue(collector.start());
        
        child = new BaseNode("child");
        collector.addAndSaveChildren(child);
        assertTrue(child.start());
        
        errorHandler = new BaseNode("errorHandler");
        collector.getErrorHandler().addAndSaveChildren(errorHandler);
        assertTrue(errorHandler.start());
        
        tempDtmfsKey = collector.getTempDtmfsKey();
        lastTsKey = collector.getLastTsKey();
    }
    
//    @Test
    public void test() {
        createMocks();
        control.replay();
        doTest();
        
        control.verify();
    }
    
//    @Test
    public void maxDtmfsCountTest() {
        createMocksForMaxDtmfsCount();
        control.replay();
        
        collector.setMaxDtmfsCount(1);
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        assertNull(collector.getEffectiveNodes());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "1");
        Collection<Node> childs = collector.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        
        control.verify();
    }
    
//    @Test
    public void autoJoinTest() {
        createMocksForAutoJoin();
        control.replay();
        //testing
        collector.setAutoJoin(Boolean.TRUE);
        doTest();
        
        control.verify();
    }

//    @Test
    public void postProcessTest() {
        createMocksForPostProccess();
        control.replay();
        //testing
        collector.setAutoJoin(Boolean.TRUE);
        collector.setUsePostProcess(Boolean.TRUE);
        collector.setPostProcess("dtmfs.toInteger()+1");
        doTest();
        
        control.verify();
    }

//    @Test
    public void postProcessValidationErrorTest() {
        createMocksForPostProccess2();
        control.replay();
        //testing
        collector.setAutoJoin(Boolean.TRUE);
        collector.setUsePostProcess(Boolean.TRUE);
        collector.setPostProcess("VALIDATION_ERROR");
        doTest(errorHandler);        
        control.verify();
    }
    
    @Test
    public void autoStopTest() {
        createMocksForAutoStop();
        control.replay();
        
        collector.setAutoStopDelay(4l);
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        assertNull(collector.getEffectiveNodes());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "1");
        assertNull(collector.getEffectiveNodes());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        Collection<Node> childs = collector.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        
        control.verify();
    }

    public void createMocks() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        //on sending *
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
    }
    
    public void createMocksForMaxDtmfsCount() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        expect(dtmfs.size()).andReturn(1);
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
    }
    
    public void createMocksForAutoJoin() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        //on sending *
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));      
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
        expect(dtmfs.iterator()).andReturn(Arrays.asList("1").iterator());
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), eq("1"), eq(BindingScope.POINT));
    }

    public void createMocksForPostProccess() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        //on sending *
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));      
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
        expect(dtmfs.iterator()).andReturn(Arrays.asList("1").iterator());
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), eq(new Integer(2)), eq(BindingScope.POINT));
    }
    
    public void createMocksForPostProccess2() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        //on sending *
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));      
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
        expect(dtmfs.iterator()).andReturn(Arrays.asList("1").iterator());
    }
    
    public void createMocksForAutoStop() {
        control = createStrictControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        dtmfs = control.createMock(List.class);
        
        //on sending empty dtmf
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(null);
        state.setBinding(eq(tempDtmfsKey), isA(List.class), eq(BindingScope.POINT));
        state.setBinding(eq(IvrEndpointConversation.DTMFS_BINDING), isA(List.class), eq(BindingScope.POINT));
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(lastTsKey)).andReturn(null);        
        //on sending 1
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(dtmfs.add("1")).andReturn(Boolean.TRUE);
        state.setBinding(eq(lastTsKey), isA(Long.class), eq(BindingScope.POINT));
        //on sending -
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(tempDtmfsKey)).andReturn(dtmfs);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(lastTsKey)).andReturn(System.currentTimeMillis()-5000);        
        state.setBinding(eq(tempDtmfsKey), isNull(), eq(BindingScope.POINT));
        state.setBinding(eq(lastTsKey), isNull(), eq(BindingScope.POINT));
        expect(dtmfs.isEmpty()).andReturn(false);
    }

    public void doTest() {
        doTest(child);
    }
    
    private void doTest(Node resultAction) {
        //testing
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        assertNull(collector.getEffectiveNodes());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "1");
        assertNull(collector.getEffectiveNodes());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "*");
        Collection<Node> childs = collector.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(resultAction, childs.iterator().next());
    }
    
}
