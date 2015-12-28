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

import java.util.List;
import java.util.Locale;
import javax.script.Bindings;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.Constants;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.ResourcesNode;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SayWordsActionTest extends OnesecRavenTestCase {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    
    private SayWordsActionNode actionNode;
    
    @Before
    public void prepare() throws Exception {
        ResourcesNode resources = (ResourcesNode) tree.getRootNode().getNode(ResourcesNode.NAME);
        resources.setDefaultLocale(new Locale("ru"));
                
        actionNode = new SayWordsActionNode();
        actionNode.setName("action node");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.getAttr(SayWordsActionNode.WORDS_NODE_ATTR).setValue(Constants.TIME_WORDS_RESOURCE);
    }
    
    @Test
    public void test(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {        
        actionNode.setWords("вчера сегодня минут минуты");
        assertTrue(actionNode.start());
        SayWordsAction action = (SayWordsAction) actionNode.createAction();
        assertNotNull(action);
        assertEquals(actionNode.getPauseBetweenWords().longValue(), action.getPauseBetweenWords());
        assertEquals(0l, action.getPauseBetweenWordsGroups());
        List sentences = action.formWords(conv);
        assertNotNull(sentences);
        assertEquals(1, sentences.size());
        assertTrue(sentences.get(0) instanceof List);
        assertArrayEquals(new String[]{"вчера", "сегодня", "минут", "минуты"}, ((List)sentences.get(0)).toArray());        
    }
}
