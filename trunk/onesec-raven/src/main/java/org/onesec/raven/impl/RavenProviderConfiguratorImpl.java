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

import org.onesec.raven.*;
import java.util.Collection;
import org.onesec.core.provider.ProviderConfiguration;
import org.onesec.core.provider.ProviderConfiguratorListener;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.impl.FileProviderConfigurator;
import org.onesec.core.provider.impl.FileProviderConfigurator.EventType;
import org.onesec.core.provider.impl.ProviderConfiguratorStateImpl;
import org.onesec.core.services.ProviderConfiguratorListeners;
import static org.onesec.core.provider.ProviderConfiguratorState.*;

/**
 *
 * @author Mikhail Titov
 */
public class RavenProviderConfiguratorImpl implements RavenProviderConfigurator {
    public static final String RAVEN_PROVIDER_CONFIGURATOR = "Raven provider configurator";
    private final ProviderConfiguratorStateImpl state;
    private final Collection<ProviderConfiguratorListener> listeners;
    private ProvidersNode providersNode;

    public RavenProviderConfiguratorImpl(ProviderConfiguratorListeners listeners) {
        this.listeners = listeners.getListeners();
        state = new ProviderConfiguratorStateImpl(this);
        state.setState(STOPED);
    }
    
    public ProviderConfiguratorState getState() {
        return state;
    }

    public Collection<ProviderConfiguration> getAll() {
        return providersNode == null ? null : providersNode.getProviders();
    }

    public void add(ProviderConfiguration configuration) {
        fireProviderEvent(FileProviderConfigurator.EventType.ADD, configuration);
    }

    public void remove(ProviderConfiguration configuration) {
        fireProviderEvent(FileProviderConfigurator.EventType.REMOVE, configuration);
    }

    public void update(ProviderConfiguration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start() {
    }

    public String getObjectName() {
        return RAVEN_PROVIDER_CONFIGURATOR;
    }

    public String getObjectDescription() {
        return "";
    }

    public void shutdown() {
    }

    public void setProvidersNode(ProvidersNode providersNode) {
        this.providersNode = providersNode;
    }

    public ProvidersNode getProvidersNode() {
        return providersNode;
    }

    private void fireProviderEvent(EventType event, ProviderConfiguration configuration)
    {
        if (listeners!=null && !listeners.isEmpty())
            for (ProviderConfiguratorListener listener: listeners)
                switch (event)
                {
                    case ADD : listener.providerAdded(configuration); break;
                    case REMOVE : listener.providerRemoved(configuration); break;
                }
    }
}