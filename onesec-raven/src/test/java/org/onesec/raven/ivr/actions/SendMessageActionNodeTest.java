/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.actions;

import java.nio.charset.Charset;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.SendMessageDirection;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class SendMessageActionNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test() throws Exception
    {
        SendMessageActionNode actionNode = new SendMessageActionNode();
        actionNode.setName("actionNode");
        tree.getRootNode().addAndSaveChildren(actionNode);
        NodeAttribute attr = actionNode.getNodeAttribute("message");
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("'Hello '+greeting");
        actionNode.setSendDirection(SendMessageDirection.CALLING_PARTY);
        assertTrue(actionNode.start());

        SendMessageAction action = (SendMessageAction) actionNode.createAction();

        Bindings bindings = new SimpleBindings();
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);

        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        conv.sendMessage("Hello World!",
                Charset.forName("windows-1251").name(), SendMessageDirection.CALLING_PARTY);
        replay(conv, state);

        bindings.put("greeting", "World!");
        action.doExecute(conv);

        verify(conv, state);
    }
}