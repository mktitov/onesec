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

import java.util.Arrays;
import java.util.List;
import org.onesec.raven.impl.NumberToDigitConverter;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayAmountAction extends AbstractSayNumberAction
{
    public final static String NAME = "Say amount action";
    private final SayAmountActionNode actionNode;

    public SayAmountAction(SayAmountActionNode actionNode, Node numbersNode, long pauseBetweenWords, 
        ResourceManager resourceManager)
    {
        super(NAME, numbersNode, pauseBetweenWords, 0, resourceManager);
        this.actionNode = actionNode;
    }

    protected List<List> formWords(IvrEndpointConversation conversation) {
        actionNode.getBindingSupport().enableScriptExecution();
        try {
            return Arrays.asList((List)NumberToDigitConverter.getCurrencyDigits(actionNode.getAmount()));
        } finally {
            actionNode.getBindingSupport().reset();
        }
    }
}
