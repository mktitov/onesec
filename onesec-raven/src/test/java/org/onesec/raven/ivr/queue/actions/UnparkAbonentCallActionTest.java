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
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class UnparkAbonentCallActionTest extends ActionTestCase {
    UnparkAbonentCallActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new UnparkAbonentCallActionNode();
        actionNode.setName("action");
        testsNode.addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void notFoundParkDNTest(
            @Mocked final Action.Execute executeMessage,
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
    ) throws Exception 
    {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_STOP);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
    @Test
    public void normalTest(
            @Mocked final Action.Execute executeMessage,
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
    ) throws Exception 
    {
        new Expectations() {{
            bindings.get(ParkOperatorCallAction.PARK_NUMBER_BINDING); result = "123";
            conv.unpark("123");
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_STOP);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
}
