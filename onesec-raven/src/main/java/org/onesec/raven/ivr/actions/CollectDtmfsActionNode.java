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
package org.onesec.raven.ivr.actions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import static org.onesec.raven.ivr.IvrEndpointConversation.*;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class CollectDtmfsActionNode extends BaseNode {
    
    @NotNull @Parameter(defaultValue="*")
    private String stopDtmf;
    
    @NotNull @Parameter(defaultValue="POINT")
    private BindingScope bindingScope;

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
        String tempDtmfsKey = RavenUtils.generateKey("dtmfs", this);
        List<String> dtmfs = (List<String>) getConversationState(bindings).getBindings().get(tempDtmfsKey);
        if (dtmfs==null) {
            dtmfs = new LinkedList<String>();
            getConversationState(bindings).setBinding(tempDtmfsKey, dtmfs, BindingScope.POINT);
            getConversationState(bindings).setBinding(DTMFS_BINDING, dtmfs, bindingScope);
        }
        String dtmf = (String) bindings.get(DTMF_BINDING);
        if (stopDtmf.equals(dtmf)) {
            getConversationState(bindings).setBinding(tempDtmfsKey, null, BindingScope.POINT);
            return super.getEffectiveChildrens();
        } else {
            if (!(EMPTY_DTMF+"").equals(dtmf))
                dtmfs.add(dtmf);
            return null;
        }
    }
    
    private ConversationScenarioState getConversationState(Bindings bindings) {
        return (ConversationScenarioState) bindings.get(CONVERSATION_STATE_BINDING);
    }

    public BindingScope getBindingScope() {
        return bindingScope;
    }

    public void setBindingScope(BindingScope bindingScope) {
        this.bindingScope = bindingScope;
    }

    public String getStopDtmf() {
        return stopDtmf;
    }

    public void setStopDtmf(String stopDtmf) {
        this.stopDtmf = stopDtmf;
    }
}
