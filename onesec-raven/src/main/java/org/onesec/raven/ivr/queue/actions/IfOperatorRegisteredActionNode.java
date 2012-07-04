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

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.IvrBindingNames;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.onesec.raven.ivr.queue.OperatorRegistrator;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class IfOperatorRegisteredActionNode extends BaseNode implements IvrBindingNames {

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueuesNode callsQueues;
    
    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens() {
        if (getStatus()!=Node.Status.STARTED)
            return null;
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        String operatorNumber = (String) bindings.get(IvrEndpointConversation.NUMBER_BINDING);
        OperatorRegistrator registrator = callsQueues.getOperatorRegistrator();
        OperatorDesc operator = registrator.getCurrentOperator(operatorNumber);
        getConversationState(bindings).setBinding(OPERATOR_BINDING, operator, BindingScope.CONVERSATION);
        return operator==null? null : super.getEffectiveChildrens();
    }
    
    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        ConversationScenarioState state = getConversationState(bindings);
        if (state!=null && state.getBindings().containsKey(OPERATOR_BINDING)) 
            bindings.put(OPERATOR_BINDING, state.getBindings().get(OPERATOR_BINDING));
    }
    
    private ConversationScenarioState getConversationState(Bindings bindings) {
        return (ConversationScenarioState) bindings.get(IvrEndpointConversation.CONVERSATION_STATE_BINDING);
    }

    public CallsQueuesNode getCallsQueues() {
        return callsQueues;
    }

    public void setCallsQueues(CallsQueuesNode callsQueues) {
        this.callsQueues = callsQueues;
    }
}
