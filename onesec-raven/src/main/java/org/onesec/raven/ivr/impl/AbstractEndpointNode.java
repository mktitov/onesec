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

package org.onesec.raven.ivr.impl;

import java.util.concurrent.atomic.AtomicReference;
import org.onesec.core.StateListener;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractEndpointNode extends BaseNode
        implements IvrTerminal, StateListener<IvrTerminalState>
{
    @Service
    protected static ProviderRegistry providerRegistry;

    @Service
    protected static StateListenersCoordinator stateListenersCoordinator;

    @Service
    protected static TerminalStateMonitoringService terminalStateMonitoringService;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private RtpStreamManagerNode rtpStreamManager;

    @NotNull @Parameter
    private String address;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(defaultValue="AUTO")
    private Codec codec;

    @Parameter
    private Integer rtpPacketSize;

    @NotNull @Parameter(defaultValue="0")
    private Integer rtpMaxSendAheadPacketsCount;

    @NotNull @Parameter(defaultValue="false")
    private Boolean enableIncomingRtp;

    protected AtomicReference<CiscoJtapiTerminal> term;

    @Override
    protected void initFields() {
        super.initFields();
        term = new AtomicReference<CiscoJtapiTerminal>();
    }
    
    @Override
    protected void doInit() throws Exception {
        super.doInit();
        terminalStateMonitoringService.addTerminal(this);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        CiscoJtapiTerminal terminal = new CiscoJtapiTerminal(providerRegistry, stateListenersCoordinator, this);
        terminal.getState().addStateListener(this);
        term.set(terminal);
        terminal.start();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        CiscoJtapiTerminal terminal = term.getAndSet(null);
        if (terminal!=null) {
            terminal.getState().removeStateListener(this);
            terminal.stop();
            terminalStopped(terminal);
        }
    }

    protected abstract void terminalStopped(CiscoJtapiTerminal terminal);
    protected abstract void terminalStateChanged(IvrTerminalState state);

    public void addConversationListener(IvrEndpointConversationListener listener) {
        CiscoJtapiTerminal terminal = term.get();
        if (terminal!=null)
            terminal.addConversationListener(listener);
    }

    public void removeConversationListener(IvrEndpointConversationListener listener) {
        CiscoJtapiTerminal terminal = term.get();
        if (terminal!=null)
            terminal.removeConversationListener(listener);
    }

    public void stateChanged(IvrTerminalState state) {
        terminalStateChanged(state);
    }

    public String getObjectDescription() {
        return getPath();
    }

    public String getObjectName() {
        return getPath();
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executorService) {
        this.executor = executorService;
    }

    public Integer getRtpMaxSendAheadPacketsCount() {
        return rtpMaxSendAheadPacketsCount;
    }

    public void setRtpMaxSendAheadPacketsCount(Integer rtpMaxSendAheadPacketsCount) {
        this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;
    }

    public Boolean getEnableIncomingRtp() {
        return enableIncomingRtp;
    }

    public void setEnableIncomingRtp(Boolean enableIncomingRtp) {
        this.enableIncomingRtp = enableIncomingRtp;
    }

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    public Integer getRtpPacketSize() {
        return rtpPacketSize;
    }

    public void setRtpPacketSize(Integer rtpPacketSize) {
        this.rtpPacketSize = rtpPacketSize;
    }

    public RtpStreamManagerNode getRtpStreamManager() {
        return rtpStreamManager;
    }

    public void setRtpStreamManager(RtpStreamManagerNode rtpStreamManager) {
        this.rtpStreamManager = rtpStreamManager;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
