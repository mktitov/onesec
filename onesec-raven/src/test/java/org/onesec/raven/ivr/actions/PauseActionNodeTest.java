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

import java.util.concurrent.TimeUnit;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.test.TestDataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class PauseActionNodeTest extends ActionTestCase {
    
    @Test
    public void pauseTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws InterruptedException 
    {
        PauseActionNode actionNode = createActionNode(1, TimeUnit.SECONDS);
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        
        long startTs = System.currentTimeMillis();
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(1010l));
        assertTrue(System.currentTimeMillis()-startTs>=1000);
        
    }
    
    @Test
    public void cancelTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws InterruptedException 
    {
        PauseActionNode actionNode = createActionNode(1, TimeUnit.SECONDS);
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade action = createAction(actionExecutor, actionNode);
        
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action);
        action.send(executeMessage);
        action.send(Action.CANCEL);
        assertTrue(actionExecutor.waitForMessage(100l));
    }
    
    private PauseActionNode createActionNode(long interval, TimeUnit timeUnit) {
        PauseActionNode actionNode = new PauseActionNode();
        actionNode.setName("pause");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setInterval(interval);
        actionNode.setIntervalTimeUnit(timeUnit);
        assertTrue(actionNode.start());
        return actionNode;
    }
    
}
