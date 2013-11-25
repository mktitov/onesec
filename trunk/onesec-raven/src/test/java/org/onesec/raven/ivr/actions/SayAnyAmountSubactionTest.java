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
public class SayAnyAmountSubactionTest extends SentenceSubactionTestHelper {
    
    @Before
    public void prepare() throws DataFileException {
        createFile(testsNode, "20");
        createFile(testsNode, "3");
        createFile(testsNode, "рубля");
        createFile(testsNode, "30");
        createFile(testsNode, "копеек");
    }
    
    @Test
    public void test() throws SayAnyActionException {
        SayAnyAmountSubaction action = new SayAnyAmountSubaction("23.30", params, testsNode, 
                Arrays.asList((Node)testsNode), resourceManager);
        SubactionSentencesResult res = action.getResult();
        checkSentences(arrOfArr(arr("20", "3", "рубля", "30", "копеек")), res.getSentences());
    }
}