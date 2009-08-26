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

package org.onesec.core.impl;

import javax.telephony.Address;
import javax.telephony.InvalidArgumentException;
import javax.telephony.Provider;
import org.onesec.core.call.CallController;
import org.onesec.core.call.CallState;
import org.onesec.core.call.impl.CallControllerImpl;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderRegistryException;
import org.onesec.core.services.Operator;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;

/**
 *
 * @author Mikhail Titov
 */
public class OperatorImpl implements Operator
{
    private final ProviderRegistry providerRegistry;
    private final StateListenersCoordinator listenersCoordinator;

    public OperatorImpl(
            ProviderRegistry providerRegistry, StateListenersCoordinator listenersCoordinator) 
    {
        this.providerRegistry = providerRegistry;
        this.listenersCoordinator = listenersCoordinator;
    }

    public CallState call(String numA, String numB) {
        CallController callController = 
                new CallControllerImpl(providerRegistry, listenersCoordinator, numA, numB);
        return callController.getState();
    }

    public boolean isOperatorNumber(String num)
    {
        try
        {
            ProviderController controller = providerRegistry.getProviderController(num);
            Provider provider = controller.getProvider();
            if (provider!=null)
            {
                try
                {
                    Address address = provider.getAddress(num);
                    return true;
                } catch (InvalidArgumentException ex)
                {
                }
            }
        }
        catch (ProviderRegistryException ex)
        {
        }
        return false;
    }
}
