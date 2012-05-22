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

import java.util.Collection;
import java.util.List;
import javax.script.Bindings;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.RavenUtils;
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
    }
    
    @Test
    public void test() {
        String tempDtmfsKey = RavenUtils.generateKey("dtmfs", collector);
        
        IMocksControl control = createStrictControl();
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings bindings = control.createMock(Bindings.class);
        List dtmfs = control.createMock(List.class);
        
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
        
        control.replay();
        
        //testing
        parent.addBinding(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        assertNull(collector.getEffectiveChildrens());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "1");
        assertNull(collector.getEffectiveChildrens());
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "*");
        Collection<Node> childs = collector.getEffectiveChildrens();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        
        control.verify();
    }
}
