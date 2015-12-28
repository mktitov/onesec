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

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class StopConversationActionTest extends ActionTestCase {
    @Test
    public void test(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv) 
        throws Exception 
    {
        StopConversationActionNode actionNode = new StopConversationActionNode();
        actionNode.setName("stop conversation");
        testsNode.addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
        
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_STOP);
        action.send(execMess);
        actionExecutor.waitForMessage(100);
        Thread.sleep(100);
        
        new Verifications() {{
            conv.stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
        }};
    }
}
