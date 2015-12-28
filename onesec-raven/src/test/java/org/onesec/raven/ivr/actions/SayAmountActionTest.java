/*
 *  Copyright 2009 Mikhail Titov.
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

import java.util.List;
import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SayAmountActionTest extends ActionTestCase
{
    @Test
    public void test(
            @Mocked final IvrEndpointConversation conversation,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        new Expectations(){{
            conversation.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
        }};
        SayAmountActionNode actionNode = new SayAmountActionNode();
        actionNode.setName("action node");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setAmount(123.4);
        actionNode.getAttr("numbersNode").setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        actionNode.setNumbersNode(testsNode);
        assertTrue(actionNode.start());
        
        SayAmountAction action = (SayAmountAction) actionNode.createAction();
        List<List> numbers = action.formWords(conversation);
        assertNotNull(numbers);
        assertEquals(1, numbers.size());
        assertArrayEquals(new Object[]{"100", "20", "3", "рубля", "40", "копеек"}, numbers.get(0).toArray());
        System.out.println("numbers: "+numbers.get(0));
    }
}