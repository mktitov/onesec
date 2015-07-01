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
import java.util.List;
import org.junit.Test;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.Sentence;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyWordSubactionTest extends SentenceSubactionTestHelper {
    
    @Test
    public void oneWordTest() throws Exception {
        AudioFile w1 = createFile(testsNode, "w1");
        params.put(SayAnyWordSubaction.SENTENCE_PAUSE_PARAM, "1");
        params.put(SayAnyWordSubaction.WORD_PAUSE_PARAM, "2");
        SayAnyWordSubaction action = new SayAnyWordSubaction("w1", params, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);
        SubactionSentencesResult res = action.getResult();
        assertNotNull(res);
        assertEquals(1l, res.getPauseBetweenSentences());
        List<Sentence> sentences = res.getSentences();
        assertNotNull(sentences);
        assertEquals(1, sentences.size());
        Sentence sentence = sentences.get(0);
        assertNotNull(sentence);
        assertEquals(2l, sentence.getPauseBetweenWords());
        assertArrayEquals(new AudioFile[]{w1}, sentence.getWords().toArray());
    }
    
    @Test
    public void twoWordsTest() throws Exception {
        AudioFile w1 = createFile(testsNode, "w1");
        AudioFile w2 = createFile(testsNode, "w2");
        SayAnyWordSubaction action = new SayAnyWordSubaction("w1,w2", params, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);
        SubactionSentencesResult res = action.getResult();
        assertNotNull(res);
        List<Sentence> sentences = res.getSentences();
        assertEquals(1, sentences.size());
        assertArrayEquals(new AudioFile[]{w1, w2}, sentences.get(0).getWords().toArray());
    }
}
