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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.onesec.core.provider.ProviderControllerState;
import static org.onesec.core.provider.ProviderControllerState.*;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.Tree;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class TerminalStateMonitoringServiceImpl implements TerminalStateMonitoringService
{
    private final Map<IvrTerminal, IvrTerminal> terminals = new ConcurrentHashMap<IvrTerminal, IvrTerminal>();
    private final TerminalNodeListener terminalNodeListener = new TerminalNodeListener();
    private TerminalStateMonitoringServiceNode serviceNode = null;

    public void treeReloaded(Tree tree) {
//        Node services = tree.getRootNode().getNode(SystemNode.NAME).getNode(ServicesNode.NAME);
//        TerminalStateMonitoringServiceNode node = (TerminalStateMonitoringServiceNode) services.getNode(
//                TerminalStateMonitoringServiceNode.NAME);
//        if (node==null) {
//            node = new TerminalStateMonitoringServiceNode();
//            services.addAndSaveChildren(node);
//            node.start();
//        }
//        serviceNode = node;
    }

    public void treeInitialized(Tree tree) { 
        Node services = tree.getRootNode().getNode(SystemNode.NAME).getNode(ServicesNode.NAME);
        TerminalStateMonitoringServiceNode node = (TerminalStateMonitoringServiceNode) services.getNode(
                TerminalStateMonitoringServiceNode.NAME);
        if (node==null) {
            node = new TerminalStateMonitoringServiceNode();
            services.addAndSaveChildren(node);
//            node.start();
        }
        serviceNode = node;
    }

    public void addTerminal(IvrTerminal terminal) {
        terminals.put(terminal, terminal);
        terminal.addListener(terminalNodeListener);
    }

    public void removeTerminal(IvrTerminal terminal) {
        terminals.remove(terminal);
//        terminal.removeListener(terminalNodeListener);
    }

    public void stateChanged(ProviderControllerState state) {
        if (ObjectUtils.in(state.getId(), IN_SERVICE, OUT_OF_SERVICE, STOPED)) {
            if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
                serviceNode.getLogger().debug("Received event ({}) from provider ({})"
                        , state.getIdName(), state.getObservableObject().getName());
            int fromAddr = state.getObservableObject().getFromNumber();
            int toAddr = state.getObservableObject().getToNumber();
            for (IvrTerminal terminal: terminals.keySet()) 
                try {
                    int addr = Integer.parseInt(terminal.getAddress());
                    if (addr>=fromAddr && addr<=toAddr) {
                        switch (state.getId()){
                            case IN_SERVICE: startTerminal(terminal); break;
                            default: stopTerminal(terminal);
                        }
                    }
                } catch (NumberFormatException e) { }
        }
    }

    private void startTerminal(IvrTerminal term) {
        TerminalStateMonitoringServiceNode _serviceNode = serviceNode;
        if (_serviceNode != null) _serviceNode.startTerminal(term);
//        if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
//            serviceNode.getLogger().debug("Restarting terminal ({})", term.getPath());
////        synchronized(term){
//            if (term.isStarted()) term.stop();
//            if (term.isAutoStart()) term.start();
//            else if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.WARN))
//                serviceNode.getLogger().warn("Can't start terminal ({}) because of autoStart==false", term.getPath());        
////        }
    }

    private void stopTerminal(IvrTerminal term) {
        TerminalStateMonitoringServiceNode _serviceNode = serviceNode;
        if (_serviceNode != null) _serviceNode.stopTerminal(term);
////        synchronized(term){
//            if (term.isStarted()) {
//                if (serviceNode!=null && serviceNode.isLogLevelEnabled(LogLevel.DEBUG))
//                    serviceNode.getLogger().debug("Stopping terminal ({})", term.getPath());
//                term.stop();
//            }
////        }
    }
    
    private class TerminalNodeListener implements NodeListener {
        public boolean isSubtreeListener() {
            return false;
        }
        public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute) {
            return false;
        }
        public void nodeRemoved(Node removedNode) {
            removeTerminal((IvrTerminal)removedNode);
        }
        public void nodeStatusChanged(Node node, Node.Status oldStatus, Node.Status newStatus) { }
        public void nodeNameChanged(Node node, String oldName, String newName) { }
        public void nodeShutdowned(Node node) { }
        public void childrenAdded(Node owner, Node children) { }
        public void dependendNodeAdded(Node node, Node dependentNode) { }
        public void nodeMoved(Node node) {}
        public void nodeIndexChanged(Node node, int oldIndex, int newIndex) {}
        public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName) {}
        public void nodeAttributeValueChanged(Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue) {}

        
    }
}
