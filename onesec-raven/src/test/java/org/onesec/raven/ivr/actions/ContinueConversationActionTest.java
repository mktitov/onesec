/*
 * Copyright 2015 Mikhail Titov.
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

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail TItov
 */
@RunWith(JMockit.class)
public class ContinueConversationActionTest extends ActionTestCase {
    
    @Test
    public void executeTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.switchToNextConversationPoint();
            conv.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new ContinueConversationAction());
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(executeMessage);
        actionExecutor.waitForMessage(100);
        Thread.sleep(100); //wating for conv.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    @Test
    public void cancelTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws Exception 
    {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, new ContinueConversationAction());
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(Action.CANCEL);
        actionExecutor.waitForMessage(100);
    }
}
