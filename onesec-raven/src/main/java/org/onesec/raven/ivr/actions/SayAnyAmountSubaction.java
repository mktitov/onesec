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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.Sentence;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.onesec.raven.ivr.impl.SentenceImpl;
import org.onesec.raven.ivr.impl.SubactionSentencesResultImpl;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyAmountSubaction extends AbstractSentenceSubaction {
    private final SubactionSentencesResult result;

    public SayAnyAmountSubaction(String value, Map<String, String> params, 
            Node actionNode, List<Node> defaultWordsNodes, ResourceManager resourceManager) 
        throws SayAnyActionException 
    {
        super(params, actionNode, defaultWordsNodes, resourceManager);
        Sentence sentence = new SentenceImpl(pauseBetweenWords, parseAmount(value));
        result = new SubactionSentencesResultImpl(pauseBetweenSentences, Arrays.asList(sentence));
    }

    public SubactionSentencesResult getResult() {
        return result;
    }

    private List<AudioFile> parseAmount(String value) throws SayAnyActionException {
        List<AudioFile> files = new LinkedList<AudioFile>();
        for (String word: NumberToDigitConverter.getCurrencyDigits(Double.parseDouble(value)))
            files.add(getAudioNode(word));
        return files;
    }
}
