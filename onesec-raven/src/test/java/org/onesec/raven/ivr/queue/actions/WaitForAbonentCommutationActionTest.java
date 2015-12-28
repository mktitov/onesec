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
package org.onesec.raven.ivr.queue.actions;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.actions.ActionTestCase;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class WaitForAbonentCommutationActionTest extends ActionTestCase {
    
    private WaitForAbonentCommutationActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new WaitForAbonentCommutationActionNode();
        actionNode.setName("wait for commutation action");
        testsNode.addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void withoutCommutationManagerTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
    ) throws Exception {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
    @Test
    public void normalTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final AbonentCommutationManager commutationManager
    ) throws Exception 
    {
        new Expectations() {{
            bindings.get(AbonentCommutationManager.ABONENT_COMMUTATION_MANAGER_BINDING); result = commutationManager;
            commutationManager.abonentReadyToCommutate(conv);
            commutationManager.addRequestListener((CallQueueRequestListener) withNotNull()); result = new Delegate() {
                public void addRequestListener(final CallQueueRequestListener listener) {
                    new Thread(new Runnable() {
                        @Override public void run() {
                            try {
                                Thread.sleep(150);
                                listener.disconnected();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(WaitForAbonentCommutationActionTest.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }).start();
                }
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT);
        action.send(executeMessage);
        assertFalse(actionExecutor.waitForMessage(100));
        assertTrue(actionExecutor.waitForMessage(100));
    }
}
