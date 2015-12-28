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

package org.onesec.raven.ivr.actions;

import java.util.Locale;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionStatus;
import org.raven.conv.impl.ConversationScenarioStateImpl;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.impl.ResourcesNode;

/**
 *
 * @author Mikhail Titov
 */
public class PlayActionTestHelper extends OnesecRavenTestCase {
    protected TestEndpointConversationNode conv;
    protected ExecutorServiceNode executor;

    @Before
    public void prepare() throws Exception {
        ResourcesNode resources = (ResourcesNode) tree.getRootNode().getNode(ResourcesNode.NAME);
        resources.setDefaultLocale(new Locale("ru"));
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(40);
        executor.setMaximumQueueSize(2);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());

        conv = new TestEndpointConversationNode();
        conv.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(conv);
        conv.setExecutorService(executor);
        conv.setConversationScenarioState(new ConversationScenarioStateImpl());
        conv.setLogLevel(LogLevel.TRACE);
    }
    
    protected void waitForAction(IvrAction action) throws InterruptedException {
//        while (action.getStatus()!=IvrActionStatus.EXECUTED)
//            Thread.sleep(10);
    }
}
