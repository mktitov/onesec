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

package org.onesec.core.impl;

import java.util.Collection;
import org.onesec.core.provider.ProviderConfiguratorListener;
import org.onesec.core.services.ProviderConfiguratorListeners;

/**
 *
 * @author Mikhail Titov
 */
public class ProviderConfiguratorListenersImpl implements ProviderConfiguratorListeners
{
    private final Collection<ProviderConfiguratorListener> listeners;

    public ProviderConfiguratorListenersImpl(Collection<ProviderConfiguratorListener> listeners)
    {
        this.listeners = listeners;
    }

    public Collection<ProviderConfiguratorListener> getListeners()
    {
        return listeners;
    }
}
