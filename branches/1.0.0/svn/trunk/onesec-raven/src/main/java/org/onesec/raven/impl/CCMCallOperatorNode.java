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

package org.onesec.raven.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.StateToNodeLogger;
import org.raven.annotations.NodeClass;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=ServicesNode.class)
public class CCMCallOperatorNode extends BaseNode implements Viewable
{
    @Service
    private static StateToNodeLogger stateLogger;

    @Service
    private static ProviderRegistry providerRegistry;

    private ProvidersNode providersNode;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        generateNodes();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        generateNodes();
        stateLogger.setLoggerNode(this);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        stateLogger.setLoggerNode(null);
    }

    private void generateNodes() {
        providersNode = (ProvidersNode) getNode(ProvidersNode.NAME);
        if (providersNode == null) {
            providersNode = new ProvidersNode();
            this.addAndSaveChildren(providersNode);
            providersNode.start();
        }
    }

    public ProvidersNode getProvidersNode() {
        return providersNode;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        Collection<ProviderController> controllers = providerRegistry.getProviderControllers();
        TableImpl table = new TableImpl(new String[]{"Provider name", "Provider status", "Numbers range", 
            "Provider host", "Provider user", "Error message"});
        if (controllers!=null && !controllers.isEmpty())
            for (ProviderController controller: controllers) {
                String color = null;
                switch (controller.getState().getId()) {
                    case ProviderControllerState.IN_SERVICE : color = "GREEN"; break;
                    case ProviderControllerState.CONNECTING : color = "BLUE"; break;
                    default: color = "RED";
                }
                String state = String.format(
                        "<p style=\"text-align: left; color: %s; margin:0 0 0 0\"><b>%s</b></p>"
                        , color, controller.getState().getIdName());
                String numbersRange = controller.getFromNumber()+"-"+controller.getToNumber();
                table.addRow(new String[]{
                    controller.getName(), 
                    state, 
                    controller.getFromNumber()+"-"+controller.getToNumber(),
                    controller.getHost(), controller.getUser(),
                    controller.getState().getErrorMessage()});
            }
        return Arrays.asList((ViewableObject)new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }
}
