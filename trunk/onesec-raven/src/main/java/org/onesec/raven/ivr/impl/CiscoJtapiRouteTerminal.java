/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRouteSession;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.callcenter.RouteAddress;
import javax.telephony.callcenter.RouteCallback;
import javax.telephony.callcenter.events.ReRouteEvent;
import javax.telephony.callcenter.events.RouteCallbackEndedEvent;
import javax.telephony.callcenter.events.RouteEndEvent;
import javax.telephony.callcenter.events.RouteEvent;
import javax.telephony.callcenter.events.RouteUsedEvent;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.CallRouteRule;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CiscoJtapiRouteTerminal implements CiscoTerminalObserver, AddressObserver, RouteCallback
{
    private final String address;
    private final ProviderRegistry providerRegistry;
    private final IvrTerminalStateImpl state;
    private final IvrTerminal term;
    private final Logger logger;
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final ConcurrentHashMap<String, CallRouteRule> routes = new ConcurrentHashMap<String, CallRouteRule>();
    private boolean termInService = false;
    private boolean termAddressInService = false;
    
    private Address termAddress;
    private Provider provider;
    private CiscoRouteTerminal ciscoTerm;

    public CiscoJtapiRouteTerminal(ProviderRegistry providerRegistry, 
            StateListenersCoordinator stateListenersCoordinator, IvrTerminal term) 
    {
        this.term = term;
        this.address = term.getAddress();
        this.providerRegistry = providerRegistry;
        this.state = new IvrTerminalStateImpl(term);
        this.logger = new LoggerHelper(term, "");
        stateListenersCoordinator.addListenersToState(state, IvrTerminalState.class);
        this.state.setState(IvrTerminalState.OUT_OF_SERVICE);
    }
    
    public IvrTerminalState getState() {
        return state;
    }
    
    public void start() throws IvrEndpointException {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Checking provider...");
            ProviderController providerController = providerRegistry.getProviderController(address);
            provider = providerController.getProvider();
            if (provider==null || provider.getState()!=Provider.IN_SERVICE)
                throw new Exception(String.format(
                        "Provider (%s) not IN_SERVICE", providerController.getName()));
            if (logger.isDebugEnabled())
                logger.debug("Checking terminal address...");
            termAddress = provider.getAddress(address);
            ciscoTerm = registerTerminal(termAddress);
            registerTerminalListeners();
        } catch (Throwable e) {
            throw new IvrEndpointException("Problem with starting endpoint", e);
        }
    }
    
    public void stop() {
        if (stopping.compareAndSet(false, true)) {
            resetListeners();
            unregisterTerminal(ciscoTerm);
            unregisterTerminalListeners();
        }
    }
    
    public Map<String, CallRouteRule> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }
    
    public void registerRoute(CallRouteRule route) {
        if (logger.isDebugEnabled())
            logger.debug("Registering new route: "+route);
        routes.put(route.getRuleKey(), route);
    }

    private CiscoRouteTerminal registerTerminal(Address addr) throws Exception {
        Terminal[] terminals = addr.getTerminals();
        if (terminals==null || terminals.length==0)
            throw new Exception(String.format("Address (%s) does not have terminals", address));
        CiscoTerminal terminal = (CiscoTerminal) terminals[0];
        if (!(terminal instanceof com.cisco.jtapi.extensions.CiscoRouteTerminal))
            throw new Exception(String.format(
                    "Invalid terminal type. Need (CiscoRouteTerminal) but found (%s)",
                    terminal.getClass().getName()));
        CiscoRouteTerminal routeTerm = (CiscoRouteTerminal) terminal;
        if (routeTerm.isRegisteredByThisApp())
            unexpectedUnregistration(routeTerm);
        routeTerm.register(Codec.AUTO.getCiscoMediaCapabilities(), 
                com.cisco.jtapi.extensions.CiscoRouteTerminal.NO_MEDIA_REGISTRATION);   
        return routeTerm;
    }
    
    private void registerTerminalListeners() throws Exception {
        ciscoTerm.addObserver(this);
        termAddress.addObserver(this);
//        termAddress.addCallObserver(this);
        ((RouteAddress)termAddress).registerRouteCallback(this);
    }
    
    private void unexpectedUnregistration(Terminal term) {
        if (logger.isWarnEnabled())
            logger.warn("Unexpected terminal unregistration. Triyng to register terminal but it "
                    + "already registered by this application! "
                    + "So unregistering terminal first");
        unregisterTerminal(term);
    }

    private void unregisterTerminal(Terminal term) {
        try {
            if (term instanceof com.cisco.jtapi.extensions.CiscoRouteTerminal)
                ((com.cisco.jtapi.extensions.CiscoRouteTerminal)term).unregister();
            else if (term instanceof CiscoMediaTerminal)
                ((CiscoMediaTerminal)term).unregister();
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error("Problem with terminal unregistration", e);
        }
    }

    private void unregisterTerminalListeners() {
        try {
            try {
                ((RouteAddress)termAddress).cancelRouteCallback(this);
            } finally {
                try {
                    termAddress.removeObserver(this);
                } finally {
                    ciscoTerm.removeObserver(this);
                }
            }
        } catch (Throwable e) {
            if (logger.isWarnEnabled())
                logger.warn("Problem with unregistering listeners from the cisco terminal", e);
        }
    }
    
    private void resetListeners() {
//        listenersLock.writeLock().lock();
//        try {
//            conversationListeners.clear();
//        } finally {
//            listenersLock.writeLock().unlock();
//        }
    }

    public void terminalChangedEvent(TermEv[] events) {
        if (logger.isDebugEnabled())
            logger.debug("Recieved terminal events: "+eventsToString(events));
        for (TermEv ev: events)
            switch (ev.getID()) {
                case CiscoTermInServiceEv.ID: termInService = true; checkState(); break;
            }
    }
    
    public void addressChangedEvent(AddrEv[] events) {
        if (logger.isDebugEnabled())
            logger.debug("Recieved address events: "+eventsToString(events));
        for (AddrEv ev: events)
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID: termAddressInService = true; checkState(); break;
                case CiscoAddrOutOfServiceEv.ID: termAddressInService = false; checkState(); break;
            }
    }

    public void callChangedEvent(CallEv[] events) {
        if (logger.isDebugEnabled())
            logger.debug("Recieved address events: "+eventsToString(events));
        for (CallEv ev: events) {
        }
    }
    
    private synchronized void checkState() {
        if (state.getId()==IvrTerminalState.OUT_OF_SERVICE && !stopping.get()) {
            if (termInService && termAddressInService)
                state.setState(IvrTerminalState.IN_SERVICE);
        } else if (!termAddressInService || !termInService)
            state.setState(IvrTerminalState.OUT_OF_SERVICE);
    }
    
    private String eventsToString(Object[] events) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append(i > 0 ? ", " : "").append(events[i].toString());
        return buf.toString();
    }

    public void reRouteEvent(ReRouteEvent event) {
        if (logger.isDebugEnabled())
            logger.debug("Received route callback event: "+event.getClass().getName());
    }

    public void routeCallbackEndedEvent(RouteCallbackEndedEvent event) {
        if (logger.isDebugEnabled())
            logger.debug("Received route callback event: "+event.getClass().getName());
    }

    public void routeEvent(RouteEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received route callback event: "+event.getClass().getName());
            logger.debug("Calling number: {}; currentRouteNumber: {}", 
                    event.getCallingAddress().getName(), event.getCurrentRouteAddress().getName());
        }
        CiscoRouteSession sess = (CiscoRouteSession) event.getRouteSession();
        CallRouteRule route = findRoute(event.getCallingAddress().getName());
        try {
            if (route==null)
                sess.endRoute(CiscoRouteSession.ERROR_NO_CALLBACK);
            else 
                sess.selectRoute(route.getDestinations(), CiscoRouteSession.DEFAULT_SEARCH_SPACE, 
                        route.getCallingNumbers());
        } catch (Exception ex) {
            if (logger.isErrorEnabled())
                logger.error("Routing error", ex);
        } 
    }
    
    private CallRouteRule findRoute(String callingNumber) {
        Iterator<CallRouteRule> it = routes.values().iterator();
        while (it.hasNext()) {
            CallRouteRule route = it.next();
            if (route.accept(callingNumber)) {
                if (logger.isDebugEnabled())
                    logger.debug("Found route for call from ({}): {}", callingNumber, route);
                if (!route.isPermanent()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Removing route: "+route);
                    it.remove();
                }
                return route;
            }
        }
        if (logger.isErrorEnabled())
            logger.error("Can't find route for incoming call from ({})", callingNumber);
        return null;
    }

    public void routeUsedEvent(RouteUsedEvent event) {
        if (logger.isDebugEnabled())
            logger.debug("Received route callback event: "+event.getClass().getName());
    }

    public void routeEndEvent(RouteEndEvent event) {
        if (logger.isDebugEnabled())
            logger.debug("Received route callback event: "+event.getClass().getName());
    }
}
