/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.List;
import java.util.Map;
import org.onesec.core.StateListener;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.IvrMultichannelEndpoint;
import org.onesec.raven.ivr.IvrMultichannelEndpointState;
import org.onesec.raven.ivr.IvrTerminalState;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titiov
 */
@NodeClass
public class IvrMultichannelEndpointNode extends AbstractEndpointNode
        implements IvrMultichannelEndpoint, Viewable, StateListener<IvrTerminalState>
{

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;
    
    private IvrMultichannelEndpointStateImpl endpointState;

    @Override
    protected void initFields() {
        super.initFields();
        endpointState = new IvrMultichannelEndpointStateImpl(this);
        stateListenersCoordinator.addListenersToState(endpointState, IvrEndpointState.class);
        endpointState.setState(IvrMultichannelEndpointState.OUT_OF_SERVICE);
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        endpointState.setState(IvrMultichannelEndpointState.OUT_OF_SERVICE);
    }

    @Override
    protected void doStart() throws Exception {
        endpointState.setState(IvrMultichannelEndpointState.OUT_OF_SERVICE);
        super.doStart();
    }

    @Override
    protected String getEndpointStateAsString() {
        return endpointState.getIdName();
    }

    @Override
    protected void terminalCreated(CiscoJtapiTerminal terminal) { }

    @Override
    protected void terminalStopped(CiscoJtapiTerminal terminal) {
        endpointState.setState(IvrMultichannelEndpointState.OUT_OF_SERVICE);
    }
    
    protected void terminalStateChanged(IvrTerminalState state) {
        endpointState.setState(state.getId());
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        CiscoJtapiTerminal terminal = term.get();
        return terminal==null? null : terminal.getViewableObjects();
    }

    public IvrMultichannelEndpointState getEndpointState() {
        return endpointState;
    }

    public Boolean getEnableIncomingCalls() {
        return Boolean.TRUE;
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }
}
