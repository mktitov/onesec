/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core.services;

import java.util.Collection;
import javax.telephony.JtapiPeerUnavailableException;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.onesec.core.StateListenerConfiguration;
import org.onesec.core.impl.ApplicationHomeImpl;
import org.onesec.core.impl.OperatorImpl;
import org.onesec.core.impl.ProviderConfiguratorListenersImpl;
import org.onesec.core.impl.StateListenerConfigurationImpl;
import org.onesec.core.impl.StateListenersCoordinatorImpl;
import org.onesec.core.impl.StateLogger;
import org.onesec.core.provider.ProviderConfiguratorListener;
import org.onesec.core.provider.impl.FileProviderConfigurator;
import org.onesec.core.provider.impl.ProviderRegistryImpl;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public final class OnesecCoreModule {
    
    public static void bind(ServiceBinder binder) {
        binder.bind(ApplicationHome.class, ApplicationHomeImpl.class);
        binder.bind(ProviderConfiguratorListeners.class, ProviderConfiguratorListenersImpl.class);
    }
    
    public static StateListenersCoordinator buildStateListenersCoordinator(
            Collection<StateListenerConfiguration> configurations)
    {
        return new StateListenersCoordinatorImpl(configurations);
    }
    
    @EagerLoad
    public static ProviderConfigurator buildProviderConfigurator(
            ApplicationHome applicationHome
            , ProviderConfiguratorListeners listeners
            , Logger logger
            , RegistryShutdownHub registryShutdownHub)
    {
        FileProviderConfigurator configurator =  
                new FileProviderConfigurator(applicationHome.getHome(), listeners, logger);
        registryShutdownHub.addRegistryShutdownListener(configurator);
        return configurator;
    }
    
    public static ProviderRegistry buildProviderRegistry(
            StateListenersCoordinator listenersCoordinator
            , Logger log
            , RegistryShutdownHub registryShutdownHub) throws JtapiPeerUnavailableException
    {
        ProviderRegistryImpl providerRegistry = 
                new ProviderRegistryImpl(listenersCoordinator, log);
        registryShutdownHub.addRegistryShutdownListener(providerRegistry);
        
        return providerRegistry;
    }
    
    public static Operator buildOperator(
            ProviderRegistry providerRegistry, StateListenersCoordinator listenersCoordinator)
    {
        return new OperatorImpl(providerRegistry, listenersCoordinator);
    }
    
    public static void contributeProviderConfiguratorListeners(
            ProviderRegistry providerRegistry
            , Configuration<ProviderConfiguratorListener> config)
    {
        config.add(providerRegistry);
    }
    
    public static void contributeStateListenersCoordinator(
            Configuration<StateListenerConfiguration> config)
    {
        config.add(new StateListenerConfigurationImpl(new StateLogger(), null));
    }
}
