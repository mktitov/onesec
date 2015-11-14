/*
 *  Copyright 2007 Mikhail K. Titov.
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

package org.onesec.core.provider.impl;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.JtapiPeerUnavailableException;
import org.apache.tapestry5.ioc.services.RegistryShutdownListener;
import org.onesec.core.provider.ProviderRegistryException;
import org.onesec.core.provider.ProviderConfiguration;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail K. Titov.
 */
public class ProviderRegistryImpl implements ProviderRegistry, RegistryShutdownListener 
{
    private final NavigableMap<Integer, ProviderController> controllers = new ConcurrentSkipListMap();
    
    private final StateListenersCoordinator stateListenersCoordinator;
    private final Logger log;
    
    private boolean stoped;
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(1);
    private final ExecutorService connectExecutor = Executors.newSingleThreadExecutor();
    private final JtapiPeer jtapiPeer;

    public ProviderRegistryImpl(StateListenersCoordinator stateListenersCoordinator, Logger log)
            throws JtapiPeerUnavailableException
    {
        this.stateListenersCoordinator = stateListenersCoordinator;
        this.log = log;
        reconnectExecutor.schedule(new ReconnectProviderTask(), 5, TimeUnit.MINUTES);
        jtapiPeer = JtapiPeerFactory.getJtapiPeer(null);
    }
    
    public ProviderController getProviderController(String phoneNumber) 
            throws ProviderRegistryException 
    {
        Integer number = new Integer(phoneNumber);
        Map.Entry<Integer, ProviderController> entry = controllers.floorEntry(number);
        
        if (entry==null || number > entry.getValue().getToNumber())
            throw new ProviderRegistryException(String.format(
                    "Provider not found for number (%d)", number));
        
        if (entry.getValue().getState().getId() != ProviderControllerState.IN_SERVICE)
            throw new ProviderRegistryException(String.format(
                    "Provider (%s) for number (%d) is not available now"
                    , entry.getValue(), number));
        
        return entry.getValue();
    }

    public Collection<ProviderController> getProviderControllers() {
        return controllers.values();
    }

    public void shutdown() {
        for (ProviderController controller: controllers.values())
            controller.shutdown();
    }

    public void providerAdded(ProviderConfiguration providerConfiguration) {
        addProviderController(providerConfiguration);
        log.info(String.format(
                "New provider controller added. %s (%d)"
                , providerConfiguration.getName(), providerConfiguration.getId()));
    }

    public void providerRemoved(ProviderConfiguration providerConfiguration) {
        removeProviderController(providerConfiguration);
        log.info(String.format(
                "Provider controller removed. %s (%d)"
                , providerConfiguration.getName(), providerConfiguration.getId()));
    }

    public void providerUpdated(ProviderConfiguration providerConfiguration) {
        removeProviderController(providerConfiguration);
        addProviderController(providerConfiguration);
        removeProviderController(providerConfiguration);
        log.info(String.format(
                "Provider controller updated. %s (%d)"
                , providerConfiguration.getName(), providerConfiguration.getId()));
    }

    private void addProviderController(ProviderConfiguration conf)
    {
        ProviderControllerImpl controller = new ProviderControllerImpl(
                stateListenersCoordinator
                , conf.getId(), conf.getName(), conf.getFromNumber(), conf.getToNumber()
                , conf.getUser(), conf.getPassword(), conf.getHost());
        controller.setExecutor(connectExecutor);
        controller.setJtapiPeer(jtapiPeer);
        controller.connect();
        controllers.put(conf.getFromNumber(), controller);
    }

    private void removeProviderController(ProviderConfiguration providerConfiguration) {
        ProviderController controller = controllers.remove(providerConfiguration.getFromNumber());
        if (controller!=null)
            controller.shutdown();
    }

    public synchronized boolean isStoped() {
        return stoped;
    }

    public synchronized void setStoped(boolean stoped) {
        this.stoped = stoped;
    }
    
    private class ReconnectProviderTask implements Runnable {        
        public void run() {
            try {
                for (ProviderController controller: controllers.values()) {
                    int id = controller.getState().getId();
                    if (id==ProviderControllerState.OUT_OF_SERVICE || id==ProviderControllerState.STOPED) {
                        log.info("Reinitializing provider controller. "+controller.getName());
                        controller.connect();
                    }
                }
            } finally {
                reconnectExecutor.schedule(this, 1, TimeUnit.MINUTES);
            }
        }
    }

    public void registryDidShutdown() {
        shutdown();
    }
    

}
