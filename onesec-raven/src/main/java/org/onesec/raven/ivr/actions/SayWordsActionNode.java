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

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class SayWordsActionNode extends BaseNode implements IvrActionNode {
    
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
    
    private BindingSupportImpl bindingSupport;

    public IvrAction createAction() {
        return new SayWordsAction(this, bindingSupport, wordsNode, pauseBetweenWords, resourceManager);
    }
    
    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

//    BindingSupportImpl getBindingSupport() {
//        return bindingSupport;
//    }
//
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
