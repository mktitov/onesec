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

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoAddress;
import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoCallChangedEv;
import com.cisco.jtapi.extensions.CiscoConnection;
import com.cisco.jtapi.extensions.CiscoMediaOpenLogicalChannelEv;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPInputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPParams;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalConnection;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.cisco.jtapi.extensions.CiscoTransferEndEv;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnDialingEv;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnEv;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlConnInitiatedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallActiveEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.CallInvalidEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnCreatedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.ConnEv;
import javax.telephony.events.TermConnEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.MediaTerminalConnection;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import static org.onesec.raven.impl.CCMUtils.*;
import org.onesec.raven.ivr.*;
import org.raven.ds.impl.DataContextImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.table.TableImpl;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.ViewableObjectImpl;
import org.slf4j.Logger;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public class CiscoJtapiTerminal implements CiscoTerminalObserver, AddressObserver, CallControlCallObserver
        , MediaCallObserver, IvrEndpointConversationListener
{
    private final ProviderRegistry providerRegistry;

    private final RtpStreamManager rtpStreamManager;
    private final ExecutorService executor;
//    private final IvrConversationScenario conversationScenario;
    private final String address;
    private final Codec codec;
    private final Integer rtpPacketSize;
    private final int rtpMaxSendAheadPacketsCount;
    private final boolean enableIncomingRtp;
    private final boolean enableIncomingCalls;
    private final IvrMediaTerminal term;
    private final Logger logger;
    private final CallsRouter callsRouter;
    private final Map<String, Call> callsToUnpark = new ConcurrentHashMap<String, Call>();

    private Address termAddress;
    private int maxChannels = Integer.MAX_VALUE;
    private CiscoTerminal ciscoTerm;
    private boolean termInService = false;
    private boolean termAddressInService = false;
    Provider provider;

    private final Map<Call, ConvHolder> calls = new HashMap<Call, ConvHolder>();
    private final Map<Integer, ConvHolder> connIds = new HashMap<Integer, ConvHolder>();
    private final Set<Call> transferCalls = new ConcurrentSkipListSet<Call>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final IvrTerminalStateImpl state;
    private final Set<IvrEndpointConversationListener> conversationListeners =
            new HashSet<IvrEndpointConversationListener>();
    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
    private final AtomicBoolean stopping = new AtomicBoolean();

    @Message private static String callIdColumnMessage;
    @Message private static String callInfoColumnMessage;
    @Message private static String callCreationTimeColumnMessage;
    @Message private static String callDurationColumnMessage;
    @Message private static String endpointBusyMessage;
    @Message private static String callsCountMessage;

    public CiscoJtapiTerminal(ProviderRegistry providerRegistry
            , StateListenersCoordinator stateListenersCoordinator
            , IvrMediaTerminal term, CallsRouter callsRouter)
    {
        this.providerRegistry = providerRegistry;
        this.rtpStreamManager = term.getRtpStreamManager();
        this.executor = term.getExecutor();
//        this.conversationScenario = term.getConversationScenario();
        this.address = term.getAddress();
        this.codec = term.getCodec();
        this.rtpPacketSize = term.getRtpPacketSize();
        this.rtpMaxSendAheadPacketsCount = term.getRtpMaxSendAheadPacketsCount();
        this.enableIncomingRtp = term.getEnableIncomingRtp();
        this.enableIncomingCalls = term.getEnableIncomingCalls();
        this.term = term;
        this.logger = new LoggerHelper(term, null);
        this.callsRouter = callsRouter;
        this.state = new IvrTerminalStateImpl(term);
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
            throw new IvrEndpointException(ccmExLog("Problem with starting endpoint.", e), e);
        }
    }

    public void stop() {
        if (stopping.compareAndSet(false, true)) {
            resetListeners();
            unregisterTerminal(ciscoTerm);
            unregisterTerminalListeners();
        }
    }

    private void resetListeners() {
        listenersLock.writeLock().lock();
        try {
            conversationListeners.clear();
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , final IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings, String callingNumber)
    {
        Call call = null;
        try {
            lock.writeLock().lock();
            IvrEndpointConversationImpl conv = null;
            try {
                if (state.getId()!=IvrTerminalState.IN_SERVICE)
                    throw new Exception("Can't invite oppenent to conversation. Terminal not ready");
                if (calls.size()>=maxChannels)
                    throw new Exception("Can't invite oppenent to conversation. Too many opened channels");
                call = provider.createCall();
                conv = new IvrEndpointConversationImpl(term, this, executor, scenario
                        , rtpStreamManager, enableIncomingRtp, address, bindings);
                conv.addConversationListener(listener);
                conv.addConversationListener(this);
                ConvHolder holder = new ConvHolder(conv, false, opponentNum);                
                opponentNum = holder.calledNumber;
                calls.put(call, holder);
                if (callsRouter!=null) {
                    callsRouter.setData(
                            null, 
                            new CallRouteRuleImpl(address, opponentNum, callingNumber, false, 10), 
                            new DataContextImpl());
                    opponentNum = callsRouter.getAddress();
                } 
                call.connect(ciscoTerm, termAddress, opponentNum); //если передвинуть за lock блок 
                                //перестает работать IvrEndpointConversation.sendMessage
                                //поскольку IvrEndpointConversation.calledNumber == null
            } finally {
                lock.writeLock().unlock();
            }
            if (inviteTimeout>0) 
                executor.execute(inviteTimeout*1000, new InviteTimeoutHandler(conv, call, maxCallDur));
            else if (maxCallDur>0)
                executor.execute(maxCallDur*1000, new MaxCallDurationHandler(conv, call));
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) 
                logger.warn(ccmExLog(String.format("Problem with inviting abonent with number (%s)", opponentNum), e), e);
            final IvrEndpointConversationStoppedEvent ev = new IvrEndpointConversationStoppedEventImpl(
                    null, CompletionCode.TERMINAL_NOT_READY);
            if (call!=null)
                stopConversation(call, CompletionCode.OPPONENT_UNKNOWN_ERROR);
            executor.executeQuietly(new AbstractTask(term, "Propagating conversation stop event") {
                @Override public void doRun() throws Exception {
                    listener.conversationStopped(ev);
                }
            });
            conversationStopped(ev);
        }
    }
    
    public void transfer(Call callForTransfer, String address) throws IvrEndpointException {
        if (logger.isErrorEnabled())
            logger.debug(callLog(callForTransfer, "Transfering call to the (%s)", address));
        CiscoTerminalConnection conn = findSelfTerminalConnection((CiscoCall)callForTransfer);
        if (conn==null)
            throw new IvrEndpointException(callLog(callForTransfer, "Transfer error. Can't find terminal connection"));
        CiscoCall call = null;
        try {
            ConvHolder convHolder = getConvHolderByCall(callForTransfer);
            if (convHolder==null) {
                if (logger.isWarnEnabled())
                    logger.warn(callLog(callForTransfer, "Can't find a call to transfer (maybe dropped)"));
            } else {
                convHolder.transfering = true;
                call = (CiscoCall) provider.createCall();
                lock.writeLock().lock();
                try {
                    ConvHolder holder = new ConvHolder(callForTransfer, address);
                    address = holder.transferAddress;
                    calls.put(call, holder);
                } finally {
                    lock.writeLock().unlock();
                }
//                call.consult(conn, address);            
                    call.connect(ciscoTerm, termAddress, address);            
            }
        } catch (Throwable e) {
            String mess = ccmExLog(callLog(callForTransfer, "Transfer error"), e);
            if (logger.isErrorEnabled())
                logger.error(mess, e);
            if (call!=null)
                getAndRemoveConvHolder(call);
            dropCallQuietly(call);
            stopConversation(callForTransfer, CompletionCode.TRANSFER_ERROR);
            throw new IvrEndpointException(mess, e);
        }
    }
    
    public void unpark(Call acall, String parkDN) throws IvrEndpointException {
        try {
//            callsToUnpark.put(parkDN, acall);
            TerminalConnection conn = ciscoTerm.unPark(termAddress, parkDN);            
            if (logger.isDebugEnabled()) {
                logger.debug(callLog(acall, "UnPark terminal connection created: "+conn));
                logger.debug(callLog(acall, "UnPark call: "+getCallDesc((CiscoCall)conn.getConnection().getCall())));
            }
            ConvHolder conv = new ConvHolder(parkDN, acall);
            lock.writeLock().lock();
            try {
                calls.put(conn.getConnection().getCall(), conv);
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Throwable e) {
            String mess = ccmExLog(callLog(acall, "Unpark error"), e);
            if (logger.isErrorEnabled())
                logger.error(ccmExLog(callLog(acall, "Unpark error"), e), e);
            stopConversation(acall, CompletionCode.UNPARK_ERROR);
            throw new IvrEndpointException(mess, e);
        }
    }
    
    public int getActiveCallsCount() {
        lock.readLock().lock();
        try {
            return Math.max(calls.size(), getTerminalCallsCount());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private int getTerminalCallsCount() {
        TerminalConnection[] connections = ciscoTerm.getTerminalConnections();
        if (connections!=null && connections.length>0) {
            Set<Call> _calls = new HashSet<Call>();
            for (TerminalConnection connection: connections)
                _calls.add(connection.getConnection().getCall());
            return _calls.size();
        }
        return 0;
    }

    public List<ViewableObject> getViewableObjects() throws Exception {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        List<CallInfo> callsInfo = getCallsInfo();
        if (callsInfo==null) {
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, endpointBusyMessage));
        } else {
            int callsCount = callsInfo.size();
            TableImpl table = new TableImpl(new String[]{callIdColumnMessage
                    , callCreationTimeColumnMessage, callDurationColumnMessage, callInfoColumnMessage});
            for (CallInfo info: callsInfo)
                table.addRow(new Object[]{
                    info.getCallId(), new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(info.getCreated()),
                    info.getDuration(), info.getDescription()
                });
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
            vos.add(new ViewableObjectImpl(
                    Viewable.RAVEN_TEXT_MIMETYPE, String.format(callsCountMessage, callsCount)));
        }
//        if (lock.readLock().tryLock(500, TimeUnit.MILLISECONDS)){
//            try{
//                callsCount = calls.size();
//                TableImpl table = new TableImpl(new String[]{callIdColumnMessage
//                        , callCreationTimeColumnMessage, callDurationColumnMessage, callInfoColumnMessage});
//                for (Map.Entry<Call, ConvHolder> entry: calls.entrySet())
//                    table.addRow(new Object[]{
//                        entry.getKey().toString(),
//                        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(entry.getValue().created)),
//                        entry.getValue().getDuration(),
//                        entry.getValue().toString()});
//                obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
//            }finally{
//                lock.readLock().unlock();
//            }
//        } else {
//            obj = new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, endpointBusyMessage);
//        }
        return vos;
    }
    
    public List<CallInfo> getCallsInfo() {
        try {
            if (lock.readLock().tryLock(500, TimeUnit.MILLISECONDS))
                try {
                    List<CallInfo> res = new LinkedList<CallInfo>();
                    for (Map.Entry<Call, ConvHolder> entry: calls.entrySet())
                        res.add(new CallInfo(
                                entry.getKey().toString(), new Date(entry.getValue().created), 
                                entry.getValue().getDuration(), entry.getValue().toString()));
                    return res;
                } finally {
                    lock.readLock().unlock();
                }
        } catch (InterruptedException e) {}
        return null;
    }

    private CiscoTerminal registerTerminal(Address addr) throws Exception {
        Terminal[] terminals = addr.getTerminals();
        if (terminals==null || terminals.length==0)
            throw new Exception(String.format("Address (%s) does not have terminals", address));
        CiscoTerminal terminal = (CiscoTerminal) terminals[0];
        if (terminal instanceof CiscoRouteTerminal) {
            if (logger.isDebugEnabled())
                logger.debug("Registering {} terminal", CiscoRouteTerminal.class.getName());
            CiscoRouteTerminal routeTerm = (CiscoRouteTerminal) terminal;
            if (routeTerm.isRegisteredByThisApp())
                unexpectedUnregistration(routeTerm);
            routeTerm.register(codec.getCiscoMediaCapabilities(), CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
            return routeTerm;
        } else if (terminal instanceof CiscoMediaTerminal) {
            if (logger.isDebugEnabled())
                logger.debug("Registering {} terminal", CiscoMediaTerminal.class.getName());
            CiscoMediaTerminal mediaTerm = (CiscoMediaTerminal) terminal;
            if (mediaTerm.isRegisteredByThisApp())
                unexpectedUnregistration(mediaTerm);
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
    
    private void unexpectedUnregistration(Terminal term) {
        if (logger.isWarnEnabled())
            logger.warn("Unexpected terminal unregistration. Triyng to register terminal but it "
                    + "already registered by this application! "
                    + "So unregistering terminal first");
        unregisterTerminal(term);
    }

    private void unregisterTerminal(Terminal term) {
        try {
            if (term instanceof CiscoRouteTerminal)
                ((CiscoRouteTerminal)term).unregister();
            else if (term instanceof CiscoMediaTerminal)
                ((CiscoMediaTerminal)term).unregister();
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error(ccmExLog("Problem with terminal unregistration", e), e);
        }
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
            if (logger.isWarnEnabled())
                logger.warn(ccmExLog("Problem with unregistering listeners from the cisco terminal", e), e);
        }
    }

    public void terminalChangedEvent(TermEv[] events) {
        if (logger.isDebugEnabled())
            logger.debug("Recieved terminal events: "+eventsToString(events));
        for (TermEv ev: events)
            switch (ev.getID()) {
                case CiscoTermInServiceEv.ID: termInService = true; checkState(); break;
                case CiscoTermOutOfServiceEv.ID: termInService = false; checkState(); break;
                case CiscoMediaOpenLogicalChannelEv.ID: initInRtp((CiscoMediaOpenLogicalChannelEv)ev); break;
                case CiscoRTPOutputStartedEv.ID: initAndStartOutRtp((CiscoRTPOutputStartedEv) ev); break;
                case CiscoRTPInputStartedEv.ID: startInRtp((CiscoRTPInputStartedEv)ev); break;
                case CiscoRTPOutputStoppedEv.ID: stopOutRtp((CiscoRTPOutputStoppedEv)ev); break;
                case CiscoRTPInputStoppedEv.ID: stopInRtp((CiscoRTPInputStoppedEv)ev); break;
            }
    }

    public void callChangedEvent(CallEv[] events) {
        if (logger.isDebugEnabled())
            logCallEvents(events);
        for (CallEv ev: events) {
            switch (ev.getID()) {
                case CallActiveEv.ID: createConversation(((CallActiveEv)ev).getCall()); break;
                case CiscoCallChangedEv.ID: replaceCalls((CiscoCallChangedEv)ev); break;
                case ConnConnectedEv.ID: bindConnIdToConv((ConnConnectedEv)ev); break;
                case CallCtlConnOfferedEv.ID: acceptIncomingCall((CallCtlConnOfferedEv) ev); break;
                case TermConnRingingEv.ID   : answerOnIncomingCall((TermConnRingingEv)ev); break;
                case CallCtlConnEstablishedEv.ID: 
                    appendAdditionalNumberToAddress((CallCtlConnEv)ev); 
                    openLogicalChannel((CallCtlConnEstablishedEv)ev); 
                    break;
                case MediaTermConnDtmfEv.ID: continueConv((MediaTermConnDtmfEv)ev); break;
                case CiscoTransferEndEv.ID: callTransfered((CiscoTransferEndEv)ev);
                case ConnDisconnectedEv.ID: unbindConnIdFromConv((ConnDisconnectedEv)ev); break;
                case CallCtlConnFailedEv.ID: handleConnFailedEvent((CallCtlConnFailedEv)ev); break;
                case CallInvalidEv.ID: stopConversation(ev.getCall(), CompletionCode.COMPLETED_BY_OPPONENT); break;
            }
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

    private void createConversation(Call call) {
        if (logger.isDebugEnabled()) {
            CiscoCall c = (CiscoCall) call;
            logger.debug(callLog(call, "Trying to create conversation. (%s) -> (%s)", 
                    c.getCurrentCallingAddress(), c.getCurrentCalledAddress()));
        }
        lock.writeLock().lock();
        try {
            try {
                ConvHolder holder = calls.get(call);
                if (holder!=null && holder.isTransferCall())
                    return;
                if (holder==null && enableIncomingCalls && calls.size()<=maxChannels) {
                    if (logger.isDebugEnabled())
                        logger.debug(callLog(call, "Creating conversation"));
                    IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(
                            term, this, executor, term.getConversationScenario(), rtpStreamManager
                            , enableIncomingRtp, address, null);
                    conv.setCall((CallControlCall) call);
                    conv.addConversationListener(this);
                    calls.put(call, new ConvHolder(conv, true, null));
                } else if (holder!=null && !holder.incoming) 
                    holder.conv.setCall((CallControlCall) call);
            } catch (Throwable e) {
                if (logger.isErrorEnabled())
                    logger.error(ccmExLog("Error creating conversation", e), e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void replaceCalls(CiscoCallChangedEv event) {
        ConvHolder holder = getAndRemoveConvHolder(event.getOriginalCall());        
        if (holder!=null) {
            if (logger.isDebugEnabled())
                logger.debug(callLog(event.getOriginalCall(), "Replacing this call with: "+
                        getCallDesc(event.getSurvivingCall())));
            lock.writeLock().lock();
            try {
                calls.put(event.getSurvivingCall(), holder);
            } finally {
                lock.writeLock().unlock();
            }
            try {
                holder.conv.replaceCall((CiscoCall)event.getSurvivingCall());
            } catch (IvrEndpointConversationException ex) {
                if (logger.isErrorEnabled())
                    logger.error(callLog(event.getCall(), "Error replacing call in conversation with: %s", 
                            getCallDesc(event.getSurvivingCall())), ex);
            }
        }
    }

    private void bindConnIdToConv(ConnConnectedEv ev) {
        lock.writeLock().lock();
        try {
            if (ev.getConnection().getAddress().getName().equals(address)) {
                ConvHolder conv = calls.get(ev.getCall());
                if (conv!=null) {
                    connIds.put(((CiscoConnection)ev.getConnection()).getConnectionID().intValue(), conv);
                    if (logger.isDebugEnabled())
                        logger.debug(callLog(ev.getCall(), "Connection ID binded to the conversation"));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void unbindConnIdFromConv(ConnDisconnectedEv ev) {
        if (address.equals(ev.getConnection().getAddress().getName())) {
            lock.writeLock().lock();
            try {
                if (logger.isDebugEnabled())
                    logger.debug(callLog(ev.getCall(), "Unbinding connection ID from the conversation"));
                connIds.remove(((CiscoConnection)ev.getConnection()).getConnectionID().intValue());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void acceptIncomingCall(CallCtlConnOfferedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());        
        if (conv==null || !conv.incoming || conv.isUnparkCall()) 
            return;
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog(ev.getCall(), "Accepting call"));
            ((CallControlConnection)ev.getConnection()).accept();
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) 
                logger.warn(ccmExLog(callLog(ev.getCall(), "Problem with accepting call"), e), e);
        }
    }

    private void answerOnIncomingCall(TermConnRingingEv ev) {
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog(ev.getCall(), "Answering on call: ev (%s); termConn (%s)", ev, ev.getTerminalConnection()));
//            if (ev.getTerminalConnection().getState()==TerminalConnection.RINGING)
            if (ciscoTerm.equals(ev.getTerminalConnection().getTerminal())) {
                ev.getTerminalConnection().answer();
                if (logger.isDebugEnabled())
                    logger.debug(callLog(ev.getCall(), "Answered"));
            }
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error(callLogEx(ev.getCall(), "Problem with answering on call", e), e);
        }
    }
    
    private void openLogicalChannel(CallCtlConnEstablishedEv ev) {
        if (logger.isDebugEnabled())
            logger.debug(callLog(ev.getCall(), "Logical connection opened for address (%s)"
                    , ev.getConnection().getAddress().getName()));
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv==null) {
            if (logger.isDebugEnabled())
                logger.debug(callLog(ev.getCall(), "Ignoring 'logical connection opened' conversation not found"));
            return;
        }
        if (conv.isTransferCall() || conv.isUnparkCall()) {            
//            if (termAddress.equals(ev.getConnection().getAddress()))
                doTransfer((CiscoCall)ev.getCall(), conv);
        } else 
            try {
                conv.conv.logicalConnectionCreated(ev.getConnection().getAddress().getName());
            } catch (IvrEndpointConversationException e) {
                if (logger.isErrorEnabled())
                    logger.error(callLogEx(ev.getCall(), 
                            "Error open logical connection for address (%s)", e
                            , ev.getConnection().getAddress().getName()), e);
                conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
    }
    
    private void doTransfer(final CiscoCall call, final ConvHolder conv) {
        if (logger.isDebugEnabled())
            logger.debug(callLog(call, "Trying to transfer call"));
        Connection[] conns = call.getConnections();
        if (conns==null || conns.length==0)
            return;
        for (Connection con: conns)
            if (con.getState()!=Connection.CONNECTED)
                return;
        if (!conv.transfered.compareAndSet(false, true)) {
            if (logger.isDebugEnabled())
                logger.debug(callLog(call, "Already transfered"));
            return;
        }
        String mess = callLog(conv.callForTransfer, "Transfering call to address (%s)", conv.transferAddress);
        if (logger.isDebugEnabled())
            logger.debug(mess);
        executor.executeQuietly(new AbstractTask(term, mess) {
            @Override public void doRun() throws Exception {
                try {
//                    call.setTransferController(findSelfTerminalConnection(call));
//                    conv.callForTransfer.setTransferController(findSelfTerminalConnection(conv.callForTransfer));
//                    if (logger.isDebugEnabled())
                    conv.callForTransfer.transfer(call);
                } catch (Throwable ex) {
                    if (logger.isErrorEnabled())
                        logger.error(callLogEx(conv.callForTransfer, 
                                "Transfer error to address (%s)", ex, conv.transferAddress), ex);

                }
            }
        });
    }
    
    private void callTransfered(CiscoTransferEndEv ev) {
//        if (logger.isDebugEnabled())
//            logger.debug(callLog(ev.getCall(), "Logical connection opened for address (%s)"
//                    , ev.getConnection().getAddress().getName()));
//        System.out.println("!!! Transfer controller address: "+ev.getTransferControllerAddress().getName());
//        ConvHolder conv = getConvHolderByCall(ev.getCall());
//        if (conv!=null) {
//            conv.conv.opponentPartyTransfered();
//        }
//            try {
//            } catch (IvrEndpointConversationException e) {
//                if (logger.isErrorEnabled())
//                    logger.error(callLog(ev.getCall(), 
//                            "Error open logical connection for address (%s)"
//                            , ev.getConnection().getAddress().getName()), e);
//                conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//            }
    }
    
    private void initInRtp(CiscoMediaOpenLogicalChannelEv ev) {
        ConvHolder conv = getConvHolderByConnId(ev.getCiscoRTPHandle().getHandle());
        if (conv==null || conv.isTransferCall() || conv.isTransfering())
            return;
        try {
            if (logger.isDebugEnabled())
                logger.debug("Initializing incoming RTP stream for terminal: "+ev.getTerminal());
            IncomingRtpStream rtp = conv.conv.initIncomingRtp();
            CiscoRTPParams params = new CiscoRTPParams(rtp.getAddress(), rtp.getPort());
            if (ev.getTerminal() instanceof CiscoMediaTerminal)
                ((CiscoMediaTerminal)ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
            else if (ev.getTerminal() instanceof CiscoRouteTerminal)
                ((CiscoRouteTerminal)ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
        } catch (Throwable e) {
            if (conv.conv.getState().getId()!=IvrEndpointConversationState.INVALID && logger.isErrorEnabled())
                logger.error(ccmExLog("Error initializing incoming RTP stream", e), e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void initAndStartOutRtp(CiscoRTPOutputStartedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv==null || conv.isTransferCall())
            return;
        try {
            CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
            if (logger.isDebugEnabled())
                    logger.debug(callLog(ev.getCallID().getCall(),
                            "Proposed RTP params: remoteHost (%s), remotePort (%s), packetSize (%s ms), " +
                            "payloadType (%s), bitrate (%s)"
                            , props.getRemoteAddress().toString(), props.getRemotePort()
                            , props.getPacketSize(), props.getPayloadType(), props.getBitRate()));
            Integer psize = rtpPacketSize;
            Codec streamCodec = Codec.getCodecByCiscoPayload(props.getPayloadType());
            if (streamCodec==null)
                throw new Exception(String.format(
                        "Not supported payload type (%s)", props.getPayloadType()));
            if (psize==null)
                psize = (int)streamCodec.getPacketSizeForMilliseconds(props.getPacketSize());
            if (logger.isDebugEnabled())
                logger.debug(callLog(ev.getCallID().getCall()
                    ,"Choosed RTP params: packetSize (%s ms), codec (%s), audioFormat (%s)"
                    , streamCodec.getMillisecondsForPacketSize(psize), streamCodec, streamCodec.getAudioFormat()));
            conv.conv.initOutgoingRtp(props.getRemoteAddress().getHostAddress(), props.getRemotePort()
                    , psize, rtpMaxSendAheadPacketsCount, streamCodec);
            conv.conv.startOutgoingRtp();
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error(callLogEx(ev.getCallID().getCall()
                        ,"Error initializing and starting outgoing RTP stream", e), e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void startInRtp(CiscoRTPInputStartedEv ev) {
        final Call call = ev.getCallID().getCall();
        ConvHolder conv = getConvHolderByCall(call);
        if (logger.isDebugEnabled())
            logger.debug(callLog(call, "Trying to start incoming RTP"));
        if (conv==null)
            return;
        try {
            conv.conv.startIncomingRtp();
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error(callLogEx(ev.getCallID().getCall(), "Problem with start incoming RTP stream", e), e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void stopOutRtp(CiscoRTPOutputStoppedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv!=null)
            conv.conv.stopOutgoingRtp();
    }
    
    private void stopInRtp(CiscoRTPInputStoppedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv!=null)
            conv.conv.stopIncomingRtp();
    }

    private void continueConv(MediaTermConnDtmfEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv!=null)
            conv.conv.continueConversation(ev.getDtmfDigit());
    }

    private void handleConnFailedEvent(CallCtlConnFailedEv ev) {
        final Call call = ev.getCall();
        if (logger.isDebugEnabled())
            logger.debug(callLog(call, "Connection failed (%s). Cause: %s, Call control cause: %s", 
                    ev.getConnection(), ev.getCause(), ev.getCallControlCause()));
        ConvHolder conv = getAndRemoveConvHolder(ev.getCall());
        if (conv==null)
            return;
        int cause = ev.getCallControlCause();
        CompletionCode code = CompletionCode.OPPONENT_UNKNOWN_ERROR;
        switch (cause) {
            case CallCtlConnFailedEv.CAUSE_BUSY:
                code = CompletionCode.OPPONENT_BUSY;
                break;
            case CallCtlConnFailedEv.CAUSE_CALL_NOT_ANSWERED:
            case CallCtlConnFailedEv.CAUSE_NORMAL:
                code = CompletionCode.OPPONENT_NOT_ANSWERED;
                break;
        }
        conv.conv.stopConversation(code);
    }
    
    private void stopConversation(Call call, CompletionCode completionCode) {
        ConvHolder conv = getAndRemoveConvHolder(call);
        if (conv!=null)
            conv.conv.stopConversation(completionCode);
    }

    private String callLog(Call call, String message, Object... args) {
        return getCallDesc((CiscoCall)call)+" : Terminal. "+String.format(message, args);
    }
    
    private String callLogEx(Call call, String message, Throwable ex, Object... args) {
        return ccmExLog(callLog(call, message, args), ex);
    }

    String getCallDesc(CiscoCall call) {
        return "[cid: "+call.getCallID().intValue()+", "+"("+termAddress.getName()+")"+
                getAddrName(call.getCurrentCallingAddress())+"->"+getAddrName(call.getCurrentCalledAddress())+"]";
    }
    
    private static String getAddrName(Address addr) {
        return addr==null? "" : addr.getName();
    }
    
    private String eventsToString(Object[] events) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append(i > 0 ? ", " : "").append(events[i].toString());
        return buf.toString();
    }
    
    private void logCallEvents(CallEv[] events) {
        if (events!=null && events.length>0)
            for (CallEv event: events)
                logger.debug(callLog(event.getCall(), "Received call event: "+event));
                
    }

    private ConvHolder getConvHolderByConnId(int connId) {
        lock.readLock().lock();
        try {
            return connIds.get(connId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private ConvHolder getConvHolderByCall(Call call) {
        lock.readLock().lock();
        try {
            return calls.get(call);
        } finally {
            lock.readLock().unlock();
        }
    }

    ConvHolder getAndRemoveConvHolder(Call call) {
        lock.writeLock().lock();
        try {
            return calls.remove(call);
        } finally {
            lock.writeLock().unlock();
        }
    }

//    private boolean isLogLevelEnabled(LogLevel logLevel) {
//        return term.isLogLevelEnabled(logLevel);
////        return this.logLevel.ordinal() <= logLevel.ordinal();
//    }

    private synchronized void checkState() {
        if (state.getId()==IvrTerminalState.OUT_OF_SERVICE && !stopping.get()) {
            if (termInService && termAddressInService)
                state.setState(IvrTerminalState.IN_SERVICE);
        } else if (!termAddressInService || !termInService)
            state.setState(IvrTerminalState.OUT_OF_SERVICE);
    }
    
    private CiscoTerminalConnection findSelfTerminalConnection(CiscoCall call) {
        Connection[] conns = call.getConnections();
        if (conns!=null)
            for (Connection conn: conns)
                if (termAddress.equals(conn.getAddress())) {
                    TerminalConnection[] termCons = conn.getTerminalConnections();
                    if (termCons!=null && termCons.length>0)
                        return (CiscoTerminalConnection) termCons[0];
                }
        return null;
    }
    
    private void dropCallQuietly(Call acall) {
        if (acall==null)
            return;
        CiscoCall call = (CiscoCall) acall;
        try {
            call.drop();
        } catch (Throwable e){}
    }

    int getCallsCount() {
        lock.readLock().lock();
        try {
            return calls.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    int getConnectionsCount() {
        lock.readLock().lock();
        try {
            return connIds.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addConversationListener(IvrEndpointConversationListener listener) {
        listenersLock.writeLock().lock();
        try {
            conversationListeners.add(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    public void removeConversationListener(IvrEndpointConversationListener listener) {
        listenersLock.writeLock().lock();
        try {
            conversationListeners.remove(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    private void fireConversationEvent(MethodCaller method) {
        listenersLock.readLock().lock();
        try {
            for (IvrEndpointConversationListener listener : conversationListeners)
                method.callMethod(listener);
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    //--------------- IvrEndpointConversationListener methods -----------------//
    public void listenerAdded(final IvrEndpointConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.listenerAdded(event);
            }
        });
    }

    public void conversationStarted(final IvrEndpointConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationStarted(event);
            }
        });
    }

    public void conversationStopped(final IvrEndpointConversationStoppedEvent event) {
//        if (event.getCompletionCode()==CompletionCode.COMPLETED_BY_ENDPOINT)
//            getAndRemoveConvHolder(event.getCall());
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationStopped(event);
            }
        });
    }

    public void conversationTransfered(final IvrEndpointConversationTransferedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationTransfered(event);
            }
        });
    }

    public void incomingRtpStarted(final IvrIncomingRtpStartedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.incomingRtpStarted(event);
            }
        });
    }

    public void outgoingRtpStarted(final IvrOutgoingRtpStartedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.outgoingRtpStarted(event);
            }
        });
    }

    public void dtmfReceived(final IvrDtmfReceivedConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.dtmfReceived(event);
            }
        });
    }
    //--------------- End of the IvrEndpointConversationListener methods -----------------//

    private void appendAdditionalNumberToAddress(CallCtlConnEv ev) {
        ConvHolder holder = getConvHolderByCall(ev.getCall());
        if (holder!=null && holder.additionalNumber!=null) {
            Connection[] connections = ev.getCall().getConnections();
            if (connections!=null) {
                TerminalConnection termConn = null;
                int estCount = 0;
                for (Connection conn: connections) {
                    if (logger.isDebugEnabled())
                        logger.debug("Testing connection: "+conn);
                    CallControlConnection ctlConn = (CallControlConnection) conn;
                    TerminalConnection[] termConns = conn.getTerminalConnections();                    
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("    CallControll connection state: "+ctlConn.getCallControlState());
//                        if (termConns!=null && termConns.length>0) {
//                            logger.debug("    TerminalConnection state: "+termConns[0].getState());
//                            logger.debug("    TerminalConnections: "+ Arrays.toString(termConns));
//                        }
//                    }
                    if (ctlConn.getCallControlState()==CallControlConnection.ESTABLISHED) {
                        if (!address.equals(conn.getAddress().getName()))
                            estCount++;
                        else if (termConns!=null && termConns.length>0 
                                && termConns[0].getState()==TerminalConnection.ACTIVE) 
                        {
                            estCount++;
                            termConn = termConns[0];
                        }
                    }
                }
                if (estCount>=2 && termConn!=null) {
                    if (logger.isDebugEnabled()) 
                        logger.debug(String.format("Appending (%s) to called number for connection: %s", 
                                holder.additionalNumber, termConn));
                    try {
                        ((MediaTerminalConnection)termConn).generateDtmf(holder.additionalNumber);
                    } catch (Throwable e) {
                        if (logger.isErrorEnabled())
                            logger.error(String.format("Error appending additional digits (%s) to called number: %s", 
                                    holder.additionalNumber, termConn), e);
                    }
                } else if (logger.isDebugEnabled())
                    logger.debug("Can't append additional digits to numbers. Not all connections ready, wating...");
            }
//                ((CallControlConnection)ev.getConnection()).addToAddress(holder.additionalNumber);
        }
    }
    
    private class ConvHolder {
        private final IvrEndpointConversationImpl conv;
        private final boolean incoming;
        private final long created = System.currentTimeMillis();
        private final AtomicBoolean transfered = new AtomicBoolean(false);
        private volatile CiscoCall callForTransfer;
        private volatile String transferAddress;
        private volatile boolean unparkCall = false;
        private volatile boolean transfering = false;
        private final String additionalNumber;
        private final String calledNumber;
        

        public ConvHolder(IvrEndpointConversationImpl conv, boolean incoming, String calledNumber) {
            this.conv = conv;
            this.incoming = incoming;
            callForTransfer = null;
            this.transferAddress = null;
            String[] nums = splitNumber(calledNumber);
            this.calledNumber = nums[0];
            this.additionalNumber = nums[1];
        }

        public ConvHolder(Call callForTransfer, String transferAddress) {
            this.conv = null;
            this.incoming = false;
            this.callForTransfer = (CiscoCall)callForTransfer;
            String[] nums = splitNumber(transferAddress);
            this.transferAddress = nums[0];
            this.additionalNumber = nums[1];
            this.calledNumber=null;
        }
        
        public ConvHolder(String parkDN, Call callForUnpark) {
            this(callForUnpark, parkDN);
            this.unparkCall = true;
        }
        
        /**
         * Returns <b>true</b> if this call created to make transfer
         */
        public boolean isTransferCall() {
            return callForTransfer!=null;
        }
        
        /**
         *  Returns <b>true</b> if this call is transferring
         */
        public boolean isTransfering() {
            return transfering;
        }
        
        public boolean isUnparkCall() {
            return unparkCall;
        }
        
        public void setCallForUnpark(CiscoCall call, String unparkDN) {
            unparkCall = true;
            callForTransfer = call;
            this.transferAddress = unparkDN;
        }
        
        public long getDuration() {
            return (System.currentTimeMillis() - created)/1000;
        }
        
        private String[] splitNumber(String number) {
            String[] res = new String[]{null, null};
            if (number!=null) {
                String[] tokens = number.split("\\*");
                for (int i=0; i<Math.min(tokens.length, res.length); ++i)
                    res[i] = tokens[i];
            }
            return res;
        }

        @Override
        public String toString() {
            return conv.toString();
        }
    }

    private abstract class MethodCaller {
        public abstract void callMethod(IvrEndpointConversationListener listener);
    }

    private class InviteTimeoutHandler extends AbstractTask implements IvrEndpointConversationListener {
        private final IvrEndpointConversationImpl conv;
        private final Call call;
        private final long stopCallAt;
        private final AtomicBoolean valid = new AtomicBoolean(true);

        public InviteTimeoutHandler(IvrEndpointConversationImpl conv, Call call, int maxCallDuration) {
            super(term, "Invite timeout handler for call");
            this.conv = conv;
            this.call = call;
            this.stopCallAt = maxCallDuration>0? System.currentTimeMillis()+maxCallDuration*1000 : 0;
            conv.addConversationListener(this);
        }

        @Override
        public void doRun() throws Exception {
            if (valid.get() && ObjectUtils.in(conv.getState().getId(), IvrEndpointConversationState.READY
                    , IvrEndpointConversationState.CONNECTING))
            {
                if (logger.isDebugEnabled())
                    logger.debug(callLog(call, "Detected INVITE TIMEOUT. Canceling a call"));
                getAndRemoveConvHolder(call);
                conv.stopConversation(CompletionCode.OPPONENT_NOT_ANSWERED);
            } else if (stopCallAt>0 && conv.getState().getId()!=IvrEndpointConversationState.INVALID)
                executor.executeQuietly(stopCallAt-System.currentTimeMillis()
                        , new MaxCallDurationHandler(conv, call));
        }

        public void listenerAdded(IvrEndpointConversationEvent event) { }

        public void conversationStarted(IvrEndpointConversationEvent event) {
            valid.compareAndSet(true, false);
        }

        public void conversationStopped(IvrEndpointConversationStoppedEvent event) { }

        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) { }

        public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }

        public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }

        public void dtmfReceived(IvrDtmfReceivedConversationEvent event) { }
    }
    
    private class MaxCallDurationHandler extends AbstractTask {
        private final IvrEndpointConversationImpl conv;
        private final Call call;

        public MaxCallDurationHandler(IvrEndpointConversationImpl conv, Call call) {
            super(term, "Long call duration handler");
            this.conv = conv;
            this.call = call;
        }   

        @Override
        public void doRun() throws Exception {
            if (conv.getState().getId()!=IvrEndpointConversationState.INVALID) {
                if (logger.isDebugEnabled())
                    logger.debug(callLog(call, "The call duration is TOO LONG. Canceling a call"));
                getAndRemoveConvHolder(call);
                conv.stopConversation(CompletionCode.CALL_DURATION_TOO_LONG);
            }
        }
    }
    
    public static class CallInfo {
        private final String callId;
        private final Date created;
        private final long duration;
        private final String description;

        public CallInfo(String callId, Date created, long duration, String description) {
            this.callId = callId;
            this.created = created;
            this.duration = duration;
            this.description = description;
        }

        public String getCallId() {
            return callId;
        }

        public Date getCreated() {
            return created;
        }

        public long getDuration() {
            return duration;
        }

        public String getDescription() {
            return description;
        }
    }
    
//    private static class StopConversationTask extends AbstractTask {
//        private final IvrEndpointConversation conversation;
//        private final Node taskNode;
//
//        public StopConversationTask(Node taskNode, IvrEndpointConversation conversation) {
//            super(taskNode, status);
//            this.conversation = conversation;
//        }
//
//        @Override
//        public void doRun() throws Exception {
//            conversation.stopConversation(CompletionCode.OPPONENT_BUSY);
//        }
//        
//    }
}
