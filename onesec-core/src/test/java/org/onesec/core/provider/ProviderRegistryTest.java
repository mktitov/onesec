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

package org.onesec.core.provider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.telephony.JtapiPeerUnavailableException;
import org.apache.commons.io.FileUtils;
import org.onesec.core.StateListenerConfiguration;
import org.onesec.core.StateWaitResult;
import org.onesec.core.impl.ProviderConfiguratorListenersImpl;
import org.onesec.core.impl.StateListenerConfigurationImpl;
import org.onesec.core.impl.StateListenersCoordinatorImpl;
import org.onesec.core.impl.StateLogger;
import org.onesec.core.provider.impl.FileProviderConfigurator;
import org.onesec.core.provider.impl.ProviderRegistryImpl;
import org.onesec.core.services.ProviderConfigurator;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author Mikhail Titov
 */
@Test
public class ProviderRegistryTest extends Assert
{
    @Test(timeOut=180000)
    public void test()
        throws InterruptedException, IOException, ProviderRegistryException
            , JtapiPeerUnavailableException
    {
        
        List<StateListenerConfiguration> listenersConfig = 
                new ArrayList<StateListenerConfiguration>();
        listenersConfig.add(new StateListenerConfigurationImpl(new StateLogger(), null));
        
        StateListenersCoordinator listenersCoordinator = 
                new StateListenersCoordinatorImpl(listenersConfig);
        
        final ProviderRegistry registry = new ProviderRegistryImpl(
                listenersCoordinator, LoggerFactory.getLogger(ProviderRegistry.class));
        
        final ProviderConfigurator configurator = new FileProviderConfigurator(
                new File(System.getProperty("user.home")+"/.onesec")
                , new ProviderConfiguratorListenersImpl(
                    Arrays.asList((ProviderConfiguratorListener)registry))
                , LoggerFactory.getLogger(ProviderConfigurator.class));
        
        ProviderConfiguratorState state = configurator.getState();
        StateWaitResult res = state.waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 1000L);
        
        assertFalse(res.isWaitInterrupted());
        
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            public void run() {
                try{
                    boolean allControllersInService = false;
                    while (!allControllersInService) {
                        allControllersInService = true;
                        for (ProviderController controller: registry.getProviderControllers())
                            if (controller.getState().getId() != ProviderControllerState.IN_SERVICE){
                                allControllersInService = false;
                                break;
                            }
                        TimeUnit.SECONDS.sleep(1);
                    }
                }catch(InterruptedException e){
                    
                }
            }
        });
        
        executor.shutdown();
        executor.awaitTermination(175, TimeUnit.SECONDS);
        
        List<String> numbers = FileUtils.readLines(
                new File(System.getProperty("user.home")+"/"+"/.onesec/numbers.txt"));
        
        for (String number: numbers) {
            ProviderController controller = registry.getProviderController(number);
            assertNotNull(controller);
            int intNumber = Integer.valueOf(number);
            assertTrue(intNumber>=controller.getFromNumber());
            assertTrue(intNumber<=controller.getToNumber());
        }
                
        registry.shutdown();
    }

}
