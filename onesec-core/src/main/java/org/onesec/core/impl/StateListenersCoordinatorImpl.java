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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onesec.core.State;
import org.onesec.core.StateListener;
import org.onesec.core.StateListenerConfiguration;
import org.onesec.core.services.StateListenersCoordinator;

/**
 *
 * @author Mikhail Titov
 */
public class StateListenersCoordinatorImpl implements StateListenersCoordinator {
    
    private final Map<Class, List<StateListener>> configurations;

    public StateListenersCoordinatorImpl(
            Collection<StateListenerConfiguration> listOfconfigurations) 
    {
        configurations = new HashMap<Class, List<StateListener>>();
        for (StateListenerConfiguration config: listOfconfigurations) {
            Class[] stateTypes = config.getStateTypes();
            if (stateTypes!=null)
                for (Class stateType: stateTypes)
                    getListForStateType(stateType).add(config.getStateListener());
            else
                getListForStateType(null).add(config.getStateListener());
        }
    }
    
    private List<StateListener> getListForStateType(Class stateType) {
        List<StateListener> list = configurations.get(stateType);
        if (list==null){
            list = new ArrayList<StateListener>();
            configurations.put(stateType, list);
        }
        return list;
    }

    public void addListenersToState(State state) {
        for (Class stateClass: new Class[]{null, state.getClass()}) {
            List<StateListener> listeners = configurations.get(stateClass);
            if (listeners!=null)
                for (StateListener listener: listeners)
                    state.addStateListener(listener);
        }
    }
}
