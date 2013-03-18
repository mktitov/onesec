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
package org.onesec.raven.ivr.vmail.actions;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.Constants;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailManager;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class IfHasVMailBoxActionNode extends BaseNode implements Constants {
    public enum VMailBoxType {CALLING_NUMBER, LAST_REDIRECT_NUMBER};
    
    @NotNull @Parameter(defaultValue="LAST_REDIRECT_NUMBER")
    private VMailBoxType detectVMailBoxNumberForm;
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private VMailManager vmailManager;

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        final ConversationScenarioState state = getState(bindings);
        if (state!=null && !bindings.containsKey(VMAIL_BOX) && state.getBindings().containsKey(VMAIL_BOX)) {
            bindings.put(VMAIL_BOX, state.getBindings().get(VMAIL_BOX));
            bindings.put(VMAIL_BOX_NUMBER, state.getBindings().get(VMAIL_BOX_NUMBER));
        }
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        if (!isStarted())
            return null;
        ConversationScenarioState state = getState();
        if (state==null)
            return null;
        String vboxNumber = (String) (detectVMailBoxNumberForm==VMailBoxType.CALLING_NUMBER?
                          state.getBindings().get(IvrEndpointConversation.NUMBER_BINDING) :
                          state.getBindings().get(IvrEndpointConversation.LAST_REDIRECTED_NUMBER));
        if (vboxNumber==null || vboxNumber.isEmpty()) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Invalid voice mail number. Calling number: "+
                        state.getBindings().get(IvrEndpointConversation.NUMBER_BINDING));
            return null;
        }
        VMailBox vbox = vmailManager.getVMailBox(vboxNumber);
        if (vbox==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Not found voice mail box for number ({})", vboxNumber);
            return null;
        }
        state.setBinding(VMAIL_BOX, vbox, BindingScope.CONVERSATION);
        state.setBinding(VMAIL_BOX_NUMBER, vboxNumber, BindingScope.CONVERSATION);
        
        return super.getEffectiveNodes();
    }
    
    private ConversationScenarioState getState() {
        Bindings bindings = new SimpleBindings();
        getParent().formExpressionBindings(bindings);
        return getState(bindings);
    }
    
    private ConversationScenarioState getState(Bindings bindings) {
        return (ConversationScenarioState) bindings.get(IvrEndpointConversation.CONVERSATION_STATE_BINDING);
    }

    public VMailBoxType getDetectVMailBoxNumberForm() {
        return detectVMailBoxNumberForm;
    }

    public void setDetectVMailBoxNumberForm(VMailBoxType detectVMailBoxNumberForm) {
        this.detectVMailBoxNumberForm = detectVMailBoxNumberForm;
    }

    public VMailManager getVmailManager() {
        return vmailManager;
    }

    public void setVmailManager(VMailManager vmailManager) {
        this.vmailManager = vmailManager;
    }
}
