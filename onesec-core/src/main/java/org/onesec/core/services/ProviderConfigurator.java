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

import java.util.Collection;
import org.onesec.core.ObjectDescription;
import org.onesec.core.ShutdownableService;
import org.onesec.core.provider.*;
import org.onesec.core.provider.ProviderConfiguration;

/**
 *
 * @author Mikhail Titov
 */
public interface ProviderConfigurator extends ObjectDescription, ShutdownableService {
    /**
     * Возвращает состояние конфигуратора
     * @return
     */
    public ProviderConfiguratorState getState();
    /**
     * Возвращает список провайдеров, имеющихся в конфигураторе
     * @return
     */
    public Collection<ProviderConfiguration> getAll();
    /**
     * Добавляет информацию о провайдере
     * @param configuration
     */
    public void add(ProviderConfiguration configuration);
    /**
     * Удаляет информацию о провайдере
     * @param configuration
     */
    public void remove(ProviderConfiguration configuration);
    /**
     * Обновляет информацию о провайдере
     * @param configuration
     */
    public void update(ProviderConfiguration configuration);
    
    public void start();
    
}
