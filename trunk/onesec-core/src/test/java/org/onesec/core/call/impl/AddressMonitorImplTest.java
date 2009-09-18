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

package org.onesec.core.call.impl;

import java.util.Date;
import org.onesec.core.StateWaitResult;
import org.onesec.core.call.AddressMonitor;
import org.onesec.core.call.CallResult;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderConfigurator;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.ServiceTestCase;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class AddressMonitorImplTest extends ServiceTestCase
{
    @Test()
    public void inServiceTest() throws Exception
    {
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);

        StateWaitResult result = configurator.getState().waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 1000l);

        assertFalse(result.isWaitInterrupted());

        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry.getProviderControllers());
        assertEquals(providerRegistry.getProviderControllers().size(), 1);

        ProviderController providerController =
                providerRegistry.getProviderControllers().iterator().next();

        result = providerController.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 120000L);

        assertFalse(result.isWaitInterrupted());

        AddressMonitor monitor = new AddressMonitorImpl(providerRegistry, "88024");
        System.out.println("WAITING For in service");
        assertTrue(monitor.waitForInService(20000));
//        assertTrue(monitor.waitForCallWait(60000));
        CallResult callRes = monitor.waitForCallCompletion("089128672947", 10000, 60000);
        System.out.println("\n-------------CALL RESULT--------------");
        System.out.println("completion code: "+callRes.getCompletionCode());
        System.out.println("call start time: "+new Date(callRes.getCallStartTime()));
        System.out.println("conversation start time: "+new Date(callRes.getConversationStartTime()));
        System.out.println("conversation duration: "+callRes.getConversationDuration());
        Thread.sleep(2000);
        monitor.releaseMonitor();
    }

    @Override
    protected String[][] initConfigurationFiles()
    {
        return new String[][]{
            {"providers_CtiUser1.cfg", "providers.cfg"}
        };
    }
}