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
package org.onesec.raven.ivr.conference.actions;

import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.onesec.raven.ivr.conference.ConferenceManager;
import static org.onesec.raven.ivr.conference.actions.JoinToConferenceAction.CONFERENCE_STATE_BINDING;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class JoinToConferenceActionTest extends ActionTestCase {
    
    @Test
    public void firstPassTest(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final ConferenceManager confManager) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(JoinToConferenceAction.CONFERENCE_STATE_BINDING); result = null;            
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, new JoinToConferenceAction(confManager, Boolean.TRUE, Boolean.TRUE, "123", "321"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(execMess);
        actionExecutor.waitForMessage(100);
        
        new Verifications() {{
            state.setBinding(CONFERENCE_STATE_BINDING, withInstanceOf(ConferenceSessionState.class), BindingScope.POINT);
            confManager.join(conv, "123", "321", withInstanceOf(ConferenceSessionState.class));
        }};
    }    
    
    @Test
    public void autoConnectTest(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final ConferenceSessionState confState,
            @Mocked final ConferenceManager confManager) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(JoinToConferenceAction.CONFERENCE_STATE_BINDING); result = confState;
            confState.getStatus(); result = ConferenceSessionStatus.JOINED;
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, new JoinToConferenceAction(confManager, Boolean.TRUE, Boolean.TRUE, "123", "321"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(execMess);
        actionExecutor.waitForMessage(100);
        
        new Verifications() {{
            confState.connect();
            confState.unmute(); times = 0;
        }};
    }    
    
    @Test
    public void autoUnmuteTest(
            @Mocked final DataProcessor executeActionDP,
            @Mocked final Action.Execute execMess,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final ConferenceSessionState confState,
            @Mocked final ConferenceManager confManager) 
        throws Exception 
    {
        new Expectations() {{
            execMess.getConversation(); result = conv;
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindings.get(JoinToConferenceAction.CONFERENCE_STATE_BINDING); result = confState;
            confState.getStatus(); result = ConferenceSessionStatus.CONNECTED;
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(executeActionDP);
        DataProcessorFacade action = createAction(actionExecutor, new JoinToConferenceAction(confManager, Boolean.TRUE, Boolean.TRUE, "123", "321"));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(execMess);
        actionExecutor.waitForMessage(100);
        
        new Verifications() {{
            confState.connect(); times = 0;
            confState.unmute(); times = 1;
        }};
    }    
}
