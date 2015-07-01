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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.onesec.raven.impl.Genus;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayNumberAction extends AbstractSayWordsAction {
    public final static String NAME = "Say number action";
    private final SayNumberActionNode actionNode;
    private final Genus genus;
    private final boolean enableZero;

    public SayNumberAction(SayNumberActionNode owner, Collection<Node> numbersNode, Genus genus, long pauseBetweenWords, 
        long pauseBetweenNumbers, boolean enableZero, ResourceManager resourceManager) 
    {
        super(NAME, numbersNode, pauseBetweenWords, pauseBetweenNumbers, resourceManager);
        this.actionNode = owner;
        this.genus = genus;
        this.enableZero = enableZero;
    }

    @Override
    protected List<List> formWords(IvrEndpointConversation conversation) {
        actionNode.getBindingSupport().putAll(conversation.getConversationScenarioState().getBindings());
        try {
            List<List> res = new LinkedList<List>();
            for (Long number: actionNode.getNumbersSequence())
                res.add(NumberToDigitConverter.getDigits(number, genus, enableZero));
            return res;
        } finally {
            actionNode.getBindingSupport().reset();
        }
    }
}
