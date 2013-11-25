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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.onesec.raven.impl.Genus;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.Sentence;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.onesec.raven.ivr.impl.SubactionSentencesResultImpl;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyNumberSubaction extends AbstractSentenceSubaction {
    public static final String ZERO_PARAM = "z";
    public static final String REGEXP_PARAM = "p";
    
    private final boolean enableZero;
    private final SubactionSentencesResult result;
    private final Genus genus;

    public SayAnyNumberSubaction(String value, Map<String, String> params, Node actionNode, 
            List<Node> defaultWordsNodes, ResourceManager resourceManager) 
        throws SayAnyActionException 
    {
        super(params, actionNode, defaultWordsNodes, resourceManager);
        enableZero = parseEnableZero();
        genus = parseGenus();
        result = parseNumber(value);
    }

    public SubactionSentencesResult getResult() {
        return result;
    }
    
    private SubactionSentencesResult parseNumber(String value) throws SayAnyActionException {
        List<Sentence> sentences = new LinkedList<Sentence>();
        for (Long number: getNumbersSequence(value))
            sentences.add(createSentence(NumberToDigitConverter.getDigits(number, genus, enableZero)));
        return new SubactionSentencesResultImpl(pauseBetweenSentences, sentences);        
    }
    
    private Sentence createSentence(List<String> digits) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    private Collection<Long> getNumbersSequence(String value) throws SayAnyActionException {
        List<String> strNumbers = divideOnGroups(value);
        if (strNumbers.isEmpty())
            return Collections.EMPTY_LIST;
        List<Long> numbers = new ArrayList<Long>(strNumbers.size());
        for (String strNumber: strNumbers) 
            try {
                int i=0;
                if (enableZero)
                    while (i<strNumber.length() && strNumber.charAt(i)=='0') {
                        numbers.add(0l);
                        ++i;
                    }
                if (i!=strNumber.length())
                    numbers.add(Long.parseLong(strNumber));
            } catch (NumberFormatException e) {
                throw new SayAnyActionException("Can't convert (%s) to integer", strNumber);
            }
        return numbers;
    }
    
    private List<String> divideOnGroups(String value) {
        if (!params.containsKey(REGEXP_PARAM))
            return Arrays.asList(value);
        else {
            for (String pattern: params.get("r").split(",")) {
                Matcher matcher = Pattern.compile(pattern).matcher(value);
                if (matcher.matches()) {
                    if (matcher.groupCount()<1)
                        return Collections.EMPTY_LIST;
                    ArrayList<String> groups = new ArrayList<String>(matcher.groupCount());
                    for (int i=1; i<=matcher.groupCount(); ++i)
                        groups.add(matcher.group(i));
                    return groups;
                }
            }
            return Collections.EMPTY_LIST;
        }
    }

    private boolean parseEnableZero() {
        return !params.containsKey(ZERO_PARAM)?
                false : ObjectUtils.in(params.get(ZERO_PARAM).toLowerCase(), "y", "yes", "true", "t");
    }

    private Genus parseGenus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
