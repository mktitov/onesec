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

package org.onesec.raven.ivr.actions;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileInputStreamSource;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSayNumberAction extends AsyncAction
{
    private final Node numbersNode;
    private final long pauseBetweenWords;

    public AbstractSayNumberAction(String actionName, Node numbersNode, long pauseBetweenWords)
    {
        super(actionName);
        this.numbersNode = numbersNode;
        this.pauseBetweenWords = pauseBetweenWords;
    }

    public boolean isFlowControlAction() {
        return false;
    }

    protected abstract List formWords(IvrEndpointConversation conversation);

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        List words = formWords(conversation);

        if (words==null || words.isEmpty())
            return;
        
        int i=0;
        AudioFileNode[] audioSources = new AudioFileNode[words.size()];
        for (Object word: words) {
            AudioFileNode audio = null;
            if (word instanceof AudioFileNode)
                audio = (AudioFileNode) word;
            else if (word instanceof String) {
                Node child = numbersNode.getChildren((String)word);
                if (!(child instanceof AudioFileNode)) {
                    if (conversation.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                        conversation.getOwner().getLogger().error(logMess(
                                "Can not say the amount because of not found " +
                                "the AudioFileNode (%s) node in the (%s) numbers node"
                                , word, numbersNode.getPath()));
                    return;
                } else
                    audio = (AudioFileNode) child;
            }            
            audioSources[i++] = audio;
        }

        for (AudioFileNode audioFile: audioSources) {
            IvrUtils.playAudioInAction(this, conversation, audioFile);
            if (hasCancelRequest())
                return;
            TimeUnit.MILLISECONDS.sleep(pauseBetweenWords);
        }
    }
}
