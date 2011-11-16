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
import com.cisco.jtapi.extensions.CiscoCall;
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
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallActiveEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.CallInvalidEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.raven.ivr.RtpStreamManager;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.table.TableImpl;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
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
    private final IvrConversationScenario conversationScenario;
    private final String address;
    private final Codec codec;
    private final Integer rtpPacketSize;
    private final int rtpMaxSendAheadPacketsCount;
    private final boolean enableIncomingRtp;
    private final boolean enableIncomingCalls;
    private final IvrTerminal term;
    private final Logger logger;

    private Address termAddress;
    private int maxChannels = Integer.MAX_VALUE;
    private CiscoTerminal ciscoTerm;
    private boolean termInService = false;
    private boolean termAddressInService = false;
    Provider provider;

    private final Map<Call, ConvHolder> calls = new HashMap<Call, ConvHolder>();
    private final Map<Integer, ConvHolder> connIds = new HashMap<Integer, ConvHolder>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final IvrTerminalStateImpl state;
    private final Set<IvrEndpointConversationListener> conversationListeners =
            new HashSet<IvrEndpointConversationListener>();
    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();

    @Message private static String callIdColumnMessage;
    @Message private static String callInfoColumnMessage;
    @Message private static String endpointBusyMessage;
    @Message private static String callsCountMessage;


    public CiscoJtapiTerminal(ProviderRegistry providerRegistry
            , StateListenersCoordinator stateListenersCoordinator
            , IvrTerminal term)
    {
        this.providerRegistry = providerRegistry;
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
        this.logger = term.getLogger();
        this.state = new IvrTerminalStateImpl(term);
        stateListenersCoordinator.addListenersToState(state, IvrTerminalState.class);
        this.state.setState(IvrTerminalState.OUT_OF_SERVICE);
    }

    public IvrTerminalState getState() {
        return state;
    }

    public void start() throws IvrEndpointException {
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug("Checking provider...");
            ProviderController providerController = providerRegistry.getProviderController(address);
            provider = providerController.getProvider();
            if (provider==null || provider.getState()!=Provider.IN_SERVICE)
                throw new Exception(String.format(
                        "Provider (%s) not IN_SERVICE", providerController.getName()));
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug("Checking terminal address...");
            termAddress = provider.getAddress(address);
            ciscoTerm = registerTerminal(termAddress);
            registerTerminalListeners();
        } catch (Throwable e) {
            throw new IvrEndpointException("Problem with starting endpoint", e);
        }
    }

    public void stop() {
        resetListeners();
        unregisterTerminal();
        unregisterTerminalListeners();
    }

    private void resetListeners() {
        listenersLock.writeLock().lock();
        try {
            conversationListeners.clear();
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    public void invite(String opponentNum, int inviteTimeout, IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings)
    {
        lock.writeLock().lock();
        try {
            Call call = null;
            try {
                if (state.getId()!=IvrTerminalState.IN_SERVICE)
                    throw new Exception("Can't invite oppenent to conversation. Terminal not ready");
                if (calls.size()>=maxChannels)
                    throw new Exception("Can't invite oppenent to conversation. Too many opened channels");
                call = provider.createCall();
                IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(term, executor, scenario
                        , rtpStreamManager, enableIncomingRtp, bindings);
                conv.addConversationListener(listener);
                ConvHolder holder = new ConvHolder(conv, false);
                calls.put(call, holder);
                call.connect(ciscoTerm, termAddress, opponentNum);
                if (inviteTimeout>0) 
                    executor.execute(inviteTimeout*1000, new inviteTimeoutHandler(conv, call));
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    logger.warn(String.format("Problem with inviting abonent with number (%s)", opponentNum), e);
                if (call!=null)
                    stopConversation(call, CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<ViewableObject> getViewableObjects() throws Exception {
        ViewableObject obj = null;
        int callsCount = 0;
        if (lock.readLock().tryLock(500, TimeUnit.MILLISECONDS)){
            try{
                callsCount = calls.size();
                TableImpl table = new TableImpl(new String[]{callIdColumnMessage, callInfoColumnMessage});
                for (Map.Entry entry: calls.entrySet())
                    table.addRow(new Object[]{entry.getKey().toString(), entry.getValue().toString()});
                obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
            }finally{
                lock.readLock().unlock();
            }
        } else {
            obj = new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, endpointBusyMessage);
        }
        ViewableObject callsCountText = new ViewableObjectImpl(
                Viewable.RAVEN_TEXT_MIMETYPE, String.format(callsCountMessage, callsCount));
        return Arrays.asList(obj, callsCountText);
    }

    private CiscoTerminal registerTerminal(Address addr) throws Exception {
        Terminal[] terminals = addr.getTerminals();
        if (terminals==null || terminals.length==0)
            throw new Exception(String.format("Address (%s) does not have terminals", address));
        Terminal terminal = terminals[0];
        if (terminal instanceof CiscoRouteTerminal) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug("Registering {} terminal", CiscoRouteTerminal.class.getName());
            CiscoRouteTerminal routeTerm = (CiscoRouteTerminal) terminal;
            routeTerm.register(codec.getCiscoMediaCapabilities(), CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
            return routeTerm;
        } else if (terminal instanceof CiscoMediaTerminal) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug("Registering {} terminal", CiscoMediaTerminal.class.getName());
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

    private void unregisterTerminal() {
        try {
            if (ciscoTerm instanceof CiscoRouteTerminal)
                ((CiscoRouteTerminal)ciscoTerm).unregister();
            else if (ciscoTerm instanceof CiscoMediaTerminal)
                ((CiscoMediaTerminal)ciscoTerm).unregister();
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error("Problem with unregistering terminal", e);
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
            if (isLogLevelEnabled(LogLevel.WARN))
                logger.warn("Problem with unregistering listeners from the cisco terminal", e);
        }
    }

    public void terminalChangedEvent(TermEv[] events) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
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
        if (isLogLevelEnabled(LogLevel.DEBUG))
            logger.debug("Recieved call events: "+eventsToString(events));
        for (CallEv ev: events)
            switch (ev.getID()) {
                case CallActiveEv.ID: createConversation(((CallActiveEv)ev).getCall()); break;
                case ConnConnectedEv.ID: bindConnIdToConv((ConnConnectedEv)ev); break;
                case CallCtlConnOfferedEv.ID: acceptIncomingCall((CallCtlConnOfferedEv) ev); break;
                case TermConnRingingEv.ID   : answerOnIncomingCall((TermConnRingingEv)ev); break;
                case MediaTermConnDtmfEv.ID: continueConv((MediaTermConnDtmfEv)ev); break;
                case ConnDisconnectedEv.ID: unbindConnIdFromConv((ConnDisconnectedEv)ev); break;
                case CallCtlConnFailedEv.ID: handleConnFailedEvent((CallCtlConnFailedEv)ev); break;
                case CallInvalidEv.ID: stopConversation(ev.getCall(), CompletionCode.COMPLETED_BY_OPPONENT); break;
            }
    }
    
    public void addressChangedEvent(AddrEv[] events) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            logger.debug("Recieved address events: "+eventsToString(events));
        for (AddrEv ev: events)
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID: termAddressInService = true; checkState(); break;
                case CiscoAddrOutOfServiceEv.ID: termAddressInService = false; checkState(); break;
            }
    }

    private void createConversation(Call call) {
        lock.writeLock().lock();
        try {
            try {
                ConvHolder holder = calls.get(call);
                if (holder==null && enableIncomingCalls && calls.size()<=maxChannels) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(callLog(call, "Creating conversation"));
                    IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(
                            term, executor, conversationScenario, rtpStreamManager, enableIncomingRtp, null);
                    conv.setCall((CallControlCall) call);
                    conv.addConversationListener(this);
                    calls.put(call, new ConvHolder(conv, true));
                } else if (holder!=null && !holder.incoming) 
                    holder.conv.setCall((CallControlCall) call);
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    logger.error("Error creating conversation", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void bindConnIdToConv(ConnConnectedEv ev) {
        lock.writeLock().lock();
        try {
            if (ev.getConnection().getAddress().getName().equals(address)) {
                ConvHolder conv = calls.get(ev.getCall());
                if (conv!=null) {
                    connIds.put(((CiscoConnection)ev.getConnection()).getConnectionID().intValue(), conv);
                    if (isLogLevelEnabled(LogLevel.DEBUG))
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
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    logger.debug(callLog(ev.getCall(), "Unbinding connection ID from the conversation"));
                connIds.remove(((CiscoConnection)ev.getConnection()).getConnectionID().intValue());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void acceptIncomingCall(CallCtlConnOfferedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv==null || !conv.incoming) //|| !conv.incoming
            return;
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug(callLog(ev.getCall(), "Accepting call"));
            ((CallControlConnection)ev.getConnection()).accept();
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.WARN))
                logger.error(callLog(ev.getCall(), "Problem with accepting call"), e);
        }
    }

    private void answerOnIncomingCall(TermConnRingingEv ev) {
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug(callLog(ev.getCall(), "Answering on call"));
            ev.getTerminalConnection().answer();
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(callLog(ev.getCall(), "Problem with answering on call"), e);
        }
    }
    
    private void initInRtp(CiscoMediaOpenLogicalChannelEv ev) {
        ConvHolder conv = getConvHolderByConnId(ev.getCiscoRTPHandle().getHandle());
        if (conv==null)
            return;
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug("Initializing incoming RTP stream");
            IncomingRtpStream rtp = conv.conv.initIncomingRtp();
            CiscoRTPParams params = new CiscoRTPParams(rtp.getAddress(), rtp.getPort());
            if (ev.getTerminal() instanceof CiscoMediaTerminal)
                ((CiscoMediaTerminal)ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
            else if (ev.getTerminal() instanceof CiscoRouteTerminal)
                ((CiscoRouteTerminal)ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error("Error initializing incoming RTP stream", e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void initAndStartOutRtp(CiscoRTPOutputStartedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv==null)
            return;
        try {
            CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                    logger.debug(callLog(ev.getCallID().getCall(),
                            "Proposed RTP params: remoteHost (%s), remotePort (%s), packetSize (%s), " +
                            "payloadType (%s), bitrate (%s)"
                            , props.getRemoteAddress().toString(), props.getRemotePort()
                            , props.getPacketSize()*8, props.getPayloadType(), props.getBitRate()));
            Integer psize = rtpPacketSize;
            if (psize==null)
                psize = props.getPacketSize()*8;
            Codec streamCodec = Codec.getCodecByCiscoPayload(props.getPayloadType());
            if (streamCodec==null)
                throw new Exception(String.format(
                        "Not supported payload type (%s)", props.getPayloadType()));
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug(callLog(ev.getCallID().getCall()
                    ,"Choosed RTP params: packetSize (%s), codec (%s), audioFormat (%s)"
                    , psize, streamCodec, streamCodec.getAudioFormat()));
            conv.conv.initOutgoingRtp(props.getRemoteAddress().getHostAddress(), props.getRemotePort()
                    , psize, rtpMaxSendAheadPacketsCount, streamCodec);
            conv.conv.startOutgoingRtp();
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(callLog(ev.getCallID().getCall()
                        ,"Error initializing and starting outgoing RTP stream"), e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void startInRtp(CiscoRTPInputStartedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv==null)
            return;
        try {
            conv.conv.startIncomingRtp();
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                logger.error(callLog(ev.getCallID().getCall(), "Problem with start incoming RTP stream"), e);
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
            conv.conv.stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
    }

    private static String callLog(Call call, String message, Object... args) {
        return getCallDesc((CiscoCall)call)+" : Terminal. "+String.format(message, args);
    }

    private static String getCallDesc(CiscoCall call){
        return "[call id: "+call.getCallID().intValue()+", calling number: "+call.getCallingAddress().getName()+"]";
    }
    
    private String eventsToString(Object[] events) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append(i > 0 ? ", " : "").append(events[i].toString());
        return buf.toString();
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

    private ConvHolder getAndRemoveConvHolder(Call call) {
        lock.writeLock().lock();
        try {
            return calls.remove(call);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isLogLevelEnabled(LogLevel logLevel) {
        return term.isLogLevelEnabled(logLevel);
//        return this.logLevel.ordinal() <= logLevel.ordinal();
    }

    private synchronized void checkState() {
        if (state.getId()==IvrTerminalState.OUT_OF_SERVICE) {
            if (termInService && termAddressInService)
                state.setState(IvrTerminalState.IN_SERVICE);
        } else if (!termAddressInService || !termInService)
            state.setState(IvrTerminalState.OUT_OF_SERVICE);
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
    //--------------- End of the IvrEndpointConversationListener methods -----------------//

    private class ConvHolder {
        private final IvrEndpointConversationImpl conv;
        private final boolean incoming;

        public ConvHolder(IvrEndpointConversationImpl conv, boolean incoming) {
            this.conv = conv;
            this.incoming = incoming;
        }
    }

    private abstract class MethodCaller {
        public abstract void callMethod(IvrEndpointConversationListener listener);
    }

    private class inviteTimeoutHandler extends AbstractTask {
        private final IvrEndpointConversationImpl conv;
        private final Call call;

        public inviteTimeoutHandler(IvrEndpointConversationImpl conv, Call call) {
            super(term, "Invite timeout handler for call");
            this.conv = conv;
            this.call = call;
        }

        @Override
        public void doRun() throws Exception {
            if (ObjectUtils.in(conv.getState().getId(), IvrEndpointConversationState.READY
                    , IvrEndpointConversationState.CONNECTING))
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    logger.debug(callLog(call, "Detected INVITE TIMEOUT. Canceling a call"));
                conv.stopConversation(CompletionCode.OPPONENT_NOT_ANSWERED);
            }
        }
    }
}
