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
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderConfiguratorListeners;
import org.onesec.raven.impl.RavenProviderConfiguratorImpl;
import org.onesec.raven.impl.StateToNodeLoggerImpl;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.*;
import org.raven.tree.ResourceDescriptor;
import org.raven.tree.ResourceRegistrator;
import org.raven.tree.TreeListener;
import org.raven.tree.impl.ResourceDescriptorImpl;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecRavenModule
{
    
    public static void bind(ServiceBinder binder) {
        binder.bind(StateToNodeLogger.class, StateToNodeLoggerImpl.class);
        binder.bind(TerminalStateMonitoringService.class, TerminalStateMonitoringServiceImpl.class);
        binder.bind(OnesecSystemNodesInitializer.class, OnesecSystemNodesInitializerImpl.class);
    }

    public RavenProviderConfigurator buildRavenProviderConfigurator(ProviderConfiguratorListeners listeners) {
        return new RavenProviderConfiguratorImpl(listeners);
    }
    
    @EagerLoad
    public CodecManager buildCodecManager(Logger logger) throws Exception {
        return new CodecManagerImpl(logger);
    }

    @EagerLoad
    public RTPManagerService buildRTPManagerService(Logger logger, CodecManager codecManager) 
            throws Exception
    {
        return new RTPManagerServiceImpl(logger, codecManager);
    }

    @EagerLoad
    public BufferCache buildBufferCache(RTPManagerService rtpManagerService, Logger logger
            , CodecManager codecManager) 
    {
        return new BufferCacheImpl(rtpManagerService, logger, codecManager);
    }

    public static void contributeStateListenersCoordinator(
            Configuration<StateListenerConfiguration> config
            , StateToNodeLogger stateToNodeLogger
            , TerminalStateMonitoringService terminalSSS)
    {
        config.add(new StateListenerConfigurationImpl(stateToNodeLogger, null));
        config.add(new StateListenerConfigurationImpl(
                terminalSSS, new Class[]{ProviderControllerState.class}));
    }

    public static void contributeTreeListeners(Configuration<TreeListener> conf
            , TerminalStateMonitoringService terminalStateMonitoringService
            , OnesecSystemNodesInitializer nodesInitializer)
    {
        conf.add(terminalStateMonitoringService);
        conf.add(nodesInitializer);
    }
    
    public static void contributeResourceManager(Configuration<ResourceRegistrator> conf) {
        conf.add(new SoundResourcesRegistrator());
    }
    
    public static void contributeTypeConverter(Configuration conf) {
        conf.add(new DataSourceToInputStreamSourceConverter());
        conf.add(new AudioFileToInputSourceConverter());
    }
    
    public static void contributeTemplateNodeBuildersProvider(Configuration<ResourceDescriptor> conf) {
        String base = "/org/onesec/raven/templates/";
        String vmail = "IVR/VMail/";
        conf.add(new ResourceDescriptorImpl(base, vmail+"Recording scenario.xml"));
        conf.add(new ResourceDescriptorImpl(base, vmail+"Listening scenario.xml"));
    }
}
