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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import static org.onesec.raven.ivr.queue.actions.ParkOperatorCallAction.PARK_NUMBER_BINDING;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.log.LogLevel;
import org.raven.test.TestDataProcessorFacade;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class ParkOperatorCallActionTest extends ActionTestCase {
    private ParkOperatorCallActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new ParkOperatorCallActionNode();
        actionNode.setName("park action");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setLogLevel(LogLevel.TRACE);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void notQueueOperatorConversationTest(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallQueueRequestController requestController) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING); result = null;            
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_STOP);
        action.send(execMess);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
    @Test
    public void normalTest(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallQueueRequestController requestController) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING); result = requestController;            
            conv.park(); result = "123";
            state.setBinding(PARK_NUMBER_BINDING, "123", BindingScope.POINT);
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_STOP);
        action.send(execMess);
        assertTrue(actionExecutor.waitForMessage(100));
    }
}
