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

package org.onesec.core;

import java.util.ArrayList;
import java.util.List;
import org.onesec.core.impl.StateListenerConfigurationImpl;
import org.onesec.core.impl.StateListenersCoordinatorImpl;
import org.onesec.core.services.StateListenersCoordinator;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
@Test
public class StateListenerCoordinatorTest extends Assert {
    private State state1;
    private State state2;
    
    private StateListener listenerForAllStates;
    private StateListener listenerForState2;
    
    public void test() {
        createAndTrainMocks();
        
        List<StateListenerConfiguration> configurations = 
                new ArrayList<StateListenerConfiguration>();
        configurations.add(new StateListenerConfigurationImpl(listenerForAllStates, null));
        configurations.add(new StateListenerConfigurationImpl(
                listenerForState2, new Class[]{state2.getClass()}));
        
        StateListenersCoordinator coordinator = new StateListenersCoordinatorImpl(configurations);
        
        coordinator.addListenersToState(state1);
        coordinator.addListenersToState(state2);
        
        verify(state1, state2, listenerForAllStates, listenerForState2);
    }
    
    private void createAndTrainMocks() {
        state1 = createMock("State1", State.class);
        state2 = createMock("State2", State2.class);
        
        listenerForAllStates = createMock("Listener1", StateListener.class);
        listenerForState2 = createMock("Listener2", StateListener.class);
        
        state1.addStateListener(listenerForAllStates);
        
        state2.addStateListener(listenerForAllStates);
        state2.addStateListener(listenerForState2);
        
        replay(state1, state2, listenerForAllStates, listenerForState2);
    }
    
    private interface State2 extends State {}

}
