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

import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.*;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrEndpointNode extends AbstractEndpointNode
        implements IvrEndpoint, IvrEndpointConversationListener
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean enableIncomingCalls;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsRouter callsRouter;

    private IvrEndpointStateImpl endpointState;

    @Override
    protected void initFields()
    {
        super.initFields();
        endpointState = new IvrEndpointStateImpl(this);
        stateListenersCoordinator.addListenersToState(endpointState, IvrEndpointState.class);
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
    }

    @Override
    protected void doStart() throws Exception {
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
        super.doStart();
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings, String callingNumber)
    {
        CiscoJtapiTerminal _term = term.get();
        if (_term!=null) {
            changeStateTo(IvrEndpointState.INVITING, "INVITING");
            _term.invite(opponentNum, inviteTimeout, maxCallDur, listener, scenario, bindings, callingNumber);
        } else
            listener.conversationStopped(new IvrEndpointConversationStoppedEventImpl(
                    null, CompletionCode.TERMINAL_NOT_READY));
    }
    
    public List<CiscoJtapiTerminal.CallInfo> getCallsInfo() {
        CiscoJtapiTerminal terminal = term.get();
        return terminal==null? null : terminal.getCallsInfo();
    }
    
    private synchronized void changeStateTo(int stateId, String stateName) {
        boolean invalidTrans = false;
        switch (stateId) {
            case IvrEndpointState.OUT_OF_SERVICE: endpointState.setState(stateId); break;
            case IvrEndpointState.IN_SERVICE: endpointState.setState(stateId); break;
            case IvrEndpointState.TALKING:
                if (endpointState.getId()!=IvrEndpointState.OUT_OF_SERVICE)
                    endpointState.setState(stateId);
                else
                    invalidTrans = true;
                break;
            case IvrEndpointState.INVITING:
                if (endpointState.getId()==IvrEndpointState.IN_SERVICE)
                    endpointState.setState(stateId);
                else
                    invalidTrans = true;
                break;
        }
        if (invalidTrans && isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("Invalid state transition. Can't change state from (%s) to (%s)",
                    endpointState.getIdName(), stateName);

    }

    @Override
    protected void terminalCreated(CiscoJtapiTerminal terminal) {
        terminal.addConversationListener(this);
    }

    @Override
    protected void terminalStateChanged(IvrTerminalState state) {
        changeStateTo(state.getId(), state.getIdName());
    }

    @Override
    protected void terminalStopped(CiscoJtapiTerminal terminal) {
        terminal.removeConversationListener(this);
    }

    @Override
    protected String getEndpointStateAsString() {
        return endpointState.getIdName();
    }

    public void conversationStarted(IvrEndpointConversationEvent event) {
        changeStateTo(IvrEndpointState.TALKING, "TALKING");
    }

    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
        changeStateTo(IvrEndpointState.IN_SERVICE, "IN_SERVICE");
    }

    public void conversationTransfered(IvrEndpointConversationTransferedEvent event) { }

    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }

    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }

    public void listenerAdded(IvrEndpointConversationEvent event) { }

    public void dtmfReceived(IvrDtmfReceivedConversationEvent event) { }

    public IvrEndpointState getEndpointState() {
        return endpointState;
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public Boolean getEnableIncomingCalls() {
        return enableIncomingCalls;
    }

    public void setEnableIncomingCalls(Boolean enableIncomingCalls) {
        this.enableIncomingCalls = enableIncomingCalls;
    }

    public CallsRouter getCallsRouter() {
        return callsRouter;
    }

    public void setCallsRouter(CallsRouter callsRouter) {
        this.callsRouter = callsRouter;
    }
}