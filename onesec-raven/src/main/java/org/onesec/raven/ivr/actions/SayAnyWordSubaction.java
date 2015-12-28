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
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.SentenceResult;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.onesec.raven.ivr.impl.SentenceImpl;
import org.onesec.raven.ivr.impl.SubactionSentencesResultImpl;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyWordSubaction extends AbstractSentenceSubaction {
    private final SubactionSentencesResult result;

    public SayAnyWordSubaction(String value, Map<String, String> params, Node actionNode, 
            List<Node> defaultWordsNodes, ResourceManager resourceManager) 
        throws SayAnyActionException 
    {
        super(params, actionNode, defaultWordsNodes, resourceManager);
        SentenceResult sentence = new SentenceImpl(pauseBetweenWords, parseWords(value));
        result = new SubactionSentencesResultImpl(pauseBetweenSentences, Arrays.asList(sentence));
    }

    public SubactionSentencesResult getResult() {
        return result;
    }

    private List<AudioFile> parseWords(String value) throws SayAnyActionException {
        List<AudioFile> frags = new LinkedList<AudioFile>();
        for (String word: value.split(","))
            frags.add(getAudioNode(word));
        return frags;
    }
}
