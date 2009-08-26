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

package org.onesec.server.services;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.mortbay.jetty.Response;
import org.onesec.core.StateWaitResult;
import org.onesec.core.call.CallState;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderConfigurator;
import org.onesec.core.services.ProviderRegistry;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
@Test
public class JettyServerServiceTest extends ServiceTestCase {
    
    @Test(timeOut=60000L)
    public void test() throws Exception {
        Properties props = getProperties("numbers_JettyServerServiceTest.txt");
        String numA = props.getProperty("numA");
        assertNotNull(numA);
        String numB = props.getProperty("numB");
        assertNotNull(numB);
        
        JettyServer server = registry.getService(JettyServer.class);
        assertNotNull(server);
        
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);
        ProviderConfiguratorState configuratorState = configurator.getState();
        
        StateWaitResult result = configuratorState.waitForState(
                new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 2000L);
        
        assertFalse(result.isWaitInterrupted());
        
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        ProviderController controller = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(controller);
        
        ProviderControllerState state = controller.getState();
        result = state.waitForState(new int[]{ProviderControllerState.IN_SERVICE}, 40000L);
        
        assertFalse(result.isWaitInterrupted());
        
        String url = String.format(
                "http://localhost:8080/dialer/make-call?num_a=%s&num_b=%s", numA, numB);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        assertEquals(connection.getResponseCode(), Response.SC_OK);
        
        while(JettyServerTestModule.getCallState()==null)
            TimeUnit.MILLISECONDS.sleep(10);
        
        CallState callState = JettyServerTestModule.getCallState();
        
        result = callState.waitForState(new int[]{CallState.FINISHED}, 30000L);
        
        assertFalse(result.isWaitInterrupted());
        
        verify(JettyServerTestModule.callListener);
    }

    @Override
    protected String[][] initConfigurationFiles() {
        return new String[][]{
            {"providers_OperatorServiceTest.cfg", "providers.cfg"},
            {"onesec_withNoHostRestriction.cfg", "onesec.cfg"}
        };
    }
    
}
