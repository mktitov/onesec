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
import java.util.HashMap;
import java.util.Map;
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
import javax.telephony.callcontrol.events.CallCtlCallEv;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnTalkingEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermEv;
import org.onesec.core.call.AddressMonitor;
import org.onesec.core.call.CallCompletionCode;
import org.onesec.core.call.CallResult;
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
    private String expectedAddress;
    private Map<String, CallResultImpl> activeCalls;
    private Lock lock;
    private Condition inServiceCondition;
    private Condition waitingForCallCondition;
    private Condition callCompleteCondition;
    private Condition callStartedCondition;

    public AddressMonitorImpl(ProviderRegistry providerRegistry, String address)
            throws Exception
    {
        this.address = address;
        terminalReady = new AtomicBoolean(false);
        addressReady = new AtomicBoolean(false);
        activeCalls = new HashMap<String, CallResultImpl>();
        
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
        callStartedCondition = lock.newCondition();
        callCompleteCondition = lock.newCondition();
        
        terminal.addObserver(this);
        terminalAddress.addObserver(this);
        terminalAddress.addCallObserver(this);

        Thread.sleep(1000);
        checkStatus(null, null);
    }

    private void checkStatus(String callStartAddress, String callFinishAddress)
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
            if (callFinishAddress!=null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(address+". Finished call with ("+callFinishAddress+")");
                CallResultImpl callRes = activeCalls.remove(callFinishAddress);
                if (callFinishAddress.equals(expectedAddress))
                {
                    callRes.markConversationEnd();
                    if (callRes.getCompletionCode()==null)
                        callRes.setCompletionCode(CallCompletionCode.NORMAL);
                    callCompleteCondition.signal();
                }
            }
            if (callStartAddress!=null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(address+". Started call with ("+callStartAddress+")");
                activeCalls.put(callStartAddress, new CallResultImpl(System.currentTimeMillis()));
                if (callStartAddress.equals(expectedAddress))
                    callStartedCondition.signal();
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

    public CallResult waitForCallCompletion(
            String expectedAddress, long callStartTimeout, long callEndTimeout)
    {
        if (logger.isDebugEnabled())
            logger.debug(
                    address+". Waiting for completion the call with ("+expectedAddress+") address");
        lock.lock();
        try
        {
            this.expectedAddress = expectedAddress;
            if (!activeCalls.containsKey(expectedAddress))
            {
                boolean started = waitForCondition(callStartedCondition, callStartTimeout);
                if (!started)
                    return new CallResultImpl(CallCompletionCode.ERROR);
            }
            CallResultImpl callResult = activeCalls.get(expectedAddress);
            if (!waitForCondition(callCompleteCondition, callEndTimeout))
                callResult.setCompletionCode(CallCompletionCode.ERROR);
            return callResult;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void releaseMonitor()
    {
        try{
            try{
                terminalAddress.removeCallObserver(this);
            } finally {
                try {
                    terminalAddress.removeObserver(this);
                } finally {
                    terminal.removeObserver(this);
                }
            }
        }catch(Throwable e){}
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

    public void addressChangedEvent(AddrEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved address events: "+OnesecUtils.eventsToString(events));
        for (AddrEv event: events)
            switch (event.getID())
            {
                case CiscoAddrInServiceEv.ID:
                    addressReady.set(true); checkStatus(null, null); break;
                case CiscoAddrOutOfServiceEv.ID: 
                    addressReady.set(false); checkStatus(null, null); break;
            }
    }

    public void terminalChangedEvent(TermEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved terminal events: "+OnesecUtils.eventsToString(events));
        for (TermEv event: events)
            switch (event.getID())
            {
                case CiscoTermInServiceEv.ID: 
                    terminalReady.set(true); checkStatus(null, null); break;
                case CiscoTermOutOfServiceEv.ID: 
                    terminalReady.set(false); checkStatus(null, null); break;
            }
    }

    public void callChangedEvent(CallEv[] events)
    {
        if (logger.isDebugEnabled())
            logger.debug(address+". Recieved call events: "+OnesecUtils.eventsToString(events));
        for (CallEv event: events)
        {
            switch(event.getID())
            {
                case CallCtlConnFailedEv.ID:
                {
                    CallCtlConnFailedEv ev = (CallCtlConnFailedEv) event;
                    lock.lock();
                    try {
                        String opAddress = getOppositeAddress(ev);
                        CallResultImpl callRes = activeCalls.get(opAddress);
                        if (ev.getCallControlCause()==CallCtlConnFailedEv.CAUSE_NORMAL)
                            callRes.setCompletionCode(CallCompletionCode.NO_ANSWER);
                        else
                            callRes.setCompletionCode(CallCompletionCode.ERROR);
                        if (logger.isDebugEnabled())
                            logger.debug(
                                    "{}. Connection with ({}) address failed. " +
                                    "Completion code is {}"
                                    , new Object[]{
                                        address, opAddress, callRes.getCompletionCode()});
                    }finally{
                        lock.unlock();
                    }
                    break;
                }
                case CallCtlConnDisconnectedEv.ID:
                {
                    CallCtlConnDisconnectedEv ev = (CallCtlConnDisconnectedEv) event;
                    String connectionAddress = ev.getConnection().getAddress().getName();
                    if (!address.equals(connectionAddress))
                        checkStatus(null, connectionAddress);
                    else
                        checkStatus(null, null);
                    break;
                }
                case CallCtlConnEstablishedEv.ID:
                {
                    CallCtlConnEstablishedEv ev = (CallCtlConnEstablishedEv) event;
                    String connectionAddress = ev.getConnection().getAddress().getName();
                    if (!address.equals(connectionAddress))
                        checkStatus(connectionAddress, null);
                    break;
                }
                case CallCtlTermConnTalkingEv.ID:
                {
                    CallCtlTermConnTalkingEv ev = (CallCtlTermConnTalkingEv) event;
                    lock.lock();
                    try {
                        String opAddress = getOppositeAddress(ev);
                        activeCalls.get(opAddress).markConversationStart();
                        if (logger.isDebugEnabled())
                            logger.debug(
                                    "{}. Conversation started with ({}) address"
                                    , new Object[]{address, opAddress});
                    } finally {
                        lock.unlock();
                    }
                    break;
                }
            }
        }
    }

    private String getOppositeAddress(CallCtlCallEv ev)
    {
        return !address.equals(ev.getCalledAddress().getName())?
            ev.getCalledAddress().getName() : ev.getCallingAddress().getName();
    }
}
