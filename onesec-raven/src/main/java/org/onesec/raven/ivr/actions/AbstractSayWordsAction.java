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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.PlayAudioDP;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.dp.DataProcessorFacade;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSayWordsAction extends AbstractAction {
    private static final String PLAYER_DP = "Player";
    private final static String PLAY_NEXT_GROUP = "PLAY_NEXT_GROUP";
    
    private final Collection<Node> wordsNodes;
    private final long pauseBetweenWords;
    private final long pauseBetweenWordsGroups;
    private final ResourceManager resourceManager;
    
    private Iterator<Object> groupsOfWords;
    private DataProcessorFacade player;
    private long nextSentencePause = -1;

    public AbstractSayWordsAction(String actionName, Collection<Node> wordsNodes, long pauseBetweenWords, 
        long pauseBetweenWordsGroups, ResourceManager resourceManager)
    {
        super(actionName);
        this.wordsNodes = wordsNodes;
        this.pauseBetweenWords = pauseBetweenWords;
        this.pauseBetweenWordsGroups = pauseBetweenWordsGroups;
        this.resourceManager = resourceManager;
    }

    public long getPauseBetweenWords() {
        return pauseBetweenWords;
    }

    public long getPauseBetweenWordsGroups() {
        return pauseBetweenWordsGroups;
    }

    public Collection<Node> getWordsNodes() {
        return wordsNodes;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    protected abstract List<Object> formWords(IvrEndpointConversation conversation) throws Exception;

    @Override
    public Object processData(Object message) throws Exception {
        if (message==PLAY_NEXT_GROUP) {
            playWords();
            return VOID;
        } else if (message==PlayAudioDP.PLAYED) {
            final long pause = nextSentencePause>=0? nextSentencePause : pauseBetweenWordsGroups;
            nextSentencePause = -1;
            if (pause>0 && groupsOfWords.hasNext())
                getFacade().sendDelayed(pauseBetweenWordsGroups, PLAY_NEXT_GROUP);
            else
                playWords();
            return VOID;
        } else
            return super.processData(message);
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        List<Object> groups = formWords(message.getConversation());
        if (groups==null || groups.isEmpty()) {
            if (getLogger().isDebugEnabled()) 
                getLogger().debug("No word(s) to say");            
            return ACTION_EXECUTED_then_EXECUTE_NEXT;
        } else {
            player = getContext().addChild(getContext().createChild(PLAYER_DP, new PlayAudioDP(message.getConversation().getAudioStream())));
            groupsOfWords = groups.iterator();
            getFacade().send(PLAY_NEXT_GROUP);
            return null;
        }        
    }
    
    private void playWords() {
        if (!groupsOfWords.hasNext()) 
            sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
        else {
            final Object audioObject = groupsOfWords.next();
            if      (audioObject==null)
                getFacade().send(PLAY_NEXT_GROUP);
            else if (audioObject instanceof List) 
                playListOfWords((List)audioObject);
            else if (audioObject instanceof Pause) 
                getFacade().sendDelayed(((Pause)audioObject).pause, PLAY_NEXT_GROUP);
            else if (audioObject instanceof Sentence) 
                playSentence(audioObject);            
        }
    }

    private void playSentence(final Object audioObject) {
        Sentence sentence = (Sentence) audioObject;
        nextSentencePause = sentence.pauseAfterSentence;
        playAudioFiles(sentence.words, sentence.pauseBetweenWords);
    }

    private void playListOfWords(final List audioObject) {
        playAudioFiles(getAudioSources(audioObject), pauseBetweenWords);
    }
    
    private void playAudioFiles(final List<AudioFile> audioFiles, final long pause) {
        if (audioFiles.isEmpty())
            getFacade().send(PLAY_NEXT_GROUP);
        else
            getFacade().sendTo(player, new PlayAudioDP.PlayAudioFiles(audioFiles, pause));
    }
    
    private List<AudioFile> getAudioSources(List words) {
        if (words==null || words.isEmpty())
            return Collections.EMPTY_LIST;
        List<AudioFile> audioSources = new ArrayList<>(words.size());
        for (Object word: words) {
            AudioFileNode audio = null;
            if (word instanceof AudioFileNode)
                audio = (AudioFileNode) word;
            else if (word instanceof String) {
                Node child = getAudioNode((String)word);
                if (!(child instanceof AudioFileNode)) {
                    if (getLogger().isErrorEnabled())
                        getLogger().error(String.format(
                                "Can not say the word because of not found " +
                                "the AudioFileNode (%s) node in nodes: %s"
                                , word, wordsNodes));
                    return Collections.EMPTY_LIST;
                } else
                    audio = (AudioFileNode) child;
            }            
            audioSources.add(audio);
        }
        return audioSources;
    }
    
    private Node getAudioNode(String word) {
        for (Node words: wordsNodes) {
            Node res = resourceManager.getResource(words, word, null);
            if (res!=null) return res;
        }
        return null;
    }
    
    public static class Pause {
        private final long pause;

        public Pause(long pause) {
            this.pause = pause;
        }

        public long getPause() {
            return pause;
        }
    }
    
    public static class Sentence {
        private final long pauseAfterSentence;
        private final long pauseBetweenWords;
        private final List<AudioFile> words;

        public Sentence(long pauseAfterSentence, long pauseBetweenWords, List<AudioFile> words) {
            this.pauseAfterSentence = pauseAfterSentence;
            this.pauseBetweenWords = pauseBetweenWords;
            this.words = words;
        }

        public long getPauseAfterSentence() {
            return pauseAfterSentence;
        }

        public long getPauseBetweenWords() {
            return pauseBetweenWords;
        }

        public List<AudioFile> getWords() {
            return words;
        }
    }
}
