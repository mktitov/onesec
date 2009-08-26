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

import org.onesec.core.services.ServiceTestCase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
/**
 *
 * @author Mikhail Titov
 */
public class ProviderRegistryServiceTest extends ServiceTestCase {
    
    @Test
    public void test() throws InterruptedException {
        
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);
        assertNotNull(configurator);
        //configurator.start();
        configurator.getState().waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 500l);
        
        final ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(registry);
        
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            public void run() {
                try{
                    boolean allControllersInService = false;
                    while (!allControllersInService) {
                        allControllersInService = true;
                        for (ProviderController controller: providerRegistry.getProviderControllers())
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
        
    }

    @Override
    protected String[][] initConfigurationFiles() {
        return new String[][]{
            {"providers_ProviderRegistryServiceTest.cfg", "providers.cfg"}
        };
    }

}
