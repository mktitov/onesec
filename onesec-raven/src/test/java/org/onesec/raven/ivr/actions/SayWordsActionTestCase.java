/*
 * Copyright 2015 Mikhail Titov.
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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioFile;

/**
 *
 * @author Mikhail Titov
 */
public class SayWordsActionTestCase extends OnesecRavenTestCase {
    
    protected void checkSentence(Object sentenceObj, long pauseAfterSentence, long pauseBetweenWords, String[] words) {
        assertTrue(sentenceObj instanceof AbstractSayWordsAction.Sentence);
        AbstractSayWordsAction.Sentence sentence = (AbstractSayWordsAction.Sentence) sentenceObj;
        assertEquals(pauseAfterSentence, sentence.getPauseAfterSentence());
        assertEquals(pauseBetweenWords, sentence.getPauseBetweenWords());
        List<AudioFile> audioFiles = sentence.getWords();
        assertNotNull(audioFiles);
        String[] names = new String[audioFiles.size()];
        for (int i=0; i<audioFiles.size(); ++i)
            names[i] = audioFiles.get(i).getParent().getName();
        assertArrayEquals(words, names);
    }
}
