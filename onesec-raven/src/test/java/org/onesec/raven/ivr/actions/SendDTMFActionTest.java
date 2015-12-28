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

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
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
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SendDTMFActionTest extends ActionTestCase {
    private SendDTMFActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new SendDTMFActionNode();
        actionNode.setName("send dtmfs");        
        testsNode.addAndSaveChildren(actionNode);
    }
    
    @Test
    public void test(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv) 
        throws Exception 
    {
        final String dtmfs = "234";
        new Expectations() {{
//            executeMessage.getConversation(); result = conv;
//            conv.getConversationScenarioState(); result = state;
//            state.switchToNextConversationPoint();
//            conv.sendDTMF(dtmfs);
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        actionNode.setDtmfs(dtmfs);
        assertTrue(actionNode.start());
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(executeMessage);
        actionExecutor.waitForMessage(100);        
        
        new Verifications() {{
            conv.sendDTMF(dtmfs);
        }};
    }
    
    @Test
    public void cancelTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv) 
        throws Exception 
    {
        final String dtmfs = "234";
        new Expectations() {{
//            executeMessage.getConversation(); result = conv;
//            conv.getConversationScenarioState(); result = state;
//            state.switchToNextConversationPoint();
            conv.sendDTMF(dtmfs); result = new Delegate() {
                void sendDTMF(String dtmfs) throws InterruptedException {
                    Thread.currentThread().sleep(500);
                }
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        actionNode.setDtmfs(dtmfs);
        assertTrue(actionNode.start());
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(executeMessage);
        actionExecutor.waitForMessage(100);        
        
        new Verifications() {{
            conv.sendDTMF(dtmfs);
        }};
    }
}
