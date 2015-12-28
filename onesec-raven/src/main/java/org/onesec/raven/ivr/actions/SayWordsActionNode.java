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

import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class SayWordsActionNode extends AbstractActionNode {
    
    public final static String WORDS_NODE_ATTR = "wordsNode";
    
    @Service
    private static ResourceManager resourceManager;
    
    @NotNull @Parameter
    private String words;
    
    @NotNull 
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private Node wordsNode;
    
    @NotNull @Parameter(defaultValue="10") 
    private Integer pauseBetweenWords;
    
    @Override
    protected Action doCreateAction() {
        return new SayWordsAction(this, bindingSupport, 
                NodeUtils.getAttrValuesByPrefixAndType(this, "wordsNode", Node.class), 
                pauseBetweenWords, resourceManager);
    }
    
    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public Node getWordsNode() {
        return wordsNode;
    }

    public void setWordsNode(Node wordsNode) {
        this.wordsNode = wordsNode;
    }

    public Integer getPauseBetweenWords() {
        return pauseBetweenWords;
    }

    public void setPauseBetweenWords(Integer pauseBetweenWords) {
        this.pauseBetweenWords = pauseBetweenWords;
    }
}
