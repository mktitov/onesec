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
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import static org.easymock.EasyMock.*;
import org.junit.Before;

/**
 *
 * @author Mikhail Titov
 */
public class UnparkAbonentCallActionTest extends OnesecRavenTestCase {
    UnparkAbonentCallActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new UnparkAbonentCallActionNode();
        actionNode.setName("action");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setLogLevel(LogLevel.TRACE);
        assertTrue(actionNode.start());
    }
    
    @Test(expected=Exception.class)
    public void notFoundParkDNTest() throws Exception {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(ParkOperatorCallAction.PARK_NUMBER_BINDING)).andReturn(null);
        replay(conv, state, bindings);
        
        UnparkAbonentCallAction action = (UnparkAbonentCallAction) actionNode.createAction();
        action.doExecute(conv);
        verify(conv, state, bindings);
    }
    
    @Test
    public void normalTest() throws Exception {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(ParkOperatorCallAction.PARK_NUMBER_BINDING)).andReturn("1234");
        conv.unpark("1234");
        replay(conv, state, bindings);
        
        UnparkAbonentCallAction action = (UnparkAbonentCallAction) actionNode.createAction();
        action.doExecute(conv);
        verify(conv, state, bindings);
    }
}
