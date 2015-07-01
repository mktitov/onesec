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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.CallRouteRule;
import org.onesec.raven.ivr.CallRouteRuleProvider;
import org.onesec.raven.ivr.CallsRouter;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CiscoCallsRouterNode extends BaseNode implements IvrTerminal, CallsRouter, Viewable
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
        CiscoJtapiRouteTerminal terminal = new CiscoJtapiRouteTerminal(providerRegistry, stateListenersCoordinator, this);
//        terminal.getState().addStateListener(this);
        term.set(terminal);
//        terminalCreated(terminal);
        terminal.start();
        addInternalRules(terminal);
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        CiscoJtapiRouteTerminal terminal = term.getAndSet(null);
        if (terminal!=null) {
            terminal.stop();
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
        try {
            if (data==null || !isStarted()) {            
                return;
            }
            if (!(data instanceof CallRouteRule)) {
                String mess = String.format("Invalid data type expected (%s) but received (%s)",
                            CallRouteRule.class.getName(), data.getClass().getName());
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(mess);
                context.addError(this, mess);
                return;
            }
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Received route rule: "+data);
            CiscoJtapiRouteTerminal terminal = term.get();
            if (terminal!=null)
                terminal.registerRoute((CallRouteRule)data);
        } finally {
            DataSourceHelper.executeContextCallbacks(this, context, data);
        }
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

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) {
        super.nodeStatusChanged(node, oldStatus, newStatus);
        getLogger().debug("node ({}) newStatus ({})", node.getName(), newStatus);
        if (node.getParent()!=this || !isStarted() || !(node instanceof CallRouteRuleProvider))
            return;
        CiscoJtapiRouteTerminal terminal = term.get();
        if (terminal==null)
            return;
        if (Status.STARTED.equals(newStatus))
            terminal.registerRoute(((CallRouteRuleProvider)node).getCallRouteRule());
        else if (Status.INITIALIZED.equals(newStatus))
            terminal.unregisterRoute(formRuleKey(node));
        
    }

    public String formRuleKey(Node node) {
        return "INTERNAL_RULE_"+node.getId();
    }
    
    public int getRouteRulePriority(Node node) {
        return 100+node.getIndex();
    }

    private void addInternalRules(CiscoJtapiRouteTerminal terminal) {
        for (CallRouteRuleProvider ruleProv: NodeUtils.getChildsOfType(this, CallRouteRuleProvider.class))
            terminal.registerRoute(ruleProv.getCallRouteRule());
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        TableImpl table = new TableImpl(new String[]{"Rule key", "Rule priority", "Permanent", "Rule description"});
        CiscoJtapiRouteTerminal terminal = term.get();
        if (terminal!=null) {
            for (CallRouteRule rule: terminal.getRoutes())
                table.addRow(new Object[]{rule.getRuleKey(), rule.getPriority(), rule.isPermanent(), rule.toString()});
        }
        ViewableObject vo = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        return Arrays.asList(vo);
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}
