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

package org.onesec.core.provider.impl;

import org.onesec.core.impl.BaseState;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.services.ProviderConfigurator;

/**
 *
 * @author Mikhail Titov
 */
public class ProviderConfiguratorStateImpl 
        extends BaseState<ProviderConfiguratorState, ProviderConfigurator> 
        implements ProviderConfiguratorState 
{

    public ProviderConfiguratorStateImpl(ProviderConfigurator observableObject) {
        super(observableObject);
        addIdName(CONFIGURATION_UPDATED, "CONFIGURATION_UPDATED");
        addIdName(CONFIGURATION_UPDATING, "CONFIGURATION_UPDATING");
        addIdName(ERROR, "ERROR");
        addIdName(READY, "READY");
        addIdName(STOPED, "STOPED");
    }

}
