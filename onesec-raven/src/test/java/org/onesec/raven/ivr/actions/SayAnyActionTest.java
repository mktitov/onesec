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

import java.util.ArrayList;
import java.util.List;
import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onesec.raven.Constants;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SayAnyActionTest extends SayWordsActionTestCase {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    private SayAnyActionNode actionNode;
    
    @Before
    public void prepare() throws Exception {
        actionNode = new SayAnyActionNode();
        actionNode.setName("say any action");        
        testsNode.addAndSaveChildren(actionNode);
        actionNode.getAttr(SayAnyActionNode.WORDS_NODE_ATTR).setValue(Constants.TIME_WORDS_RESOURCE);
    }
    
    @Test
    public void test(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state, 
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
        }};
        actionNode.setActionsSequence("#wp=-200;r=(...)(...)(..)(..):9128672947 #12 ^минут #6 ^wp=-100:часов,прошлого,года @3s $wp=-150:123.45");
        assertTrue(actionNode.start());
        SayAnyAction action = (SayAnyAction) actionNode.createAction();
        assertNotNull(action);
        List sentences = action.formWords(conv);
        assertNotNull(sentences);
        assertEquals(10, sentences.size());
        checkSentence(sentences.get(0), 0, -200, new String[]{"900", "12"});
        checkSentence(sentences.get(1), 0, -200, new String[]{"800", "60", "7"});
        checkSentence(sentences.get(2), 0, -200, new String[]{"20", "9"});
        checkSentence(sentences.get(3), 0, -200, new String[]{"40", "7"});
        checkSentence(sentences.get(4), 0, actionNode.getNumbersWordPause(), new String[]{"12"});
        checkSentence(sentences.get(5), actionNode.getWordsSentencePause(), actionNode.getWordsWordPause(), new String[]{"минут"});
        checkSentence(sentences.get(6), 0, actionNode.getNumbersWordPause(), new String[]{"6"});
        checkSentence(sentences.get(7), actionNode.getWordsSentencePause(), -100, new String[]{"часов","прошлого","года"});
        assertTrue(sentences.get(8) instanceof AbstractSayWordsAction.Pause);
        assertEquals(3000l, ((AbstractSayWordsAction.Pause)sentences.get(8)).getPause());
        checkSentence(sentences.get(9), 0, -150, new String[]{"100", "20", "3", "рубля", "40", "5", "копеек"});
    }    
 }
