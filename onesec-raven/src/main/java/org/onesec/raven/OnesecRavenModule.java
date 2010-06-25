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

package org.onesec.raven;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.onesec.core.StateListenerConfiguration;
import org.onesec.core.impl.StateListenerConfigurationImpl;
import org.onesec.raven.impl.RavenProviderConfiguratorImpl;
import org.onesec.core.services.ProviderConfiguratorListeners;
import org.onesec.raven.impl.StateToNodeLoggerImpl;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.ivr.impl.RTPManagerServiceImpl;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecRavenModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(StateToNodeLogger.class, StateToNodeLoggerImpl.class);
    }

    public RavenProviderConfigurator buildRavenProviderConfigurator(
            ProviderConfiguratorListeners listeners)
    {
        return new RavenProviderConfiguratorImpl(listeners);
    }

    @EagerLoad
    public RTPManagerService buildRTPManagerService(Logger logger) throws Exception
    {
        return new RTPManagerServiceImpl(logger);
    }

    public static void contributeStateListenersCoordinator(
            Configuration<StateListenerConfiguration> config
            , StateToNodeLogger stateToNodeLogger)
    {
        config.add(new StateListenerConfigurationImpl(stateToNodeLogger, null));
    }
}
