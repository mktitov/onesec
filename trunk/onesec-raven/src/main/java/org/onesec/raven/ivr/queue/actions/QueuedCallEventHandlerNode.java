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

package org.onesec.raven.ivr.queue.actions;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class QueuedCallEventHandlerNode extends BaseNode
{
    public enum Status {QUEUING, READY_TO_COMMUTATE, DISCONNECTED, REJECTED}

    @NotNull @Parameter
    private Status eventType;

    public Status getEventType() {
        return eventType;
    }

    public void setEventType(Status eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens()
    {
        if (getStatus()!=Node.Status.STARTED)
            return null;
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        QueuedCallStatus status = (QueuedCallStatus)bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING);        
        return checkStatus(status)? super.getEffectiveChildrens() : null;
    }

    private boolean checkStatus(QueuedCallStatus status)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Selecting behaviour for status: {}", status==null?null:status.getStatus());
        if (status==null)
            return false;
        Status event = eventType;
        switch (status.getStatus()) {
            case QUEUEING: return event==Status.QUEUING;
            case DISCONNECTED: return event==Status.DISCONNECTED;
            case REJECTED: return event==Status.REJECTED;
            case READY_TO_COMMUTATE: return event==Status.READY_TO_COMMUTATE;
            default: return false;
        }
    }
}
