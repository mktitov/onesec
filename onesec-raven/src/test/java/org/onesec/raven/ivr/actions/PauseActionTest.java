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

import java.util.concurrent.TimeUnit;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
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
public class PauseActionTest extends ActionTestCase {
    
    
    @Test
    public void pauseTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws InterruptedException 
    {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade pauseAction = createAction(actionExecutor, new PauseAction(100, TimeUnit.MILLISECONDS));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, pauseAction);
        final long ts = System.currentTimeMillis();
        pauseAction.send(executeMessage);        
        actionExecutor.waitForMessage(110l);
        assertTrue((System.currentTimeMillis()-ts)>=100);
    }
    
    @Test
    public void pauseCancelTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws InterruptedException 
    {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade pauseAction = createAction(actionExecutor, new PauseAction(100, TimeUnit.MILLISECONDS));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, pauseAction);
        final long ts = System.currentTimeMillis();
        pauseAction.send(executeMessage);        
        pauseAction.send(Action.CANCEL);
        actionExecutor.waitForMessage(30l);
        assertTrue((System.currentTimeMillis()-ts)<=30);
        
    }
}