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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import org.onesec.raven.Constants;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class SayNumberActionNode extends BaseNode implements IvrActionNode {
    
    public final static String NUMBER_ATTR = "number";
    
    @Service
    private static ResourceManager resourceManager;
    
    @NotNull @Parameter
    private String number;
    
    @NotNull 
    @Parameter(valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE, 
               defaultValue=Constants.NUMBERS_FEMALE_RESOURCE)
    private Node numbersNode;
    
    @NotNull @Parameter(defaultValue="10") 
    private Integer pauseBetweenWords;
    
    @NotNull @Parameter(defaultValue="100")
    private Integer pauseBetweenNumbers;
    
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    BindingSupportImpl getBindingSupport() {
        return bindingSupport;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public IvrAction createAction() {
        return new SayNumberAction(this, numbersNode, pauseBetweenWords, pauseBetweenNumbers, resourceManager);
    }
    
    public Collection<Long> getNumbersSequence() {
        List<Pattern> patterns = NodeUtils.getChildsOfType(this, Pattern.class);
        List<String> strNumbers = new LinkedList<String>();
        String _number = number;
        if (patterns.isEmpty()) 
            strNumbers.add(_number);
        else
            for (Pattern pattern: patterns) {
                Collection<String> groups = pattern.matches(_number);
                if (groups!=null) {
                    strNumbers.addAll(groups);
                    break;
                }
            }
        if (strNumbers.isEmpty())
            return Collections.EMPTY_LIST;
        List<Long> numbers = new ArrayList<Long>(strNumbers.size());
        for (String strNumber: strNumbers) 
            try {
                numbers.add(Long.parseLong(strNumber));
            } catch (NumberFormatException e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Can't convert ({}) to integer");
                return Collections.EMPTY_LIST;
            }
        return numbers;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Node getNumbersNode() {
        return numbersNode;
    }

    public void setNumbersNode(Node numbersNode) {
        this.numbersNode = numbersNode;
    }

    public Integer getPauseBetweenWords() {
        return pauseBetweenWords;
    }

    public void setPauseBetweenWords(Integer pauseBetweenWords) {
        this.pauseBetweenWords = pauseBetweenWords;
    }

    public Integer getPauseBetweenNumbers() {
        return pauseBetweenNumbers;
    }

    public void setPauseBetweenNumbers(Integer pauseBetweenNumbers) {
        this.pauseBetweenNumbers = pauseBetweenNumbers;
    }
}
