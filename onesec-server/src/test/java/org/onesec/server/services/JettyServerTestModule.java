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

import org.apache.tapestry5.ioc.Configuration;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.onesec.core.State;
import org.onesec.core.StateListener;
import org.onesec.core.StateListenerConfiguration;
import org.onesec.core.call.CallState;
import org.onesec.core.call.impl.CallStateImpl;
import org.onesec.core.impl.StateListenerConfigurationImpl;

/**
 *
 * @author Mikhail Titov
 */
public final class JettyServerTestModule extends EasyMock {
    public static StateListener callListener;
    public static CallState callState;
    
    public static void contributeStateListenersCoordinator(
            Configuration<StateListenerConfiguration> config)
    {
        callListener = createMock(StateListener.class);
        callListener.stateChanged(checkState(CallState.PREPARING));
        callListener.stateChanged(checkState(CallState.PREPARED));
        callListener.stateChanged(checkState(CallState.CALLING));
        callListener.stateChanged(checkState(CallState.TALKING));
        callListener.stateChanged(checkState(CallState.FINISHED));
        
        replay(callListener);
        
        config.add(new StateListenerConfigurationImpl(
                callListener, new Class[]{CallStateImpl.class}));
    }
    
    private static State checkState(final int stateId)
    {
        reportMatcher(new IArgumentMatcher() 
        {
            public boolean matches(Object obj) {
                State state = (State) obj;
                synchronized(JettyServerTestModule.class){
                    if (callState==null)
                        callState = (CallState) state;
                }
                return state.getId()==stateId;
            }
            public void appendTo(StringBuffer arg0) {
            }
        });
        return null;
    }
    
    public static synchronized CallState getCallState(){
        return callState;
    }
}
