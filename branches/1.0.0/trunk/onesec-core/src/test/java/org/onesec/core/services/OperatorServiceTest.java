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

import java.util.Properties;
import org.onesec.core.StateWaitResult;
import org.onesec.core.call.CallState;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
/**
 *
 * @author Mikhail Titov
 */
public class OperatorServiceTest extends ServiceTestCase
{
    @Test
    public void callTest() throws Exception
    {
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);
        
        StateWaitResult result = configurator.getState().waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 500l);
        
        assertFalse(result.isWaitInterrupted());
        
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry.getProviderControllers());
        assertEquals(providerRegistry.getProviderControllers().size(), 1);
        
        ProviderController providerController = 
                providerRegistry.getProviderControllers().iterator().next();
        
        result = providerController.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 60000L);
        
        assertFalse(result.isWaitInterrupted());
        
        Operator operator = registry.getService(Operator.class);
        
        Properties props = getProperties("numbers_OperatorServiceTest.txt");
        String numA = props.getProperty("numA");
        assertNotNull(numA);
        String BNumbers = props.getProperty("numB");
        assertNotNull(BNumbers);
        String[] numBList = BNumbers.split("\\s+");
        
        for (String numB: numBList ) {
            CallState callState = operator.call(numA, numB);
            result = callState.waitForState(new int[]{CallState.FINISHED}, 120000L);
            assertFalse(result.isWaitInterrupted());
        }
    }

    @Test
    public void isOperatorNumberTest() throws Exception
    {
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);

        StateWaitResult result = configurator.getState().waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 500l);

        assertFalse(result.isWaitInterrupted());

        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry.getProviderControllers());
        assertEquals(providerRegistry.getProviderControllers().size(), 1);

        ProviderController providerController =
                providerRegistry.getProviderControllers().iterator().next();

        result = providerController.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 60000L);

        assertFalse(result.isWaitInterrupted());

        Operator operator = registry.getService(Operator.class);

        Properties props = getProperties("numbers_OperatorServiceTest.txt");
        String numA = props.getProperty("numA");
        String noneOperatorNum = props.getProperty("noneOperatorNum");
        
        assertTrue(operator.isOperatorNumber(numA));
        assertFalse(operator.isOperatorNumber(noneOperatorNum));
    }
    
    @Override
    protected String[][] initConfigurationFiles() {
        return new String[][]{
            {"providers_OperatorServiceTest.cfg", "providers.cfg"}
        };
    }
    
}
