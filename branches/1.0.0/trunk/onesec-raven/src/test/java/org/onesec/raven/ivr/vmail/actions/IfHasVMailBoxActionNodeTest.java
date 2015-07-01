/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.vmail.actions;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.Constants;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailManager;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class IfHasVMailBoxActionNodeTest  extends OnesecRavenTestCase implements Constants {
    
    private IfHasVMailBoxActionNode action;
    private VMailManagerWrapper managerWrapper;
    private BindingSourceNode parent;
    private Node child;
    
    //mocks
    private ConversationScenarioState state;
    private Bindings bindings;
    private VMailManager manager;
    private VMailBox vbox;
    
    @Before
    public void prepare() {
        managerWrapper = new VMailManagerWrapper();
        managerWrapper.setName("manager");
        testsNode.addAndSaveChildren(managerWrapper);
        assertTrue(managerWrapper.start());
        
        parent  = new BindingSourceNode();
        parent.setName("parent");
        testsNode.addAndSaveChildren(parent);
        assertTrue(parent.start());
        
        action = new IfHasVMailBoxActionNode();
        action.setName("action node");
        parent.addAndSaveChildren(action);
        action.setVmailManager(managerWrapper);
        action.setLogLevel(LogLevel.TRACE);
        assertTrue(action.start());
        
        child = new BaseNode("child");
        action.addAndSaveChildren(child);
        assertTrue(child.start());
    }
    
    @Test
    public void notStartedTest() {
        action.stop();
        assertNull(action.getEffectiveNodes());
    }
    
    @Test
    public void nullConversationStateTest() {
        assertNull(action.getEffectiveNodes());
    }
    
    @Test
    public void nullCallingNumberTest() {
        IMocksControl control = createStage1();
        control.replay();
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        assertNull(action.getEffectiveNodes());
        control.verify();
    }
    
    @Test
    public void vboxNotFoundTest() {
        IMocksControl control = createStage2();
        control.replay();
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        assertNull(action.getEffectiveNodes());
        control.verify();
    }
    
    @Test
    public void successTest() {
        IMocksControl control = createStage3();
        control.replay();
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        Collection<Node> childs = action.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        Bindings bindings = new SimpleBindings();
        action.formExpressionBindings(bindings);
        assertSame(vbox, bindings.get(VMAIL_BOX));
        assertEquals("111", bindings.get(VMAIL_BOX_NUMBER));
        control.verify();
    }
    
    private IMocksControl createStage1() {
        IMocksControl control = createControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        
        expect(state.getBindings()).andReturn(bindings).atLeastOnce();
        expect(bindings.get(IvrEndpointConversation.LAST_REDIRECTED_NUMBER)).andReturn(null);
        expect(bindings.get(IvrEndpointConversation.NUMBER_BINDING)).andReturn("123");
                
        return control;
    }
    
    private IMocksControl createStage2() {
        IMocksControl control = createControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        manager = control.createMock(VMailManager.class);
        managerWrapper.setManager(manager);
        
        expect(state.getBindings()).andReturn(bindings).atLeastOnce();
        expect(bindings.get(IvrEndpointConversation.LAST_REDIRECTED_NUMBER)).andReturn("111");
        expect(manager.getVMailBox("111")).andReturn(null);
                
        return control;
    }
    
    private IMocksControl createStage3() {
        IMocksControl control = createControl();
        state = control.createMock(ConversationScenarioState.class);
        bindings = control.createMock(Bindings.class);
        manager = control.createMock(VMailManager.class);
        managerWrapper.setManager(manager);
        vbox = control.createMock(VMailBox.class);
        
        expect(state.getBindings()).andReturn(bindings).atLeastOnce();
        expect(bindings.get(IvrEndpointConversation.LAST_REDIRECTED_NUMBER)).andReturn("111");
        expect(manager.getVMailBox("111")).andReturn(vbox);
        state.setBinding(VMAIL_BOX, vbox, BindingScope.CONVERSATION);
        state.setBinding(VMAIL_BOX_NUMBER, "111", BindingScope.CONVERSATION);
        expect(bindings.containsKey(VMAIL_BOX)).andReturn(Boolean.TRUE);
        expect(bindings.get(VMAIL_BOX)).andReturn(vbox);
        expect(bindings.get(VMAIL_BOX_NUMBER)).andReturn("111");
        return control;
    }
}
