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
import java.util.List;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractSayNumberAction;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SayNumberInQueueAction extends AbstractSayNumberAction
{
    private final static String NAME = "Say number in queue action";

    private final AudioFileNode preambleAudio;

    public SayNumberInQueueAction(Node numbersNode, long pauseBetweenWords, AudioFileNode preambleAudio)
    {
        super(NAME, numbersNode, pauseBetweenWords);
        this.preambleAudio = preambleAudio;
    }

    @Override
    protected List<String> formWords(IvrEndpointConversation conversation)
    {
        QueuedCallStatus callStatus = (QueuedCallStatus) conversation.getConversationScenarioState()
                .getBindings().get(QueueCallAction.QUEUED_CALL_STATUS_BINDING);
        if (callStatus==null || !callStatus.isQueueing() || callStatus.getSerialNumber()<1)
            return null;
        List words = new ArrayList();
        words.add(preambleAudio);
        words.addAll(NumberToDigitConverter.getDigits(callStatus.getSerialNumber()));
        return words;
    }
}
