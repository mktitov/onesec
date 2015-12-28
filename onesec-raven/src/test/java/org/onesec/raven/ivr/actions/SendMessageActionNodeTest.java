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
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.SendMessageDirection;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SendMessageActionNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test(
            @Mocked final Action.Execute execMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state) 
        throws Exception
    {
        final Bindings bindings = new SimpleBindings();
        bindings.put("greeting", "World!");
        new Expectations() {{
            state.getBindings(); result = bindings;
        }};
        
        SendMessageActionNode actionNode = new SendMessageActionNode();
        actionNode.setName("actionNode");
        testsNode.addAndSaveChildren(actionNode);
        NodeAttribute attr = actionNode.getAttr("message");
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("'Hello '+greeting");
        actionNode.setSendDirection(SendMessageDirection.CALLING_PARTY);
        assertTrue(actionNode.start());

        SendMessageAction action = (SendMessageAction) actionNode.createAction();

        assertSame(action.processExecuteMessage(execMessage), AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        
        new Verifications() {{
            conv.sendMessage("Hello World!", Charset.forName("windows-1251").name(), SendMessageDirection.CALLING_PARTY);
        }};

//        verify(conv, state);
    }
}