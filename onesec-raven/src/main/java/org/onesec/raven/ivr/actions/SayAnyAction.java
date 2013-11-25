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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.onesec.raven.impl.Genus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.SayAnySubaction;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyAction extends AsyncAction {
    public final static String NAME = "Say any action";
    private final static String[] EMPTY_ARR = new String[]{};
    
    private final List<Node> wordsNodes;
    private final List<Node> numbersNodes;
    private final List<Node> amountNumbersNodes;
    private final Genus numbersGenus;
    private final long pauseBetweenNumbers;
    private final SayAnyActionNode actionNode;

    public SayAnyAction(List<Node> wordsNodes, List<Node> numbersNodes, List<Node> amountNumbersNodes, 
            Genus numbersGenus, long pauseBetweenNumbers, SayAnyActionNode actionNode, String actionName) 
    {
        super(NAME);
        this.wordsNodes = wordsNodes;
        this.numbersNodes = numbersNodes;
        this.amountNumbersNodes = amountNumbersNodes;
        this.numbersGenus = numbersGenus;
        this.pauseBetweenNumbers = pauseBetweenNumbers;
        this.actionNode = actionNode;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conv) throws Exception {
        actionNode.getBindingSupport().putAll(conversation.getConversationScenarioState().getBindings());
        try {
            String actionsSeq = actionNode.getActionsSequence();
            
        } finally {
            actionNode.getBindingSupport().reset();
        }
    }
    
    private List<SayAnySubaction> parseActionsSequence(String actionsSeq) throws Exception {
        if (actionsSeq==null || actionsSeq.trim().isEmpty()) {
            if (logger.isDebugEnabled())
                logger.debug("Nothing to say!");
            return Collections.EMPTY_LIST;
        }
        List<SayAnySubaction> subactions = new LinkedList<SayAnySubaction>();
        for (String actionConfig: actionsSeq.trim().split("\\s+")) {
            try {
                if (actionConfig.length()<2)
                    throw new Exception("Length must be greater than 1");
                char actionType = actionConfig.charAt(0);
                String[] paramsAndValue = decodeParamsAndValue(actionConfig);
                String value = paramsAndValue[paramsAndValue.length-1];
                Map<String, String> params = decodeParams(paramsAndValue);
                switch (actionType) {
                    case '@' : subactions.add(new SayAnyPauseSubaction(value)); break;
                    case '#' : break;
                    case '$' : break;
                    case '^' : break;
                }
            } catch (Exception e) {
                throw new Exception(String.format(
                        "Invalid definition of action (%s). %s", actionConfig, e.getMessage()));
            }
        }
        return subactions;
    }

    private Map<String, String> decodeParams(String[] paramsAndValue) throws Exception {
        String[] paramsSeq = paramsAndValue.length==1? EMPTY_ARR : paramsAndValue[0].split(";");
        Map<String, String> params = new HashMap<String, String>();
        for (String paramConfig: paramsSeq) {
            String[] nameAndValue = paramConfig.split("=");
            if (nameAndValue.length!=2)
                throw new Exception(String.format("Invalid parameter (%s) definition", paramConfig));
            params.put(nameAndValue[0], nameAndValue[1]);
        }
        return params;
    }

    private String[] decodeParamsAndValue(String actionConfig) throws Exception {
        String[] paramsAndValue = actionConfig.substring(1).split(":");
        if (paramsAndValue.length == 0 || paramsAndValue.length > 2)
            throw new Exception("");
        return paramsAndValue;
    }

    public boolean isFlowControlAction() {
        return false;
    }
    
    public interface Action {};
}
