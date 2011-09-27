/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import org.junit.Test;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrTerminal;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class TerminalStateMonitoringServiceImplTest extends OnesecRavenTestCase
{
    @Test
    public void startTerminalTest()
    {
        IvrTerminal term = createMock(IvrTerminal.class);
        ProviderControllerState state = createMock(ProviderControllerState.class);
        ProviderController controller = createMock(ProviderController.class);

        replay(term, state, controller);

        

        verify(term, state, controller);
    }

    @Test
    public void restartTerminalTest(){

    }

    @Test
    public void stopStartedTerminalTest(){
        
    }

    @Test
    public void stopStoppedTerminalTest(){

    }

    @Test
    public void filterTest()
    {
        
    }

    @Test
    public void serviceTest(){
        
    }

    @Test
    public void stateListenerCoordinatorTest()
    {
        
    }
}