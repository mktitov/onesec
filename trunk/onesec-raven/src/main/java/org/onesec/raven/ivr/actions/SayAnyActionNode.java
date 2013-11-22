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
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyActionNode extends AbstractActionNode {
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private Node defaultWordsNode;
    
    @NotNull @Parameter(
            valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE, 
            defaultValue=Constants.NUMBERS_MALE_RESOURCE)
    private Node defaultNumbersNode;
    
    @NotNull @Parameter(
            valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE, 
            defaultValue=Constants.NUMBERS_MALE_RESOURCE)
    private Node defaultAmountNumbersNode;
    
    @NotNull @Parameter(defaultValue="MALE")
    private Genus defaultNumbersGenus;
    
    @NotNull @Parameter(defaultValue = "-100")
    private Long defaultPauseBetweenNumbers;
    
    @NotNull @Parameter
    private String actionsSequence;

    @Override
    protected IvrAction doCreateAction() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public Node getDefaultWordsNode() {
        return defaultWordsNode;
    }

    public void setDefaultWordsNode(Node defaultWordsNode) {
        this.defaultWordsNode = defaultWordsNode;
    }

    public Node getDefaultNumbersNode() {
        return defaultNumbersNode;
    }

    public void setDefaultNumbersNode(Node defaultNumbersNode) {
        this.defaultNumbersNode = defaultNumbersNode;
    }

    public Node getDefaultAmountNumbersNode() {
        return defaultAmountNumbersNode;
    }

    public void setDefaultAmountNumbersNode(Node defaultAmountNumbersNode) {
        this.defaultAmountNumbersNode = defaultAmountNumbersNode;
    }

    public Genus getDefaultNumbersGenus() {
        return defaultNumbersGenus;
    }

    public void setDefaultNumbersGenus(Genus defaultNumbersGenus) {
        this.defaultNumbersGenus = defaultNumbersGenus;
    }

    public Long getDefaultPauseBetweenNumbers() {
        return defaultPauseBetweenNumbers;
    }

    public void setDefaultPauseBetweenNumbers(Long defaultPauseBetweenNumbers) {
        this.defaultPauseBetweenNumbers = defaultPauseBetweenNumbers;
    }

    public String getActionsSequence() {
        return actionsSequence;
    }

    public void setActionsSequence(String actionsSequence) {
        this.actionsSequence = actionsSequence;
    }
    
    
}
