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

import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoTerminal;
import java.util.concurrent.TimeUnit;
import javax.telephony.Call;
import javax.telephony.CallObserver;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlTerminalConnection;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermEv;
import org.onesec.core.StateWaitResult;
import org.onesec.core.call.CallState;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
/**
 *
 * @author Mikhail Titov
 */
@Test
public class ConferenceTest extends ServiceTestCase implements CallObserver, TerminalObserver
{
    
    @Test(enabled=false)
    public void test() throws Exception {
        StateWaitResult result;
        
        Operator operator = registry.getService(Operator.class);
        
        CallState state = operator.call("88025", "88024");
        
        result = state.waitForState(new int[]{CallState.TALKING}, 20000L);
        
        assertFalse(result.isWaitInterrupted());
        
        Call call = state.getObservableObject().getCall();
        
        CallControlCall callControl = (CallControlCall) call;
        
        CallControlTerminalConnection connection = (CallControlTerminalConnection) 
                callControl.getCallingTerminal().getTerminalConnections()[0];
        
        connection.hold();
        
        state = operator.call("88025", "089128672947");
        result = state.waitForState(new int[]{CallState.TALKING}, 20000L);
        CallControlCall call2 = (CallControlCall) state.getObservableObject().getCall();
        CallControlTerminalConnection connection2 = (CallControlTerminalConnection) 
                call2.getCallingTerminal().getTerminalConnections()[1];
        
        connection2.hold();

        state = operator.call("88025", "88027");
        CallControlCall call3 = (CallControlCall) state.getObservableObject().getCall();
        result = state.waitForState(new int[]{CallState.TALKING}, 20000L);
        CallControlTerminalConnection connection3 = (CallControlTerminalConnection) 
                call2.getCallingTerminal().getTerminalConnections()[2];
        
        connection3.hold();
        
        ((CiscoCall)call).conference(new Call[]{call2, state.getObservableObject().getCall()}); 
//        callControl.conference(state.getObservableObject().getCall());
        
//        callControl = (CallControlCall) state.getObservableObject().getCall();
//        connection = (CallControlTerminalConnection)
//                callControl.getCallingTerminal().getTerminalConnections()[0];
//        connection.hold();
//        

//        state = operator.call("88025", "88027");
//        result = state.waitForState(new int[]{CallState.TALKING}, 20000L);
        
//        callControl.setConferenceEnable(true);
        
//        callControl.conference(state.getObservableObject().getCall());
        
        
    }
    
    @Test(enabled=false)
    public void test2() throws Exception {
        connect();
        
        conference2("88025", new String[]{"88024", "089128672947", "88027", "88026", "089128650010"});
    }
    
    public void test_conference() throws Exception 
    {
        connect();
        
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        Provider provider = providerRegistry.getProviderController("88024").getProvider();
        CiscoTerminal terminal = (CiscoTerminal) provider.getAddress("88024").getTerminals()[0];
        terminal.addObserver(this);
        
        while (terminal.getState() != CiscoTerminal.IN_SERVICE)
            TimeUnit.MILLISECONDS.sleep(10);
        
        terminal.sendData(
                        "<CiscoIPPhoneMenu><Title>Conference</Title><MenuItem>" +
                        "<Name>Conference</Name>" +
                        "<URL>Dial:089128672947</URL></MenuItem></CiscoIPPhoneMenu>");
    }
    
    private void conference(String mainNumber, String[] numbers) throws Exception {
        Operator operator = registry.getService(Operator.class);
        Call firstCall = null;
        Call[] calls = new Call[numbers.length-1];
        for (int i=0; i<numbers.length; ++i) {
            CallState state = operator.call(mainNumber, numbers[i]);
            StateWaitResult result = state.waitForState(new int[]{CallState.TALKING}, 20000L);
            
            assertFalse(result.isWaitInterrupted());
            
            CallControlCall call = (CallControlCall) state.getObservableObject().getCall();
            if (i==0)
                firstCall = call;
            else
                calls[i-1]=call;
            
            ((CallControlTerminalConnection)call.getCallingTerminal().getTerminalConnections()[i])
                    .hold();            
            if (i>0)
                ((CiscoCall)firstCall).conference(call);
        }
        
//        ((CiscoCall)firstCall).conference(calls);
    }
    
    private void conference2(String mainNumber, String[] numbers) throws Exception {
        Operator operator = registry.getService(Operator.class);
        CallControlCall firstCall = null;
        CallControlTerminalConnection connection = null;
        Terminal primaryTerminal = null;
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        Provider provider = providerRegistry.getProviderController(mainNumber).getProvider();
        for (int i=0; i<numbers.length; ++i) {
            if (i==0) {
                CallState state = operator.call(mainNumber, numbers[i]);
                StateWaitResult result = state.waitForState(new int[]{CallState.TALKING}, 20000L);

                assertFalse(result.isWaitInterrupted());

                CallControlCall call = (CallControlCall) state.getObservableObject().getCall();
                firstCall = call;
                connection = (CallControlTerminalConnection) 
                        call.getCallingTerminal().getTerminalConnections()[0];
                primaryTerminal = firstCall.getCallingTerminal();
//                call.setConferenceEnable(true);
            } else {
                ((CiscoTerminal)primaryTerminal).sendData(
                        "<CiscoIPPhoneMenu><Title>Conference</Title><MenuItem>" +
                        "<Name>Conference</Name>" +
                        "<URL>Dial:089128672947</URL></MenuItem></CiscoIPPhoneMenu>");
//                CallControlCall call = (CallControlCall)provider.createCall();
//                call.addObserver(this);
//                while (call.getState()!=Call.IDLE)
//                    TimeUnit.MILLISECONDS.sleep(10);
//                call.consult(connection, numbers[i]);
//                TimeUnit.SECONDS.sleep(10);
//                
//                firstCall.conference(call);
            }
                        
        }
        
//        ((CiscoCall)firstCall).conference(calls);
    }
    
    @Override
    protected String[][] initConfigurationFiles() {
        return new String[][]{
            {"providers_OperatorServiceTest.cfg", "providers.cfg"}
        };
    }

    private void connect() {
        ProviderConfigurator configurator = registry.getService(ProviderConfigurator.class);

        StateWaitResult result = configurator.getState().waitForState(new int[]{ProviderConfiguratorState.CONFIGURATION_UPDATED}, 500l);

        assertFalse(result.isWaitInterrupted());

        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry.getProviderControllers());
        assertEquals(providerRegistry.getProviderControllers().size(), 1);

        ProviderController providerController = providerRegistry.getProviderControllers().iterator().next();

        result = providerController.getState().waitForState(new int[]{ProviderControllerState.IN_SERVICE}, 60000L);

        assertFalse(result.isWaitInterrupted());

    }

    public void callChangedEvent(CallEv[] arg0) {
    }

    public void terminalChangedEvent(TermEv[] arg0) {
    }

}
