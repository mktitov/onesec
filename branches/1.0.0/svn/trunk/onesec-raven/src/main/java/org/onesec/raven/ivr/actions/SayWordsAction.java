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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.onesec.raven.ivr.IvrEndpointConversation;
import static org.raven.RavenUtils.*;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SayWordsAction extends AbstractSayWordsAction {
    public final static String ACTION_NAME = "Say words action";
    private final BindingSupportImpl actionNodeBindingSupport;
    private final SayWordsActionNode actionNode;

    public SayWordsAction(SayWordsActionNode actionNode, BindingSupportImpl actionNodeBindingSupport, 
            Collection<Node> wordsNode, long pauseBetweenWords, ResourceManager resourceManager) 
    {
        super(ACTION_NAME, wordsNode, pauseBetweenWords, 0, resourceManager);
        this.actionNodeBindingSupport = actionNodeBindingSupport;
        this.actionNode = actionNode;
    }

    @Override
    protected List<List> formWords(IvrEndpointConversation conversation) {
        try {
            actionNodeBindingSupport.putAll(conversation.getConversationScenarioState().getBindings());
            return Arrays.asList((List)arrayToList(split(actionNode.getWords(), " ")));
        } finally {
            actionNodeBindingSupport.reset();
        }
    }
}
