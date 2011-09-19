/*
 *  Copyright 2011 Mikhail Titov.
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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class SendMessageAction extends AsyncAction
{
    public final static String NAME = "Send message action";

    private final SendMessageActionNode actionNode;
    private final BindingSupport bindingSupport;

    public SendMessageAction(SendMessageActionNode actionNode, BindingSupport bindingSupport)
    {
        super(NAME);
        this.actionNode = actionNode;
        this.bindingSupport = bindingSupport;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        try {
            bindingSupport.putAll(conversation.getConversationScenarioState().getBindings());
            String message = actionNode.getMessage();
            conversation.sendMessage(message, actionNode.getEncoding().name(), actionNode.getSendDirection());
        } finally {
            bindingSupport.reset();
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }

}
