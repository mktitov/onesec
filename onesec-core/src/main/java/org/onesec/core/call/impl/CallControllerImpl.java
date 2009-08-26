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

package org.onesec.core.call.impl;

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.CallObserver;
import javax.telephony.Connection;
import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnTalkingEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnEv;
import javax.telephony.events.TermEv;
import org.onesec.core.State;
import org.onesec.core.StateListener;
import org.onesec.core.StateWaitResult;
import org.onesec.core.call.CallController;
import org.onesec.core.call.CallState;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;

/**
 *
 * @author Mikhail Titov
 */
public class CallControllerImpl 
        implements CallController, AddressObserver, TerminalObserver, CallObserver, StateListener
            , CallControlCallObserver
{
    public static final int DEFAULT_PREPARING_TIMEOUT = 10000;
    
    private ProviderController providerController;
    private final CallStateImpl state;
    
    private Terminal sourceTerminal;
    private Address sourceAddress;
    private final String numA;
    private final String numB;
    private Call call = null;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CallControllerImpl(
            ProviderRegistry providerRegistry, StateListenersCoordinator listenersCoordinator
            , String numA, String numB) 
    {
        this.numA = numA;
        this.numB = numB;

        state = new CallStateImpl(this);
        state.addStateListener(this);
        listenersCoordinator.addListenersToState(state);        
        state.setState(CallState.PREPARING);

        try {
            providerController = providerRegistry.getProviderController(numA);
            sourceAddress = providerController.getProvider().getAddress(numA);
            Terminal[] terminals = sourceAddress.getTerminals();
            if (terminals==null)
                throw new Exception("No terminals found for address");
            sourceTerminal = terminals[0];
            
            sourceAddress.addObserver(this);
            sourceTerminal.addObserver(this);
            sourceAddress.addCallObserver(this);
            
            executor.execute(new Runnable() {
                public void run() {
                    call();
                }
            });
            
        } catch (Exception ex) {
            state.setState(CallState.FINISHED, ex.getMessage(), ex);
        }
    }
    
    private void call(){
        StateWaitResult res = state.waitForState(
                new int[]{CallState.PREPARED}, DEFAULT_PREPARING_TIMEOUT);
        if (!res.isWaitInterrupted()){
            try{
                call = providerController.getProvider().createCall();
                ((CallControlCall)call).setConferenceEnable(true);
                call.connect(sourceTerminal, sourceAddress, numB);
                state.setState(CallState.CALLING);
            }catch(Exception e){
                state.setState(CallState.FINISHED, e.getMessage(), e);
            }
        }else if (res.getState().getId()!=CallState.FINISHED){
            state.setState(CallState.FINISHED, "Initialization timeout", null);
        }
            
    }
    
    public CallState getState(){
        return state;
    }
    
    public Call getCall() {
        return call;
    }

    public String getObjectName() {
        return "Call";
    }

    public String getObjectDescription() {
        return (call!=null? ((CiscoCall)call).getCallID() : "")+" ("+numA+"->"+numB+")";
    }

    public void addressChangedEvent(AddrEv[] events) {
        for (AddrEv event: events)
            switch (event.getID()){
                case CiscoAddrInServiceEv.ID: state.setAddressReady(true); break;
            }
    }

    public void terminalChangedEvent(TermEv[] events) {
        for (TermEv event: events)
            switch (event.getID()){
                case CiscoTermInServiceEv.ID: state.setTerminalReady(true); break;
            }
    }

    public void callChangedEvent(CallEv[] events) {
        for (CallEv event: events){
//            LoggerFactory.getLogger(CallController.class).debug(event.toString());
            switch(event.getID()){
                case CallCtlConnEstablishedEv.ID: 
                {
                    String address = ((ConnEv)event).getConnection().getAddress().getName();
                    if (numA.equals(address))
                        state.setSideAConnected(true);
                    else
                        state.setSideBConnected(true);
                }
                case CallCtlTermConnTalkingEv.ID:
                {
                    TerminalConnection tc = 
                            ((CallCtlTermConnTalkingEv)event).getTerminalConnection();
                    if (tc.getConnection().getAddress().getName().equals(numB)){
                        try{
                            tc.getConnection().disconnect();
                        }catch(Exception e){
                            state.setState(
                                    CallState.FINISHED
                                    , "Error while diconnection counter part connection"
                                    , e);
                        }
                    }
                    break;
                }
                case CallCtlConnDisconnectedEv.ID:
                {
                    Connection conn = ((CallCtlConnDisconnectedEv)event).getConnection();
                    if (conn.getAddress().equals(sourceAddress))
                        state.setState(CallState.FINISHED);
                }
            }
        }
    }

    public void stateChanged(State state) {
        if (state.getId()==CallState.FINISHED) {
            if (sourceAddress!=null){
                sourceAddress.removeObserver(this);
                sourceAddress.removeCallObserver(this);
            }
            if (sourceTerminal!=null)
                sourceTerminal.removeObserver(this);
        }
    }

}
