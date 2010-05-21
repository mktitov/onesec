/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.actions;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileInputStreamSource;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SayAmountAction extends AsyncAction
{
    public final static String NAME = "Say amount action";

    private final Node numbersNode;
    private final Double amount;
    private final long pauseBetweenWords;

    public SayAmountAction(Node numbersNode, Double amount, long pauseBetweenWords)
    {
        super(NAME);
        this.numbersNode = numbersNode;
        this.amount = amount;
        this.pauseBetweenWords = pauseBetweenWords;
    }

    public boolean isFlowControlAction() {
        return false;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        if (amount==null || amount==0.)
            return;

        List<String> words = NumberToDigitConverter.getCurrencyDigits(amount);
        if (words==null || words.isEmpty())
            return;

        AudioFileInputStreamSource[] audioSources = new AudioFileInputStreamSource[words.size()];
        int i=0;
        for (String number: words)
        {
            Node node = numbersNode.getChildren(number);
            if (!(node instanceof AudioFileNode))
            {
                if (conversation.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                    conversation.getOwner().getLogger().error(logMess(
                            "Can not say the amount because of not found " +
                            "the AudioFileNode (%s) node in the (%s) numbers node"
                            , number, numbersNode.getPath()));
                return;
            }
            audioSources[i] = new AudioFileInputStreamSource((AudioFileNode)node, conversation.getOwner());
            ++i;
        }

        for (AudioFileInputStreamSource audioSource: audioSources)
        {
            if (!playAudio(audioSource, conversation.getAudioStream()))
                return;
            TimeUnit.MILLISECONDS.sleep(pauseBetweenWords);
        }
    }

    private boolean playAudio(AudioFileInputStreamSource audioSource, AudioStream stream)
            throws InterruptedException
    {
        stream.addSource(audioSource);
        while (!hasCancelRequest() && stream.isPlaying())
            Thread.sleep(10);
        return !hasCancelRequest();
    }
}
