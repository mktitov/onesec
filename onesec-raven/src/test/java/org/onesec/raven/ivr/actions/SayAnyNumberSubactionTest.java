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
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyNumberSubactionTest extends SentenceSubactionTestHelper {
    
    @Before
    public void prepare() throws DataFileException {
        createFile(testsNode, "20");
        createFile(testsNode, "2");
        createFile(testsNode, "3");
        createFile(testsNode, "1'");
        createFile(testsNode, "1");
        createFile(testsNode, "0");
    }
    
    @Test
    public void withoutPatterns() throws Exception {
        params.put(SayAnyNumberSubaction.SENTENCE_PAUSE_PARAM, "1");
        params.put(SayAnyNumberSubaction.WORD_PAUSE_PARAM, "2");
        SayAnyNumberSubaction action = createSayNumberAction("23");
        SubactionSentencesResult res = action.getResult();
        assertEquals(1l, res.getPauseBetweenSentences());
        checkSentences(arrOfArr(arr("20","3")), res.getSentences());
    }
    
    @Test
    public void withPattenrsTest() throws Exception {
        params.put(SayAnyNumberSubaction.REGEXP_PARAM, "(\\d)(\\d),(\\d\\d)(\\d)");
        
        SayAnyNumberSubaction action = createSayNumberAction("23");
        SubactionSentencesResult res = action.getResult();
        checkSentences(arrOfArr(arr("2"),arr("3")), res.getSentences());
        
        action = createSayNumberAction("233");
        res = action.getResult();
        checkSentences(arrOfArr(arr("20","3"), arr("3")), res.getSentences());
    }
    
    @Test
    public void genusTest() throws Exception {
        params.put(SayAnyNumberSubaction.GENUS_PARAM, "female");        
        SayAnyNumberSubaction action = createSayNumberAction("1");
        SubactionSentencesResult res = action.getResult();
        checkSentences(arrOfArr(arr("1'")), res.getSentences());
        
        params.put(SayAnyNumberSubaction.GENUS_PARAM, "male");        
        action = createSayNumberAction("1");
        res = action.getResult();
        checkSentences(arrOfArr(arr("1")), res.getSentences());
    }
    
    @Test
    public void enabledZeroTest() throws Exception {
        SayAnyNumberSubaction action = createSayNumberAction("01");
        SubactionSentencesResult res = action.getResult();
        checkSentences(arrOfArr(arr("1")), res.getSentences());
        
        params.put(SayAnyNumberSubaction.ZERO_PARAM, "yes");        
        action = createSayNumberAction("01");
        res = action.getResult();
        checkSentences(arrOfArr(arr("0"), arr("1")), res.getSentences());
    }

//    private void checkSentences(String[][] words, List<Sentence> sentences) {
//        assertEquals(words.length, sentences.size());
//        for (int i=0; i<words.length; ++i) {
//            List<String> names = new LinkedList<String>();
//            for (AudioFile file: sentences.get(i).getWords())
//                names.add(file.getName());
//            assertArrayEquals(words[i], names.toArray());
//        }
//    }
//    
//    private String[] arr(String... args) {
//        return args;
//    }
//    
//    private String[][] arrOfArr(String[]... args) {
//        return args;
//    }

    private SayAnyNumberSubaction createSayNumberAction(String number) throws SayAnyActionException {
        SayAnyNumberSubaction action = new SayAnyNumberSubaction(number, params, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);
        return action;
    }
}