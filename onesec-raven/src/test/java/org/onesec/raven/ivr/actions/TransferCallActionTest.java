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

import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class TransferCallActionTest extends ActionTestCase {
    private TransferCallActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new TransferCallActionNode();
        actionNode.setName("transfer action");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setAddress("1234");
        assertTrue(actionNode.start());
    }
    
    @Test
    public void test(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            conv.transfer("1234", false, 10000, 600000);
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(execMess);
        actionExecutor.waitForMessage(100);
    }    
}
