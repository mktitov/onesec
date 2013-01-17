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
package org.onesec.raven.ivr.queue.actions;

import javax.script.Bindings;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.SayNumberActionNode;
import org.raven.expr.impl.IfNode;
import org.raven.test.BindingsContainer;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.raven.conv.ConversationScenarioState;

/**
 *
 * @author Mikhail Titov
 */
public class SayTimeLeftForAnswerActionNodeTest extends OnesecRavenTestCase {
    private SayTimeLeftForAnswerActionNode sayNode;
    private BindingsContainer container;
    
    @Before
    public void prepare() {
        container = new BindingsContainer();
        container.setName("root");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());
        
        sayNode = new SayTimeLeftForAnswerActionNode();
        sayNode.setName("say minutes left");
        container.addAndSaveChildren(sayNode);
    }

    @Test
    public void startTest() {
        assertTrue(sayNode.start());
        Node node = sayNode.getChildren(SayTimeLeftForAnswerActionNode.NODE1_NAME);
        assertTrue(node instanceof PlayAudioActionNode);
        
        //check if minutesLeft>1
        node = sayNode.getChildren(SayTimeLeftForAnswerActionNode.IFNODE1_NAME);
        assertTrue(node instanceof IfNode);
        assertStarted(node);
        assertFalse(((IfNode)node).getUsedInTemplate());
        Node node2 = node.getChildren(SayTimeLeftForAnswerActionNode.NODE2_NAME);
        assertTrue(node2 instanceof SayNumberActionNode);
        assertStarted(node2);
        node2 = node.getChildren(SayTimeLeftForAnswerActionNode.NODE3_NAME);
        assertTrue(node2 instanceof PlayAudioActionNode);
        assertStarted(node2);
        
        //check if minutesLeft==1
        node = sayNode.getChildren(SayTimeLeftForAnswerActionNode.IFNODE2_NAME);
        assertTrue(node instanceof IfNode);
        assertStarted(node);
        assertFalse(((IfNode)node).getUsedInTemplate());
        node2 = node.getChildren(SayTimeLeftForAnswerActionNode.NODE4_NAME);
        assertTrue(node2 instanceof PlayAudioActionNode);
        assertStarted(node2);
    }
    
    @Test
    public void nullConversationStateTest() throws Exception {
        assertNull(sayNode.getEffectiveChildrens());
    }
    
    @Test
    public void earlyLastInformTime() throws Exception {
        IMocksControl control = createControl();
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings bindings = control.createMock(Bindings.class);
        
        expect(state.getBindings()).andReturn(bindings);
//        expect()
        control.replay();
        
        control.verify();
    }
}
