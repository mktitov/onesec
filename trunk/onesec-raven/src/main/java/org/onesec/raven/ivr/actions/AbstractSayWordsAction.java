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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSayWordsAction extends AsyncAction
{
    private final Collection<Node> wordsNodes;
    private final long pauseBetweenWords;
    private final long pauseBetweenWordsGroups;
    private final ResourceManager resourceManager;

    public AbstractSayWordsAction(String actionName, Collection<Node> wordsNodes, long pauseBetweenWords, 
        long pauseBetweenWordsGroups, ResourceManager resourceManager)
    {
        super(actionName);
        this.wordsNodes = wordsNodes;
        this.pauseBetweenWords = pauseBetweenWords;
        this.pauseBetweenWordsGroups = pauseBetweenWordsGroups;
        this.resourceManager = resourceManager;
    }

    public boolean isFlowControlAction() {
        return false;
    }

    protected abstract List<List> formWords(IvrEndpointConversation conversation);

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        List<List> numbers = formWords(conversation);
        if (numbers==null || numbers.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("No word(s) to say");            
        } else
            for (List number: numbers) {
                sayWords(number);
                TimeUnit.MILLISECONDS.sleep(pauseBetweenWordsGroups);
            }
    }
    
    private void sayWords(List words) throws Exception {
        if (words==null || words.isEmpty())
            return;
        int i=0;
        AudioFile[] audioSources = new AudioFileNode[words.size()];
        for (Object word: words) {
            AudioFileNode audio = null;
            if (word instanceof AudioFileNode)
                audio = (AudioFileNode) word;
            else if (word instanceof String) {
                Node child = getAudioNode((String)word);
                if (!(child instanceof AudioFileNode)) {
                    if (logger.isErrorEnabled())
                        logger.error(String.format(
                                "Can not say the word because of not found " +
                                "the AudioFileNode (%s) node in nodes: %s"
                                , word, wordsNodes));
                    return;
                } else
                    audio = (AudioFileNode) child;
            }            
            audioSources[i++] = audio;
        }

        if (pauseBetweenWords>0) {
            for (AudioFile audioFile: audioSources) {
                IvrUtils.playAudioInAction(this, conversation, audioFile);
                if (hasCancelRequest())
                    return;
                TimeUnit.MILLISECONDS.sleep(pauseBetweenWords);
            }
        } else {
            conversation.getAudioStream().playContinuously(Arrays.asList(audioSources), Math.abs(pauseBetweenWords));
            while (conversation.getAudioStream().isPlaying())
                Thread.sleep(5);
        }
    }
    
    private Node getAudioNode(String word) {
        for (Node words: wordsNodes) {
            Node res = resourceManager.getResource(words, word, null);
            if (res!=null) return res;
        }
        return null;
    }
}
