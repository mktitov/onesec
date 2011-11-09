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

import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallActiveEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointImpl implements CiscoTerminalObserver, AddressObserver, CallControlCallObserver
        , MediaCallObserver
{
    private final ProviderRegistry providerRegistry;
    private final StateListenersCoordinator stateListenersCoordinator;
    private final TerminalStateMonitoringService terminalStateMonitoringService;

    private final RtpStreamManager rtpStreamManager;
    private final ExecutorService executor;
    private final IvrConversationScenario conversationScenario;
    private final String address;
    private final Codec codec;
    private final Integer rtpPacketSize;
    private final int rtpMaxSendAheadPacketsCount;
    private final boolean enableIncomingRtp;
    private final boolean enableIncomingCalls;
    private final IvrTerminal term;

    private Address termAddress;
    private int maxChannels = Integer.MAX_VALUE;
    private CiscoTerminal ciscoTerm;

    private final Map<Call, IvrEndpointConversationImpl> calls =
            new HashMap<Call, IvrEndpointConversationImpl>();
    private final Map<Integer, IvrEndpointConversationImpl> connIds =
            new HashMap<Integer, IvrEndpointConversationImpl>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointImpl(ProviderRegistry providerRegistry
            , StateListenersCoordinator stateListenersCoordinator
            , TerminalStateMonitoringService terminalStateMonitoringService
            , IvrTerminal term)
    {
        this.providerRegistry = providerRegistry;
        this.stateListenersCoordinator = stateListenersCoordinator;
        this.terminalStateMonitoringService = terminalStateMonitoringService;
        this.rtpStreamManager = term.getRtpStreamManager();
        this.executor = term.getExecutor();
        this.conversationScenario = term.getConversationScenario();
        this.address = term.getAddress();
        this.codec = term.getCodec();
        this.rtpPacketSize = term.getRtpPacketSize();
        this.rtpMaxSendAheadPacketsCount = term.getRtpMaxSendAheadPacketsCount();
        this.enableIncomingRtp = term.getEnableIncomingRtp();
        this.enableIncomingCalls = term.getEnableIncomingCalls();
        this.term = term;
    }

    public void start() throws IvrEndpointException {
        try {
            if (term.isLogLevelEnabled(LogLevel.DEBUG))
                term.getLogger().debug("Checking provider...");
            ProviderController providerController = providerRegistry.getProviderController(address);
            Provider provider = providerController.getProvider();
            if (provider==null || provider.getState()!=Provider.IN_SERVICE)
                throw new Exception(String.format(
                        "Provider (%s) not IN_SERVICE", providerController.getName()));
            if (term.isLogLevelEnabled(LogLevel.DEBUG))
                term.getLogger().debug("Checking terminal address...");
            termAddress = provider.getAddress(address);
            ciscoTerm = registerTerminal(termAddress);
            registerTerminalListeners();
        } catch (Throwable e) {
            throw new IvrEndpointException("Error starting endpoint", e);
        }
    }

    public void stop() {
        unregisterTerminalListeners();
    }

    public void invite() {
        
    }

    private CiscoTerminal registerTerminal(Address addr) throws Exception {
        Terminal[] terminals = addr.getTerminals();
        if (terminals==null || terminals.length==0)
            throw new Exception(String.format("Address (%s) does not have terminals", address));
        Terminal terminal = terminals[0];
        if (terminal instanceof CiscoRouteTerminal) {
            if (term.isLogLevelEnabled(LogLevel.DEBUG))
                term.getLogger().debug("Registering {} terminal", CiscoRouteTerminal.class.getName());
            CiscoRouteTerminal routeTerm = (CiscoRouteTerminal) terminal;
            routeTerm.register(codec.getCiscoMediaCapabilities(), CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
            return routeTerm;
        } else if (terminal instanceof CiscoMediaTerminal) {
            if (term.isLogLevelEnabled(LogLevel.DEBUG))
                term.getLogger().debug("Registering {} terminal", CiscoMediaTerminal.class.getName());
            CiscoMediaTerminal mediaTerm = (CiscoMediaTerminal) terminal;
            mediaTerm.register(codec.getCiscoMediaCapabilities());
            maxChannels = 1;
            return mediaTerm;
        }
        throw new Exception(String.format("Invalid terminal class. Expected one of: %s, %s. But was %s"
                , CiscoRouteTerminal.class.getName(), CiscoMediaTerminal.class.getName()
                , terminal.getClass().getName()));
    }

    private void registerTerminalListeners() throws Exception {
        ciscoTerm.addObserver(this);
        termAddress.addObserver(this);
        termAddress.addCallObserver(this);
    }

    private void unregisterTerminalListeners() {
        try {
            try {
                termAddress.removeCallObserver(this);
            } finally {
                try {
                    termAddress.removeObserver(this);
                } finally {
                    ciscoTerm.removeObserver(this);
                }
            }
        } catch (Throwable e) {
            if (term.isLogLevelEnabled(LogLevel.WARN))
                term.getLogger().warn("Problem with unregistering listeners from the cisco terminal", e);
        }
    }

    public void terminalChangedEvent(TermEv[] eventList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void callChangedEvent(CallEv[] events) {
        if (term.isLogLevelEnabled(LogLevel.DEBUG))
            term.getLogger().debug("Recieved call events: "+eventsToString(events));
        for (CallEv event: events)
            switch (event.getID()) {
                case CallActiveEv.ID: createConversation(((CallActiveEv)event).getCall()); break;
                case ConnConnectedEv.ID: bindConnIdToConversation((ConnConnectedEv)event); break;
            }
    }
    
    public void addressChangedEvent(AddrEv[] events) {
    }

    private void createConversation(Call call) {
        lock.writeLock().lock();
        try {
            try {
                if (!calls.containsKey(call) && enableIncomingCalls && calls.size()<=maxChannels) {
                    IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(
                            term, executor, conversationScenario, rtpStreamManager, enableIncomingRtp, null);
                    calls.put(call, conv);
                }
            } catch (Throwable e) {
                if (term.isLogLevelEnabled(LogLevel.ERROR))
                    term.getLogger().error("Error creating conversation", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void bindConnIdToConversation(ConnConnectedEv ev) {
        
    }
    
    private String eventsToString(Object[] events) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append(i > 0 ? ", " : "").append(events[i].toString());
        return buf.toString();
    }
}
