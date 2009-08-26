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
import java.util.Arrays;
import javax.telephony.Provider;
import org.onesec.core.StateWaitResult;
import org.onesec.core.impl.ProviderConfiguratorListenersImpl;
import org.onesec.core.provider.impl.FileProviderConfigurator;
import org.onesec.core.provider.impl.ProviderControllerImpl;
import org.onesec.core.provider.impl.ProviderControllerStateImpl;
import org.onesec.core.services.ProviderConfigurator;
import org.onesec.core.services.StateListenersCoordinator;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import static org.onesec.core.provider.ProviderControllerState.*;
import static org.testng.Assert.*;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ProviderControllerTest implements ProviderConfiguratorListener {
    String host; 
    
    private ProviderController controller = null;
    private StateListenersCoordinator stateListenersCoordinator;
    
    @Test
    public void test_connection() {
        try{
            configProviderController();
            
            ProviderControllerState state = controller.connect();

            StateWaitResult result = state.waitForState(new int[]{IN_SERVICE, STOPED}, 40000);

            assertFalse(result.isWaitInterrupted());
            assertEquals(result.getState().getId(), IN_SERVICE);

            Provider prov = controller.getProvider();
            assertNotNull(prov);
            
            verify(stateListenersCoordinator);
        }finally{
            if (controller!=null)
                controller.shutdown();
        }
    }
    
    private void configProviderController()
    {
        stateListenersCoordinator = createMock(StateListenersCoordinator.class);
        stateListenersCoordinator.addListenersToState(isA(ProviderControllerStateImpl.class));
        replay(stateListenersCoordinator);
                
        ProviderConfigurator configurator = new FileProviderConfigurator(
                new File(System.getProperty("user.home")+"/.onesec")
                , new ProviderConfiguratorListenersImpl(
                    Arrays.asList((ProviderConfiguratorListener)this))
                , LoggerFactory.getLogger(ProviderConfigurator.class));
        
        ProviderConfiguratorState state = configurator.getState();
        
        state.waitForState(new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 500L);
        
        assertNotNull(controller);
    }

    public void providerAdded(ProviderConfiguration conf) {
        if (controller==null) {
            controller = new ProviderControllerImpl(
                    stateListenersCoordinator
                    , conf.getId(), conf.getName(), conf.getFromNumber(), conf.getToNumber()
                    , conf.getUser(), conf.getPassword(), conf.getHost());
        }
    }

    public void providerRemoved(ProviderConfiguration providerConfiguration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void providerUpdated(ProviderConfiguration providerConfiguration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
