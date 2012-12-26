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
package org.onesec.raven.ivr.impl;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.CallRouteRule;
import org.onesec.raven.ivr.CallsRouter;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CiscoCallsRouterNode extends BaseNode implements IvrTerminal, CallsRouter
{
    @Service private static ProviderRegistry providerRegistry;
    @Service private static StateListenersCoordinator stateListenersCoordinator;
    @Service private static TerminalStateMonitoringService terminalStateMonitoringService;
    
    
    @NotNull @Parameter
    private String address;

    private AtomicReference<CiscoJtapiRouteTerminal> term;
    
//    private CiscoRoute

    @Override
    protected void initFields() {
        super.initFields();
        term = new AtomicReference<CiscoJtapiRouteTerminal>();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        terminalStateMonitoringService.addTerminal(this);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        super.doStart();
        CiscoJtapiRouteTerminal terminal = new CiscoJtapiRouteTerminal(providerRegistry, stateListenersCoordinator, this);
//        terminal.getState().addStateListener(this);
        term.set(terminal);
//        terminalCreated(terminal);
        terminal.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
//        super.doStop();
        CiscoJtapiRouteTerminal terminal = term.getAndSet(null);
        if (terminal!=null) {
//            terminal.getState().removeStateListener(this);
            terminal.stop();
//            terminalStopped(terminal);
        }
    }
    
    public IvrTerminalState getTerminalState() {
        CiscoJtapiRouteTerminal terminal = term.get();
        return terminal==null? null : terminal.getState();
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
//    private void

    public String getObjectName() {
        return "CiscoCallsRouteTerminal - "+address;
    }

    public String getObjectDescription() {
        return getObjectName();
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (data==null || getStatus()!=Node.Status.STARTED)
            return;
        if (!(data instanceof CallRouteRule))
            if (isLogLevelEnabled(LogLevel.ERROR))
            getLogger().error("Invalid data type expected ({}) but received ({})",
                    CallRouteRule.class.getName(), data.getClass().getName());
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Received route rule: "+data);
        CiscoJtapiRouteTerminal terminal = term.get();
        if (terminal!=null)
            terminal.registerRoute((CallRouteRule)data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException(
                "refreshData operation is unsupported by this data source");
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this dataSource");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
}
