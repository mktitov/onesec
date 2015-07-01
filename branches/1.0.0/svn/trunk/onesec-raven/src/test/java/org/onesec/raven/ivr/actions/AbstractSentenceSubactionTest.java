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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractSentenceSubactionTest extends SentenceSubactionTestHelper {
    
    @Test
    public void noPausesInParamsTest() throws SayAnyActionException {
        TestSentenceSubaction action = new TestSentenceSubaction(Collections.EMPTY_MAP, testsNode, null, resourceManager);
        assertEquals(0l, action.pauseBetweenSentences);
        assertEquals(0l, action.pauseBetweenWords);
    }
    
    @Test
    public void pausesInParamsTest() throws SayAnyActionException {
        params.put(AbstractSentenceSubaction.SENTENCE_PAUSE_PARAM, "1");
        params.put(AbstractSentenceSubaction.WORD_PAUSE_PARAM, "2s");
        TestSentenceSubaction action = new TestSentenceSubaction(params, testsNode, null, resourceManager);
        assertEquals(1l, action.pauseBetweenSentences);
        assertEquals(2000l, action.pauseBetweenWords);
    }
    
    //Node not exists in defaultWordsNodes
    @Test(expected = SayAnyActionException.class)
    public void defaultWordsNodesTest() throws SayAnyActionException {
        TestSentenceSubaction action = new TestSentenceSubaction(Collections.EMPTY_MAP, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);
        action.getAudioNode("unknown");        
    }
    
    //Node exists in defaultWordsNodes but not an audio file
    @Test(expected = SayAnyActionException.class)
    public void defaultWordsNodesTest2() throws SayAnyActionException {
        BaseNode node = new BaseNode("node");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
        TestSentenceSubaction action = new TestSentenceSubaction(Collections.EMPTY_MAP, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);        
        action.getAudioNode("node");        
    }
    
    //success test
    @Test()
    public void defaultWordsNodesTest3() throws Exception {
        AudioFile audio1 = createFile(testsNode, "audio1");
        TestSentenceSubaction action = new TestSentenceSubaction(Collections.EMPTY_MAP, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);        
        assertSame(audio1, action.getAudioNode("audio1"));
    }
    
    //right order test
    @Test()
    public void defaultWordsNodesTest4() throws Exception {
        Node container1 = createContainer(testsNode, "c1");
        Node container2 = createContainer(testsNode, "c2");        
        AudioFile audio1_1 = createFile(container1, "audio1");
        AudioFile audio1_2 = createFile(container2, "audio1");
        AudioFile audio2_2 = createFile(container2, "audio2");
        TestSentenceSubaction action = new TestSentenceSubaction(Collections.EMPTY_MAP, testsNode, 
                Arrays.asList(container1, container2), resourceManager);        
        assertSame(audio1_1, action.getAudioNode("audio1"));
        assertSame(audio2_2, action.getAudioNode("audio2"));
    }
    
    @Test
    public void nodesParameterTest() throws Exception {
        Node c1 = createContainer(testsNode, "c1");
        Node c2 = createContainer(testsNode, "c2");        
        Node c3 = createContainer(testsNode, "c3");        
        AudioFile audio1_1 = createFile(c1, "audio1");
        AudioFile audio1_2 = createFile(c2, "audio1");
        AudioFile audio2_2 = createFile(c2, "audio2");
        AudioFile audio1_3 = createFile(c3, "audio1");
        AudioFile audio3_3 = createFile(c3, "audio3");
        params.put(AbstractSentenceSubaction.NODES_PARAM, "ac1,ac2");
        createAttr(testsNode, "ac1", c1);
        createAttr(testsNode, "ac2", c2);
        TestSentenceSubaction action = new TestSentenceSubaction(params, testsNode, 
                Arrays.asList(c3), resourceManager);        
        assertSame(audio1_1, action.getAudioNode("audio1"));
        assertSame(audio2_2, action.getAudioNode("audio2"));
        assertSame(audio3_3, action.getAudioNode("audio3"));
    }
    
    private class TestSentenceSubaction extends AbstractSentenceSubaction {

        public TestSentenceSubaction(Map<String, String> params, Node actionNode, List<Node> defaultWordsNodes, 
                ResourceManager resourceManager) 
            throws SayAnyActionException 
        {
            super(params, actionNode, defaultWordsNodes, resourceManager);
        }

        public SubactionSentencesResult getResult() {
            throw new UnsupportedOperationException("Not supported yet."); 
        }
    }
}
