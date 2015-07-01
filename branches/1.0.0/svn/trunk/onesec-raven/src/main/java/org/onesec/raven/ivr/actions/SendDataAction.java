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
import org.raven.BindingNames;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class SendDataAction extends AsyncAction
{
    public final static String NAME = "Send data action";

    private final BindingSupport bindingSupport;
    private final SendDataActionNode actionNode;

    public SendDataAction(BindingSupport bindingSupport, SendDataActionNode actionNode)
    {
        super(NAME);
        this.bindingSupport = bindingSupport;
        this.actionNode = actionNode;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        try {
            bindingSupport.putAll(conversation.getConversationScenarioState().getBindings());
            DataContext dataContext = null;
            Object context = bindingSupport.get(BindingNames.DATA_CONTEXT_BINDING);
            if (context==null || !(context instanceof DataContext)) {
                dataContext = new DataContextImpl();
                bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, dataContext);
            } else
                dataContext = (DataContext) context;
            Object data = actionNode.getExpression();
            DataSourceHelper.sendDataToConsumers(actionNode, data, dataContext);
        } finally {
            bindingSupport.reset();
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
