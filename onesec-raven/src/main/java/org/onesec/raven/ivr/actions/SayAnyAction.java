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
import org.onesec.raven.ivr.SayAnySubactionResult;
import org.onesec.raven.ivr.Sentence;
import org.onesec.raven.ivr.SubactionPauseResult;
import org.onesec.raven.ivr.SubactionSentencesResult;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

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
    private final long wordsSentencePause;
    private final long wordsWordPause;
    private final Genus numbersGenus;
    private final long numbersSentencePause;
    private final long numbersWordPause;
    private final boolean numbersEnableZero;
    private final long amountWordPause;
    private final SayAnyActionNode actionNode;
    private final ResourceManager resourceManager;

    public SayAnyAction(SayAnyActionNode actionNode, 
            List<Node> wordsNodes, List<Node> numbersNodes, List<Node> amountNumbersNodes, 
            long wordsSentencePause, long wordsWordPause,
            Genus numbersGenus, long numbersSentencePause, long numbersWordPause, boolean numbersEnableZero,
            long amountWordPause, ResourceManager resourceManager) 
    {
        super(NAME);
        this.wordsNodes = wordsNodes;
        this.numbersNodes = numbersNodes;
        this.amountNumbersNodes = amountNumbersNodes;
        this.actionNode = actionNode;
        this.wordsSentencePause = wordsSentencePause;
        this.wordsWordPause = wordsWordPause;
        this.numbersGenus = numbersGenus;
        this.numbersSentencePause = numbersSentencePause;
        this.numbersWordPause = numbersWordPause;
        this.numbersEnableZero = numbersEnableZero;
        this.amountWordPause = amountWordPause;
        this.resourceManager = resourceManager;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conv) throws Exception {
        actionNode.getBindingSupport().putAll(conversation.getConversationScenarioState().getBindings());
        try {
            String actionsSeq = actionNode.getActionsSequence();
            for (SayAnySubaction subaction: parseActionsSequence(actionsSeq)) {
                SayAnySubactionResult res = subaction.getResult();
                if (res instanceof SubactionPauseResult) {
                    IvrUtils.pauseInAction(this, ((SubactionPauseResult)res).getPause());
                } else if (res instanceof SubactionSentencesResult) {
                    SubactionSentencesResult sentences = (SubactionSentencesResult) res;
                    boolean first = true;
                    for (Sentence sentence: sentences.getSentences()) {
                        if (!first) IvrUtils.pauseInAction(this, sentences.getPauseBetweenSentences());
                        else first = false;
                        IvrUtils.playAudiosInAction(sentence.getWords(), this, conversation, sentence.getPauseBetweenWords());
                    }
                }
            }
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
                    case '#' : 
                        addPauseParams(params, numbersSentencePause, numbersWordPause);
                        addParam(params, SayAnyNumberSubaction.GENUS_PARAM, numbersGenus.name());
                        addParam(params, SayAnyNumberSubaction.ZERO_PARAM, numbersEnableZero? "yes":"no");
                        subactions.add(new SayAnyNumberSubaction(
                                value, params, actionNode, numbersNodes, resourceManager));
                        break;
                    case '$' : 
                        addPauseParams(params, 0l, amountWordPause);
                        subactions.add(new SayAnyAmountSubaction(
                                value, params, actionNode, amountNumbersNodes, resourceManager));
                        break;
                    case '^' : 
                        addPauseParams(params, wordsSentencePause, wordsWordPause);
                        subactions.add(new SayAnyWordSubaction(
                                value, params, actionNode, wordsNodes, resourceManager));
                        break;
                }
            } catch (Exception e) {
                throw new Exception(String.format(
                        "Invalid definition of action (%s). %s", actionConfig, e.getMessage()));
            }
        }
        return subactions;
    }
    
    private void addParam(Map<String, String> params, String name, String defValue) {
        if (!params.containsKey(name))
            params.put(name, defValue);
    }
    
    private void addPauseParams(Map<String, String> params, long pauseBetweenSentences, long pauseBentweenWords) {
        addParam(params, AbstractSentenceSubaction.SENTENCE_PAUSE_PARAM, Long.toString(pauseBetweenSentences));
        addParam(params, AbstractSentenceSubaction.SENTENCE_PAUSE_PARAM, Long.toString(pauseBentweenWords));
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
}
