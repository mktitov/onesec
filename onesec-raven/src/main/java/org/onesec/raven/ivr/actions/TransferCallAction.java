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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.expr.impl.BindingSupportImpl;

/**
 *
 * @author Mikhail Titov
 */
public class TransferCallAction extends AbstractAction
{
    public final static String ACTION_NAME = "Transfer call";
    
    private final TransferCallActionNode actionNode;

    public TransferCallAction(TransferCallActionNode actionNode)
    {
        super(ACTION_NAME);
        this.actionNode = actionNode;
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        BindingSupportImpl bindings = actionNode.getBindingSupport();
        final IvrEndpointConversation conversation = message.getConversation();
        bindings.putAll(conversation.getConversationScenarioState().getBindings());
        actionNode.getBindingSupport().enableScriptExecution();
        try {
            String address = actionNode.getAddress();
            if (getLogger().isDebugEnabled())
                getLogger().debug("Transfering call to the ("+address+") address");
            conversation.transfer(address, actionNode.getMonitorTransfer(), actionNode.getCallStartTimeout()*1000, 
                actionNode.getCallEndTimeout()*1000);
        } finally {
            bindings.reset();
        }
        return ACTION_EXECUTED_then_EXECUTE_NEXT;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
}
