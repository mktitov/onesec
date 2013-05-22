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
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import static org.easymock.EasyMock.*;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
/**
 *
 * @author Mikhail Titov
 */
public class ParkOperatorCallActionTest extends OnesecRavenTestCase {
    private ParkOperatorCallActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new ParkOperatorCallActionNode();
        actionNode.setName("park action");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setLogLevel(LogLevel.TRACE);
        assertTrue(actionNode.start());
        
    }
    
    @Test(expected=Exception.class)
    public void notQueueOperatorConversationTest() throws Exception {
        IvrEndpointConversation conv = createMock("OperatorConversation", IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING)).andReturn(null);
        replay(conv, state, bindings);
        
        ParkOperatorCallAction action = (ParkOperatorCallAction) actionNode.createAction();
        action.doExecute(conv);
        verify(conv, state, bindings);
    }
    
    @Test
    public void normalTest() throws Exception {
        IvrEndpointConversation conv = createMock("OperatorConversation", IvrEndpointConversation.class);
        ConversationScenarioState state = createMock("OperatorConversationState", ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        CallQueueRequestController controller = createMock(CallQueueRequestController.class);
        IvrEndpointConversation abonConv = createMock("AbonentConversation", IvrEndpointConversation.class);
        ConversationScenarioState abonState = createMock("AbonentConversationState", ConversationScenarioState.class);
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING)).andReturn(controller);
        expect(conv.park()).andReturn("1234");
        expect(controller.getConversation()).andReturn(abonConv);
        expect(abonConv.getConversationScenarioState()).andReturn(abonState);
        abonState.setBinding(ParkOperatorCallAction.PARK_NUMBER_BINDING, "1234", BindingScope.POINT);
        replay(conv, state, bindings, controller, abonConv, abonState);
        
        ParkOperatorCallAction action = (ParkOperatorCallAction) actionNode.createAction();
        action.setLogger(new LoggerHelper(actionNode, null));
        action.doExecute(conv);
        verify(conv, state, bindings, controller, abonConv, abonState);
    }
}
