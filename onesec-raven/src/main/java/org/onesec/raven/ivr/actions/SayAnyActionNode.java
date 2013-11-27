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

import org.onesec.raven.Constants;
import org.onesec.raven.impl.Genus;
import org.onesec.raven.ivr.IvrAction;
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
public class SayAnyActionNode extends AbstractActionNode {
    public static final String WORDS_NODE_ATTR = "wordsNode";
    public static final String AMOUNT_NUMBERS_NODE_ATTR = "amountNumbersNode";
    public static final String NUMBERS_NODE_ATTR = "numbersNode";
    
    @Service
    private static ResourceManager resourceManager;
    
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private Node wordsNode;
    
    @NotNull @Parameter(
            valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE, 
            defaultValue=Constants.NUMBERS_MALE_RESOURCE)
    private Node numbersNode;
    
    @NotNull @Parameter(
            valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE, 
            defaultValue=Constants.NUMBERS_MALE_RESOURCE)
    private Node amountNumbersNode;
    
    @NotNull @Parameter(defaultValue = "100")
    private Long wordsSentencePause;
    
    @NotNull @Parameter(defaultValue = "0")
    private Long wordsWordPause;
    
    @NotNull @Parameter(defaultValue="MALE")
    private Genus numbersGenus;
    
    @NotNull @Parameter(defaultValue = "0")
    private Long numbersSentencePause;
    
    @NotNull @Parameter(defaultValue = "-100")
    private Long numbersWordPause;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean numbersEnableZero;
    
    @NotNull @Parameter(defaultValue = "-100")
    private Long amountWordPause;
    
    @NotNull @Parameter
    private String actionsSequence;

    @Override
    protected IvrAction doCreateAction() {
        return new SayAnyAction(this, 
                NodeUtils.getAttrValuesByPrefixAndType(this, WORDS_NODE_ATTR, Node.class),
                NodeUtils.getAttrValuesByPrefixAndType(this, NUMBERS_NODE_ATTR, Node.class),
                NodeUtils.getAttrValuesByPrefixAndType(this, AMOUNT_NUMBERS_NODE_ATTR, Node.class),
                wordsSentencePause, wordsWordPause, 
                numbersGenus, numbersSentencePause, numbersWordPause, numbersEnableZero,
                amountWordPause, resourceManager);
    }

    public String getActionsSequence() {
        return actionsSequence;
    }

    public void setActionsSequence(String actionsSequence) {
        this.actionsSequence = actionsSequence;
    }

    public Node getWordsNode() {
        return wordsNode;
    }

    public void setWordsNode(Node wordsNode) {
        this.wordsNode = wordsNode;
    }

    public Node getNumbersNode() {
        return numbersNode;
    }

    public void setNumbersNode(Node numbersNode) {
        this.numbersNode = numbersNode;
    }

    public Node getAmountNumbersNode() {
        return amountNumbersNode;
    }

    public void setAmountNumbersNode(Node amountNumbersNode) {
        this.amountNumbersNode = amountNumbersNode;
    }

    public Long getWordsSentencePause() {
        return wordsSentencePause;
    }

    public void setWordsSentencePause(Long wordsSentencePause) {
        this.wordsSentencePause = wordsSentencePause;
    }

    public Long getWordsWordPause() {
        return wordsWordPause;
    }

    public void setWordsWordPause(Long wordsWordPause) {
        this.wordsWordPause = wordsWordPause;
    }

    public Genus getNumbersGenus() {
        return numbersGenus;
    }

    public void setNumbersGenus(Genus numbersGenus) {
        this.numbersGenus = numbersGenus;
    }

    public Long getNumbersSentencePause() {
        return numbersSentencePause;
    }

    public void setNumbersSentencePause(Long numbersSentencePause) {
        this.numbersSentencePause = numbersSentencePause;
    }

    public Long getNumbersWordPause() {
        return numbersWordPause;
    }

    public void setNumbersWordPause(Long numbersWordPause) {
        this.numbersWordPause = numbersWordPause;
    }

    public Boolean getNumbersEnableZero() {
        return numbersEnableZero;
    }

    public void setNumbersEnableZero(Boolean numbersEnableZero) {
        this.numbersEnableZero = numbersEnableZero;
    }

    public Long getAmountWordPause() {
        return amountWordPause;
    }

    public void setAmountWordPause(Long amountWordPause) {
        this.amountWordPause = amountWordPause;
    }
}
