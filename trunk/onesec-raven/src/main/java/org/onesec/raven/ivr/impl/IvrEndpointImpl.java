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
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointImpl implements CiscoTerminalObserver, AddressObserver, CallControlCallObserver
        , MediaCallObserver
{
    private final ProviderRegistry providerRegistry;
    private final StateListenersCoordinator stateListenersCoordinator;

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

    private final Map<Call, ConvHolder> calls = new HashMap<Call, ConvHolder>();
    private final Map<Integer, ConvHolder> connIds = new HashMap<Integer, ConvHolder>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointImpl(ProviderRegistry providerRegistry
            , StateListenersCoordinator stateListenersCoordinator
            , IvrTerminal term)
    {
        this.providerRegistry = providerRegistry;
        this.stateListenersCoordinator = stateListenersCoordinator;
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
        unregisterTerminal();
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
            if (term.isLogLevelEnabled(LogLevel.WARN))
                term.getLogger().warn("Problem with unregistering listeners from the cisco terminal", e);
        }
    }

    public void terminalChangedEvent(TermEv[] events) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            logger.debug("Recieved terminal events: "+eventsToString(events));
        for (TermEv ev: events)
            switch (ev.getID()) {
                case CiscoMediaOpenLogicalChannelEv.ID: initInRtp((CiscoMediaOpenLogicalChannelEv)ev); break;
                case CiscoRTPOutputStartedEv.ID: initAndStartOutRtp((CiscoRTPOutputStartedEv) ev); break;
                case CiscoRTPInputStartedEv.ID: startInRtp((CiscoRTPInputStartedEv)ev); break;
                case CiscoRTPOutputStoppedEv.ID: stopOutRtp((CiscoRTPOutputStoppedEv)ev); break;
                case CiscoRTPInputStoppedEv.ID: stopInRtp((CiscoRTPInputStoppedEv)ev); break;
            }
    }

    public void callChangedEvent(CallEv[] events) {
        if (term.isLogLevelEnabled(LogLevel.DEBUG))
            term.getLogger().debug("Recieved call events: "+eventsToString(events));
        for (CallEv ev: events)
            switch (ev.getID()) {
                case CallActiveEv.ID: createConversation(((CallActiveEv)ev).getCall()); break;
                case ConnConnectedEv.ID: bindConnIdToConversation((ConnConnectedEv)ev); break;
                case CallCtlConnOfferedEv.ID: acceptIncomingCall((CallCtlConnOfferedEv) ev); break;
                case TermConnRingingEv.ID   : answerOnIncomingCall((TermConnRingingEv)ev); break;
                case MediaTermConnDtmfEv.ID: continueConv((MediaTermConnDtmfEv)ev); break;
                case ConnDisconnectedEv.ID: unbindConnIdToConversation((ConnDisconnectedEv)ev); break;
                case CallCtlConnFailedEv.ID: handleConnFailedEvent((CallCtlConnFailedEv)ev); break;
                case CallInvalidEv.ID: stopConversation((CallInvalidEv)ev); break;
            }
    }
    
    public void addressChangedEvent(AddrEv[] events) {
    }

    private void createConversation(Call call) {
        lock.writeLock().lock();
        try {
            try {
                if (!calls.containsKey(call) && enableIncomingCalls && calls.size()<=maxChannels) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(callLog(call, "Creating conversation"));
                    IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(
                            term, executor, conversationScenario, rtpStreamManager, enableIncomingRtp, null);
                    calls.put(call, new ConvHolder(conv, true));
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
        lock.writeLock().lock();
        try {
            if (ev.getConnection().getAddress().getName().equals(address)) {
                ConvHolder conv = calls.get(ev.getCall());
                if (conv!=null) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        logger.debug(callLog(ev.getCall(), "Connection ID binded to the conversation"));
                    connIds.put(((CiscoConnection)ev.getConnection()).getConnectionID().intValue(), conv);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void unbindConnIdToConversation(ConnDisconnectedEv ev) {
        if (address.equals(ev.getConnection().getAddress().getName())) {
            lock.writeLock().lock();
            try {
                connIds.remove(((CiscoConnection)ev.getConnection()).getConnectionID().intValue());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void acceptIncomingCall(CallCtlConnOfferedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv==null || !conv.incoming)
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
                logger.debug("Answering on call");
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
    
    private void stopConversation(CallInvalidEv ev) {
        ConvHolder conv = getAndRemoveConvHolder(ev.getCall());
        if (conv!=null)
            conv.conv.stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
    }

    private static String callLog(Call call, String message, Object... args) {
        return getCallDesc((CiscoCall)call)+" : "+String.format(message, args);
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
    }

    private class ConvHolder {
        private final IvrEndpointConversationImpl conv;
        private final boolean incoming;

        public ConvHolder(IvrEndpointConversationImpl conv, boolean incoming) {
            this.conv = conv;
            this.incoming = incoming;
        }
    }
}
