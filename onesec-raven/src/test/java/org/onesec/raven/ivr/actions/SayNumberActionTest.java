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
import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.impl.Genus;
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
public class SayNumberActionTest extends SayWordsActionTestCase {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    private SayNumberActionNode actionNode;
    
    @Before
    public void prepare() throws Exception {
        actionNode = new SayNumberActionNode();
        actionNode.setName("say number");
        tree.getRootNode().addAndSaveChildren(actionNode);
    }
    
    @Test
    public void withoutPatternsTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
        }};
        actionNode.setNumber("21");
        actionNode.setGenus(Genus.NEUTER);
        actionNode.setPauseBetweenWords(-150);
        assertTrue(actionNode.start());
        SayNumberAction action = (SayNumberAction) actionNode.createAction();
        assertNotNull(action);
        assertEquals(action.getPauseBetweenWordsGroups(), actionNode.getPauseBetweenNumbers().longValue());
        assertEquals(action.getPauseBetweenWords(), actionNode.getPauseBetweenWords().longValue());
        List sentences = action.formWords(conv);
        assertNotNull(sentences);
        assertEquals(1, sentences.size());
        assertTrue(sentences.get(0) instanceof List);
        assertArrayEquals(new String[]{"20","1''"}, ((List)sentences.get(0)).toArray());
    }
    
    @Test
    public void zeroTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        actionNode.setNumber("0");
        actionNode.setEnableZero(Boolean.TRUE);
        actionNode.setGenus(Genus.MALE);
        assertTrue(actionNode.start());
        SayNumberAction action = (SayNumberAction) actionNode.createAction();
        assertNotNull(action);
        List sentences = action.formWords(conv);
        assertNotNull(sentences);
        assertEquals(1, sentences.size());
        assertTrue(sentences.get(0) instanceof List);
        assertArrayEquals(new String[]{"0"}, ((List)sentences.get(0)).toArray());
    }
    
    @Test
    public void withPatternsTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings) 
        throws Exception 
    {
        actionNode.setPauseBetweenWords(-130);
        actionNode.setNumber("9128672947");
        assertTrue(actionNode.start());
        RegexpPattern pattern = new RegexpPattern();
        pattern.setName("pattern1");
        actionNode.addAndSaveChildren(pattern);
        pattern.setPattern("(\\d\\d\\d)(\\d\\d\\d)(\\d\\d)(\\d\\d)");
        assertTrue(pattern.start());
        SayNumberAction action = (SayNumberAction) actionNode.createAction();
        assertNotNull(action);
        List sentences = action.formWords(conv);
        assertNotNull(sentences);
        assertEquals(4, sentences.size());
        assertTrue(sentences.get(0) instanceof List);
        assertArrayEquals(new String[]{"900", "12"}, ((List)sentences.get(0)).toArray());
        assertArrayEquals(new String[]{"800", "60", "7"}, ((List)sentences.get(1)).toArray());
        assertArrayEquals(new String[]{"20", "9"}, ((List)sentences.get(2)).toArray());
        assertArrayEquals(new String[]{"40", "7"}, ((List)sentences.get(3)).toArray());
    }
}
