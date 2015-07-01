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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.SayAnySubaction;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.raven.RavenUtils;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSentenceSubaction implements SayAnySubaction<SubactionSentencesResult> {
    public final static String NODES_PARAM = "n";
    public final static String SENTENCE_PAUSE_PARAM = "sp";
    public final static String WORD_PAUSE_PARAM = "wp";   
    
    protected final ResourceManager resourceManager;
    protected final Map<String, String> params;
    protected final Node actionNode;
    
    protected final long pauseBetweenSentences;
    protected final long pauseBetweenWords;
    protected final List<Node> wordsNodes;

    public AbstractSentenceSubaction(Map<String, String> params, Node actionNode, List<Node> defaultWordsNodes, 
            ResourceManager resourceManager) throws SayAnyActionException 
    {
        this.resourceManager = resourceManager;
        this.params = params;
        this.actionNode = actionNode;
        this.wordsNodes = new LinkedList<Node>();
        if (params.containsKey(NODES_PARAM))
            parseNodesParam(params.get(NODES_PARAM));
        addDefaultWordsNodes(defaultWordsNodes);
        pauseBetweenSentences = parsePause(SENTENCE_PAUSE_PARAM);
        pauseBetweenWords = parsePause(WORD_PAUSE_PARAM);
    }
    
    protected AudioFile getAudioNode(String word) throws SayAnyActionException {
        for (Node words: wordsNodes) {
            Node res = resourceManager.getResource(words, word, null);
            if (res!=null && res instanceof AudioFile) 
                return (AudioFile) res;
        }
        throw new SayAnyActionException("Not found audio node for fragment with name (%s)", word);
    }

    private void parseNodesParam(String nodesStr) throws SayAnyActionException {
        for (String node: RavenUtils.split(nodesStr, ",")) {
            NodeAttribute attr = actionNode.getAttr(node);
            if (attr!=null && Node.class.isAssignableFrom(attr.getType()) && attr.getRealValue()!=null)
                wordsNodes.add((Node)attr.getRealValue());
            else 
                throw new SayAnyActionException(
                        "Not found attribute (%s) in (%s) or attribute type isn't Node type",
                        node, actionNode.getPath());                                
        }
    }

    private long parsePause(String paramName) throws SayAnyActionException {
        return !params.containsKey(paramName)? 
                0l : new SayAnyPauseSubaction(params.get(paramName)).getResult().getPause();
    }

    private void addDefaultWordsNodes(final List<Node> defaultWordsNodes) {
        if (defaultWordsNodes!=null)
            for (Node node: defaultWordsNodes)
                if (node!=null)
                    wordsNodes.add(node);
    }
}
