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
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueuePrioritySelectorNode.class)
public class CallsQueueOperatorRefNode extends BaseNode implements CallsQueueOperatorRef
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueueOperator operator;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenario conversationScenario;

    public void setOperator(CallsQueueOperator operator) {
        this.operator = operator;
    }

    public CallsQueueOperator getOperator() {
        return operator;
    }

    public IvrConversationScenario getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenario conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request) {
        return operator.processRequest(queue, request, conversationScenario);
    }
}
