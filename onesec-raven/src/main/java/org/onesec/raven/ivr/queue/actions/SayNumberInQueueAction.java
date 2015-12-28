/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.queue.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.script.Bindings;
import org.onesec.raven.impl.Genus;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractSayWordsAction;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.RavenUtils;
import org.raven.expr.BindingSupport;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayNumberInQueueAction extends AbstractSayWordsAction
{
    public final static String LAST_SAYED_NUMBER = "lastSayedNumber";
    private final static String NAME = "Say number in queue";

    private final AudioFileNode preambleAudio;
    private final SayNumberInQueueActionNode owner;
    private final BindingSupport bindingSupport;

    public SayNumberInQueueAction(SayNumberInQueueActionNode owner, BindingSupport bindingSupport
            , Node numbersNode, long pauseBetweenWords, AudioFileNode preambleAudio
            , ResourceManager resourceManager)
    {
        super(NAME, Arrays.asList(numbersNode), pauseBetweenWords, 0, resourceManager);
        this.preambleAudio = preambleAudio;
        this.owner = owner;
        this.bindingSupport = bindingSupport;
    }

    @Override
    protected List formWords(IvrEndpointConversation conversation)
    {
        Bindings bindings = conversation.getConversationScenarioState().getBindings();
        QueuedCallStatus callStatus = (QueuedCallStatus) bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING);
        String key = RavenUtils.generateKey(LAST_SAYED_NUMBER, owner);
        Integer lastSayedNumber = (Integer) bindings.get(key);
        if (   callStatus==null || !callStatus.isQueueing() || callStatus.getSerialNumber()<1
            || (lastSayedNumber!=null && callStatus.getSerialNumber()>=lastSayedNumber))
        {
            return null;
        }
        int num = callStatus.getSerialNumber();
        bindingSupport.putAll(bindings);
        try {
            Boolean accept = owner.getAcceptSayNumber();
            if (accept==null || !accept)
                return null;
            List words = new ArrayList();
            words.add(preambleAudio);
            words.addAll(NumberToDigitConverter.getDigits(num, Genus.MALE));
            bindings.put(key, num);
            return Arrays.asList(words);
        } finally {
            bindingSupport.reset();
        }
    }
}