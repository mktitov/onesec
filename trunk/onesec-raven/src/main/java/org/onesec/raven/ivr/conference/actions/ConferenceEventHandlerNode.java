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
package org.onesec.raven.ivr.conference.actions;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
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
public class ConferenceEventHandlerNode extends BaseNode {

    @NotNull @Parameter
    private ConferenceSessionStatus eventType;

    public ConferenceSessionStatus getEventType() {
        return eventType;
    }

    public void setEventType(ConferenceSessionStatus eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        if (!isStarted())
            return null;
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        ConferenceSessionState state = (ConferenceSessionState)bindings.get(JoinToConferenceAction.CONFERENCE_STATE_BINDING);
        return checkStatus(state)? super.getEffectiveNodes() : null;
    }

    private boolean checkStatus(ConferenceSessionState state) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Selecting behaviour for status: {}", state==null?null:state.getStatus());
        return state!=null && state.getStatus()==eventType;
    }
}
