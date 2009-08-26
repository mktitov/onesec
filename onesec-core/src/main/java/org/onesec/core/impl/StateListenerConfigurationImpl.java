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

import org.onesec.core.StateListener;
import org.onesec.core.StateListenerConfiguration;

/**
 *
 * @author Mikhail Titov
 */
public class StateListenerConfigurationImpl implements StateListenerConfiguration {
    private StateListener stateListener;
    private Class[] stateTypes;

    public StateListenerConfigurationImpl(StateListener listener, Class[] stateTypes) {
        this.stateListener = listener;
        this.stateTypes = stateTypes;
    }

    public StateListener getStateListener() {
        return stateListener;
    }

    public Class[] getStateTypes() {
        return stateTypes;
    }

}
