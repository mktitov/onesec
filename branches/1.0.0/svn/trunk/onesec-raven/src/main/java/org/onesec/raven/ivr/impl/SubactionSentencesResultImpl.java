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

package org.onesec.raven.ivr.impl;

import java.util.List;
import org.onesec.raven.ivr.Sentence;
import org.onesec.raven.ivr.SubactionSentencesResult;

/**
 *
 * @author Mikhail Titov
 */
public class SubactionSentencesResultImpl implements SubactionSentencesResult {
    private final long pauseBetweenSentences;
    private final List<Sentence> sentences;

    public SubactionSentencesResultImpl(long pauseBetweenSentences, List<Sentence> sentences) {
        this.pauseBetweenSentences = pauseBetweenSentences;
        this.sentences = sentences;
    }

    public long getPauseBetweenSentences() {
        return pauseBetweenSentences;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }
}
