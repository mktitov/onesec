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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.Constants;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrAction;
import org.raven.conv.impl.ConversationScenarioStateImpl;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.ResourcesNode;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SayWordsActionTest extends OnesecRavenTestCase {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    
    private TestEndpointConversationNode conv;
    private SayWordsActionNode actionNode;
    private Node wordsNode;
    private ExecutorServiceNode executor;
    
    @Before
    public void prepare() throws Exception {
        ResourcesNode resources = (ResourcesNode) tree.getRootNode().getNode(ResourcesNode.NAME);
        resources.setDefaultLocale(new Locale("ru"));
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(40);
        executor.setMaximumPoolSize(40);
        assertTrue(executor.start());

        conv = new TestEndpointConversationNode();
        conv.setName("endpoint");
        testsNode.addAndSaveChildren(conv);
        conv.setExecutorService(executor);
        conv.setConversationScenarioState(new ConversationScenarioStateImpl());
        
        actionNode = new SayWordsActionNode();
        actionNode.setName("action node");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.getAttr(SayWordsActionNode.WORDS_NODE_ATTR).setValue(Constants.TIME_WORDS_RESOURCE);
    }
    
    @After
    public void afterTest() {
        conv.stop();
    }
    
    @Test
    public void test() throws Exception {
        conv.setFileName("target/words.wav");
        assertTrue(conv.start());
        actionNode.setWords("вчера сегодня минут минуты");
        assertTrue(actionNode.start());
        IvrAction action = actionNode.createAction();
        assertNotNull(action);
        action.execute(conv, null, logger);
        Thread.sleep(10000);
    }
}
