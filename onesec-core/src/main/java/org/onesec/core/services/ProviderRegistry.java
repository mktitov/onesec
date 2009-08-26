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

package org.onesec.core.services;

import java.util.Collection;
import org.onesec.core.ShutdownableService;
import org.onesec.core.provider.ProviderRegistryException;
import org.onesec.core.provider.ProviderConfiguratorListener;
import org.onesec.core.provider.ProviderController;

/**
 * Предоставляет доступ к <code>javax.telephony.Provider</code>.
 * @author Mikhail Titov
 */
public interface ProviderRegistry extends ProviderConfiguratorListener, ShutdownableService {
    /**
     * Возвращает провайдер для заданного номера.
     * 
     * @param phoneNumber
     * @return
     * @throws org.onesec.core.ProviderNotFoundException если провайдер для заданного номера 
     *      не найден
     */
    public ProviderController getProviderController(String phoneNumber) 
            throws ProviderRegistryException;
    
    public Collection<ProviderController> getProviderControllers();
    
}
