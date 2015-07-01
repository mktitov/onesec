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
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.impl.CallsQueueOperatorNode;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class IfQueueOperatorCallingActionNode extends BaseNode {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueuesNode callsQueues;

    public CallsQueuesNode getCallsQueues() {
        return callsQueues;
    }

    public void setCallsQueues(CallsQueuesNode callsQueues) {
        this.callsQueues = callsQueues;
    }

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
        String operNumber = (String)bindings.get(IvrEndpointConversation.NUMBER_BINDING);
        CallsQueueOperatorNode operator = callsQueues.getOperatorByPhoneNumber(operNumber);
        return operator!=null? super.getEffectiveChildrens() : null;
    }
}
