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
import java.util.List;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class AuthenticateOperatorActionNode extends BaseNode {
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueuesNode callQueues;

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
        List<String> dtmfs = (List<String>) bindings.get(IvrEndpointConversation.DTMFS_BINDING);
        if (dtmfs==null)
            return null;
        
        return super.getEffectiveChildrens();
    }

    public CallsQueuesNode getCallQueues() {
        return callQueues;
    }

    public void setCallQueues(CallsQueuesNode callQueues) {
        this.callQueues = callQueues;
    }
}
