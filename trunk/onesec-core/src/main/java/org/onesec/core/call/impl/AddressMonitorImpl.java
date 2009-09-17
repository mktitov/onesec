/*
 *  Copyright 2009 Mikhail Titov.
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
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.CallObserver;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermEv;
import org.onesec.core.call.AddressMonitor;
import org.onesec.core.impl.OnesecUtils;
import org.onesec.core.services.ProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class AddressMonitorImpl
        implements AddressMonitor, TerminalObserver, AddressObserver, CallObserver
            , CallControlCallObserver
{
    private static Logger logger = LoggerFactory.getLogger(AddressMonitor.class);

    private final String address;
    private Terminal terminal;
    private Address terminalAddress;

    private AtomicBoolean terminalReady;
    private AtomicBoolean addressReady;
    private boolean inService;
    private boolean waitingForCall;
    private Lock lock;
    private Condition inServiceCondition;
    private Condition waitingForCallCondition;

    public AddressMonitorImpl(ProviderRegistry providerRegistry, String address)
            throws Exception
    {
        this.address = address;
        terminalReady = new AtomicBoolean(false);
        addressReady = new AtomicBoolean(false);
        inService = false;
        waitingForCall = false;

        Provider provider = providerRegistry.getProviderController(address).getProvider();
        terminalAddress = provider.getAddress(address);
        Terminal[] terminals = terminalAddress.getTerminals();
        if (terminals==null)
            throw new Exception("No terminals found for address ("+address+")");
        terminal = terminals[0];

        lock = new ReentrantLock();
        inServiceCondition = lock.newCondition();
        waitingForCallCondition = lock.newCondition();
        
        terminal.addObserver(this);
        terminalAddress.addObserver(this);
        terminalAddress.addCallObserver(this);

        Thread.sleep(1000);
        checkStatus();
    }

    private void checkStatus()
    {
        lock.lock();
        try
        {
            boolean newInService = false;
            if (terminalReady.get() && addressReady.get())
                newInService = true;
            if (inService!=newInService)
            {
                inService = newInService;
                if (inService)
                    inServiceCondition.signal();
            }
            boolean newWaitingForCall = false;
            if (terminalAddress.getConnections()==null)
                newWaitingForCall = true;
            if (waitingForCall!=newWaitingForCall)
            {
                waitingForCall = newWaitingForCall;
                if (logger.isDebugEnabled())
                    logger.debug(address+". WaitingForCall changed state to ("+waitingForCall+")");
                if (waitingForCall)
                    waitingForCallCondition.signal();
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    public boolean waitForInService(long timeout)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Waiting for address IN_SERVICE status");
        lock.lock();
        try
        {
            if (inService)
                return true;
            return waitForCondition(inServiceCondition, timeout);
        }
        finally
        {
            lock.unlock();
        }
    }

    public boolean waitForCallWait(long timeout)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Waiting for ready make/recieve call status");
        lock.lock();
        try
        {
            if (waitingForCall)
                return true;
            return waitForCondition(waitingForCallCondition, timeout);
        }
        finally
        {
            lock.unlock();
        }
    }

    private boolean waitForCondition(Condition condition, long timeout)
    {
        try
        {
            return condition.await(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex)
        {
            return false;
        }
    }

    public boolean waitForCallCompletion(String addess, long timeout)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void addressChangedEvent(AddrEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved address events: "+OnesecUtils.eventsToString(events));
        for (AddrEv event: events)
            switch (event.getID())
            {
                case CiscoAddrInServiceEv.ID: addressReady.set(true); checkStatus(); break;
                case CiscoAddrOutOfServiceEv.ID: addressReady.set(false); checkStatus(); break;
            }
    }

    public void terminalChangedEvent(TermEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved terminal events: "+OnesecUtils.eventsToString(events));
        for (TermEv event: events)
            switch (event.getID())
            {
                case CiscoTermInServiceEv.ID: terminalReady.set(true); checkStatus(); break;
                case CiscoTermOutOfServiceEv.ID: terminalReady.set(false); checkStatus(); break;
            }
    }

    public void callChangedEvent(CallEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved address events: "+OnesecUtils.eventsToString(events));
        for (CallEv event: events)
        {
            switch(event.getID())
            {
                case CallCtlConnDisconnectedEv.ID: checkStatus(); break;
            }
        }
//            switch(event.getID()){
//                case CallCtlConnEstablishedEv.ID:
//                {
//                    String address = ((ConnEv)event).getConnection().getAddress().getName();
//                    if (numA.equals(address))
//                        state.setSideAConnected(true);
//                    else
//                        state.setSideBConnected(true);
//                }
//                case CallCtlTermConnTalkingEv.ID:
//                {
//                    TerminalConnection tc =
//                            ((CallCtlTermConnTalkingEv)event).getTerminalConnection();
//                    if (tc.getConnection().getAddress().getName().equals(numB)){
//                        try{
//                            tc.getConnection().disconnect();
//                        }catch(Exception e){
//                            state.setState(
//                                    CallState.FINISHED
//                                    , "Error while diconnection counter part connection"
//                                    , e);
//                        }
//                    }
//                    break;
//                }
//                case CallCtlConnDisconnectedEv.ID:
//                {
//                    Connection conn = ((CallCtlConnDisconnectedEv)event).getConnection();
//                    if (conn.getAddress().equals(sourceAddress))
//                        state.setState(CallState.FINISHED);
//                }
//            }
//        }
    }

}
