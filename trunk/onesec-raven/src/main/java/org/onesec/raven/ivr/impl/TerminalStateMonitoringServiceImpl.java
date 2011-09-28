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

import org.raven.tree.Node;
import java.util.HashSet;
import java.util.Set;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.onesec.raven.ivr.IvrTerminal;
import org.raven.log.LogLevel;
import org.raven.tree.Tree;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;
import org.weda.beans.ObjectUtils;
import static org.onesec.core.provider.ProviderControllerState.*;

/**
 *
 * @author Mikhail Titov
 */
public class TerminalStateMonitoringServiceImpl implements TerminalStateMonitoringService
{
    private final Set<IvrTerminal> terminals = new HashSet<IvrTerminal>();
    private TerminalStateMonitoringServiceNode serviceNode = null;

    public void treeReloaded(Tree tree)
    {
        Node services = tree.getRootNode().getChildren(SystemNode.NAME).getChildren(ServicesNode.NAME);
        TerminalStateMonitoringServiceNode node = (TerminalStateMonitoringServiceNode) services.getChildren(
                TerminalStateMonitoringServiceNode.NAME);
        if (node==null){
            node = new TerminalStateMonitoringServiceNode();
            services.addAndSaveChildren(node);
            node.start();
        }
        serviceNode = node;
    }

    public synchronized void addTerminal(IvrTerminal terminal)
    {
        terminals.add(terminal);
    }

    public void removeTerminal(IvrTerminal terminal) {
        terminals.remove(terminal);
    }

    public synchronized void stateChanged(ProviderControllerState state)
    {
        if (ObjectUtils.in(state.getId(), IN_SERVICE, OUT_OF_SERVICE, STOPED)) {
            if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
                serviceNode.getLogger().debug("Received event ({}) from provider ({})"
                        , state.getIdName(), state.getObservableObject().getName());
            int fromAddr = state.getObservableObject().getFromNumber();
            int toAddr = state.getObservableObject().getToNumber();
            for (IvrTerminal terminal: terminals) {
                try{
                    int addr = Integer.parseInt(terminal.getAddress());
                    if (addr>=fromAddr && addr<=toAddr) {
                        switch (state.getId()){
                            case IN_SERVICE: startTerminal(terminal); break;
                            default: stopTerminal(terminal);
                        }
                    }
                }catch(NumberFormatException e){
                }
            }
        }
    }

    private void startTerminal(IvrTerminal term)
    {
        synchronized(term){
            if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
                serviceNode.getLogger().debug("Restarting terminal ({})", term.getPath());
            if (term.getStatus().equals(Node.Status.STARTED))
                term.stop();
            if (term.isAutoStart())
                term.start();
            else if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.WARN))
                serviceNode.getLogger().warn(
                        "Can't start terminal ({}) because of autoStart==false", term.getPath());

        }
    }

    private void stopTerminal(IvrTerminal term)
    {
        synchronized(term){
            if (term.getStatus().equals(Node.Status.STARTED)) {
                if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
                    serviceNode.getLogger().debug("Stopping terminal ({})", term.getPath());
                term.stop();
            }
        }
    }
}
