/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.queue.actions;

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.onesec.raven.ivr.queue.impl.CallsQueueOperatorNode;
import org.raven.conv.BindingScope;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class QueueOperatorStatusHandlerAction extends AsyncAction {
    
    public final static String HELLO_PLAYED_BINDING = "helloPlayedFlag";
    
    private final static String ACTION_NAME = "Queue operator status handler action";
    private final QueueOperatorStatusHandlerActionNode actionNode;
    

    public QueueOperatorStatusHandlerAction(QueueOperatorStatusHandlerActionNode actionNode) {
        super(ACTION_NAME);
        this.actionNode = actionNode;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conv) throws Exception {
        BindingSupport bindings = actionNode.getBindingSupport();
        try {
            Bindings convBindings = conv.getConversationScenarioState().getBindings();
            bindings.putAll(convBindings);
            String operNumber = (String) convBindings.get(IvrEndpointConversation.NUMBER_BINDING);
            CallsQueueOperatorNode oper = actionNode.getCallsQueues().getOperatorByPhoneNumber(operNumber);
            if (oper==null)
                return;
            boolean active = oper.getActive()!=null && oper.getActive();
            conv.getOwner().getLogger().debug("DTMF: "+convBindings.get(IvrEndpointConversation.DTMF_BINDING));
            conv.getOwner().getLogger().debug("active: "+active);
            if ("1".equals(convBindings.get(IvrEndpointConversation.DTMF_BINDING))) 
                oper.setActive(!active);
            if (!convBindings.containsKey(HELLO_PLAYED_BINDING)) {
                IvrUtils.playAudioInAction(this, conversation, actionNode.getHelloAudio());
                conv.getConversationScenarioState().setBinding(HELLO_PLAYED_BINDING, true, BindingScope.POINT);
                if (hasCancelRequest()) return;
            }
            IvrUtils.playAudioInAction(this, conversation, actionNode.getCurrentStatusAudio());
            if (hasCancelRequest()) return;
            if (oper.getActive()==null || !oper.getActive())
                IvrUtils.playAudioInAction(this, conversation, actionNode.getUnavailableStatusAudio());
            else
                IvrUtils.playAudioInAction(this, conversation, actionNode.getAvailableStatusAudio());
            if (hasCancelRequest()) return;
            IvrUtils.playAudioInAction(this, conversation, actionNode.getPressOneToChangeStatusAudio());
        } finally {
            bindings.reset();
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
