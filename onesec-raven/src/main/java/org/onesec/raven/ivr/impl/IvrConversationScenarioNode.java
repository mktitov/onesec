/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrDtmfReceivedConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.actions.DtmfProcessPointActionNode;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.onesec.raven.ivr.actions.TransferCallActionNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.ConversationScenarioState;
import org.raven.conv.impl.ConversationScenarioNode;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.expr.impl.SwitchNode;
import org.raven.tree.Node;
import org.raven.tree.impl.ChildAttributesValueHandlerFactory;
import org.raven.tree.impl.GroupNode;
import org.raven.tree.impl.GroupReferenceNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={
    IvrConversationScenarioPointNode.class, IfNode.class, SwitchNode.class, GotoNode.class, GroupNode.class,
    GroupReferenceNode.class, StopConversationActionNode.class, PlayAudioActionNode.class, PauseActionNode.class,
    TransferCallActionNode.class, DtmfProcessPointActionNode.class})
public class IvrConversationScenarioNode extends ConversationScenarioNode
        implements IvrConversationScenario, IvrEndpointConversationListener
{
    @Parameter
    private String validDtmfs;
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String stopConversationHandler;
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE, parent = "stopConversationHandler")
    private Object onStopConversation;
    @NotNull @Parameter(defaultValue = "false", parent = "stopConversationHandler")
    private Boolean useOnStopConversation;
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String dtmfReceivedHandler;
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE, parent = "dtmfReceivedHandler")
    private Object onDtmfReceived;
    @NotNull @Parameter(defaultValue = "false", parent = "dtmfReceivedHandler")
    private Boolean useOnDtmfReceived;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
    }

    private void initNodes(boolean start) {
        Node groupsNode = getNode(ActionGroupsNode.NAME);
        if (groupsNode==null) {
            groupsNode = new ActionGroupsNode();
            addAndSaveChildren(groupsNode);
            if (start)
                groupsNode.start();
        }
    }

    public void setValidDtmfs(String validDtmfs)
    {
        this.validDtmfs = validDtmfs;
    }

    public String getValidDtmfs()
    {
        return validDtmfs;
    }

    public String getStopConversationHandler() {
        return stopConversationHandler;
    }

    public void setStopConversationHandler(String stopConversationHandler) {
        this.stopConversationHandler = stopConversationHandler;
    }

    public void conversationCreated(IvrEndpointConversation conversation) {
        if (useOnStopConversation || useOnDtmfReceived)
            conversation.addConversationListener(this);
    }

    public Object getOnStopConversation() {
        return onStopConversation;
    }

    public void setOnStopConversation(Object onStopConversation) {
        this.onStopConversation = onStopConversation;
    }

    public Boolean getUseOnStopConversation() {
        return useOnStopConversation;
    }

    public void setUseOnStopConversation(Boolean useOnStopConversation) {
        this.useOnStopConversation = useOnStopConversation;
    }

    public String getDtmfReceivedHandler() {
        return dtmfReceivedHandler;
    }

    public void setDtmfReceivedHandler(String dtmfReceivedHandler) {
        this.dtmfReceivedHandler = dtmfReceivedHandler;
    }

    public Object getOnDtmfReceived() {
        return onDtmfReceived;
    }

    public void setOnDtmfReceived(Object onDtmfReceived) {
        this.onDtmfReceived = onDtmfReceived;
    }

    public Boolean getUseOnDtmfReceived() {
        return useOnDtmfReceived;
    }

    public void setUseOnDtmfReceived(Boolean useOnDtmfReceived) {
        this.useOnDtmfReceived = useOnDtmfReceived;
    }
    

    //IvrEndpointConversationListener methods
    public void listenerAdded(IvrEndpointConversationEvent event) {
    }

    @Override
    public void connectionEstablished(IvrEndpointConversationEvent event) {
    }

    public void conversationStarted(IvrEndpointConversationEvent event) {
    }

    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
        if (useOnStopConversation) {
            initBindings(event);
            try {
                Object res = onStopConversation;
            } finally {
                bindingSupport.reset();
            }
        }
    }

    public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
    }

    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) {
    }

    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) {
    }

    public void dtmfReceived(IvrDtmfReceivedConversationEvent event) {
        if (useOnDtmfReceived) {
            initBindings(event);
            bindingSupport.put(IvrEndpointConversation.DTMF_BINDING, ""+event.getDtmf());
            try {
                Object res = onDtmfReceived;
            } finally {
                bindingSupport.reset();
            }
        }
    }

    private void initBindings(IvrEndpointConversationEvent event) {
        final ConversationScenarioState state = event.getConversation().getConversationScenarioState();
        bindingSupport.put(IvrEndpointConversation.CONVERSATION_STATE_BINDING, state);
        if (state!=null)
            bindingSupport.putAll(state.getBindings());
    }
}
