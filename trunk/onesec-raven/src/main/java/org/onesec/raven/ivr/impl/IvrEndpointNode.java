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

package org.onesec.raven.ivr.impl;

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoMediaOpenLogicalChannelEv;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPInputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPParams;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.cisco.jtapi.extensions.CiscoUnregistrationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
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
import javax.telephony.callcontrol.events.CallCtlTermConnDroppedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.AddrObservationEndedEv;
import javax.telephony.events.CallActiveEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.CallObservationEndedEv;
import javax.telephony.events.TermConnDroppedEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.events.TermObservationEndedEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.onesec.core.ObjectDescription;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.Operator;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationCdr;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.IvrTerminalState;
import org.onesec.raven.ivr.RtpAddress;
import org.onesec.raven.ivr.TerminalStateMonitoringService;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrEndpointNode extends AbstractEndpointNode
        implements IvrEndpoint, IvrEndpointConversationListener
{
//    private final static int LOCK_TIMEOUT = 500;
//
//    @Service
//    protected static ProviderRegistry providerRegistry;
//
//    @Service
//    protected static StateListenersCoordinator stateListenersCoordinator;
//
//    @Service
//    protected static Operator operator;
//
//    @Service
//    protected static TerminalStateMonitoringService terminalStateMonitoringService;
//
//    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
//    private RtpStreamManagerNode rtpStreamManager;
//
//    @NotNull @Parameter
//    private String address;
//
//    @Parameter
//    private IvrConversationScenarioNode conversationScenario;
//
//    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
//    private ExecutorService executor;
//
//    @NotNull @Parameter(defaultValue="false")
//    private Boolean enableIncomingRtp;
//
//    //ip address and port for outgoing rtp stream
//    private RtpAddress rtpAddress;
//
//    @NotNull @Parameter(defaultValue="AUTO")
//    private Codec codec;
//
//    @Parameter
//    private Integer rtpPacketSize;
//
//    @NotNull @Parameter(defaultValue="5")
//    private Integer rtpInitialBuffer;
//
//    @NotNull @Parameter(defaultValue="0")
//    private Integer rtpMaxSendAheadPacketsCount;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean enableIncomingCalls;

    private IvrEndpointStateImpl endpointState;
//    private Address terminalAddress;
//    CiscoMediaTerminal terminal;
//    Call call;
//
//    private int callId;
//    private boolean terminalInService;
//    private boolean terminalAddressInService;
//    private boolean handlingOutgoingCall;
//    private AtomicBoolean observingCall;
//    private AtomicBoolean observingTerminalAddress;
//    private AtomicBoolean observingTerminal;
//    private Provider provider;
//    private ConversationCdrImpl conversationResult;
//    private ConversationCompletionCallback completionCallback;
//    private IvrEndpointConversationImpl conversation;
//    private ReentrantLock lock;
//    private Set<IvrEndpointConversationListener> conversationListeners;
//    private ReadWriteLock listenersLock;
//
//    private Call conversationCall;

    @Override
    protected void initFields()
    {
        super.initFields();
//        terminal = null;
//        terminalAddress = null;
        endpointState = new IvrEndpointStateImpl(this);
        stateListenersCoordinator.addListenersToState(endpointState, IvrEndpointState.class);
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
//        lock = new ReentrantLock();
//        conversationListeners = new HashSet<IvrEndpointConversationListener>();
//        listenersLock = new ReentrantReadWriteLock();
//        resetStates();
//        resetConversationFields();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
    }

//    public void resetStates()
//    {
//        terminalInService = false;
//        terminalAddressInService = false;
//        handlingOutgoingCall = false;
//        observingTerminal = new AtomicBoolean(false);
//        observingTerminalAddress = new AtomicBoolean(false);
//        observingCall = new AtomicBoolean(false);
//        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
//    }
//
//    private void resetConversationFields()
//    {
//        handlingOutgoingCall = false;
//        conversationResult = null;
//        completionCallback = null;
//        callId = 0;
//        conversation = null;
//    }
//
    @Override
    protected void doStart() throws Exception {
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
        super.doStart();
//        resetConversationFields();
//        initializeEndpoint();
    }

    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings)
    {
        CiscoJtapiTerminal _term = term.get();
        if (_term!=null)
            _term.invite(opponentNum, inviteTimeout, maxCallDur, listener, scenario, bindings);
        else
            listener.conversationStopped(new IvrEndpointConversationStoppedEventImpl(
                    null, CompletionCode.OPPONENT_UNKNOWN_ERROR));
    }
    
    private synchronized void changeStateTo(int stateId, String stateName) {
        boolean invalidTrans = false;
        switch (stateId) {
            case IvrEndpointState.OUT_OF_SERVICE: endpointState.setState(stateId); break;
            case IvrEndpointState.IN_SERVICE: endpointState.setState(stateId); break;
            case IvrEndpointState.TALKING:
                if (endpointState.getId()!=IvrEndpointState.OUT_OF_SERVICE)
                    endpointState.setState(stateId);
                else
                    invalidTrans = true;
                break;
            case IvrEndpointState.INVITING:
                if (endpointState.getId()==IvrEndpointState.IN_SERVICE)
                    endpointState.setState(stateId);
                else
                    invalidTrans = true;
                break;
        }
        if (invalidTrans && isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("Invalid state transition. Can't change state from (%s) to (%s)",
                    endpointState.getIdName(), stateName);

    }

    @Override
    protected void terminalCreated(CiscoJtapiTerminal terminal) {
        terminal.addConversationListener(this);
    }

    @Override
    protected void terminalStateChanged(IvrTerminalState state) {
        changeStateTo(state.getId(), state.getIdName());
    }

    @Override
    protected void terminalStopped(CiscoJtapiTerminal terminal) {
        terminal.removeConversationListener(this);
    }

    @Override
    protected String getEndpointStateAsString() {
        return endpointState.getIdName();
    }

    public void conversationStarted(IvrEndpointConversationEvent event) {
        changeStateTo(IvrEndpointState.TALKING, "TALKING");
    }

    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
        changeStateTo(IvrEndpointState.IN_SERVICE, "IN_SERVICE");
    }

    public void conversationTransfered(IvrEndpointConversationTransferedEvent event) { }

    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }

    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }

    public void listenerAdded(IvrEndpointConversationEvent event) { }

//    @Override
//    protected void doStop() throws Exception
//    {
//        super.doStop();
//        stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
//        unregisterTerminal();
//        removeCallObserver();
//        removeTerminalAddressObserver();
//        removeTerminalObserver();
//        terminal = null;
//        terminalAddress = null;
//        resetStates();
//    }

//    private void removeCallObserver() {
//        try {
//            if (observingCall.get())
//                terminalAddress.removeCallObserver(this);
//        } catch(Throwable e) {
//            if (isLogLevelEnabled(LogLevel.WARN))
//                warn("Error removing call observer", e);
//        }
//    }
//
//    private void removeTerminalAddressObserver() {
//        try {
//            if (observingTerminalAddress.get())
//                terminalAddress.removeObserver(this);
//        } catch(Throwable e) {
//            if (isLogLevelEnabled(LogLevel.WARN))
//                warn("Error removing terminal address observer", e);
//        }
//    }
//
//    private void removeTerminalObserver() {
//        try {
//            if (observingTerminal.get())
//                terminal.removeObserver(this);
//        } catch(Throwable e) {
//            if (isLogLevelEnabled(LogLevel.WARN))
//                warn("Error removing terminal observer", e);
//        }
//    }
//
//    private synchronized void initializeEndpoint() throws Exception {
//        try {
//            endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
//            resetStates();
//            if (isLogLevelEnabled(LogLevel.DEBUG))
//                debug("Checking provider...");
//            ProviderController providerController = providerRegistry.getProviderController(address);
//            provider = providerController.getProvider();
//            if (provider==null || provider.getState()!=Provider.IN_SERVICE)
//                throw new Exception(String.format(
//                        "Provider (%s) is not IN_SERVICE", providerController.getName()));
//            if (isLogLevelEnabled(LogLevel.DEBUG))
//                debug("Checking terminal address...");
//            terminalAddress = provider.getAddress(address);
//            Terminal[] terminals = terminalAddress.getTerminals();
//            if (terminals==null || terminals.length==0)
//                throw new Exception(String.format("Address (%s) does not have terminals", address));
//            if (!(terminals[0] instanceof CiscoMediaTerminal))
//                throw new Exception(String.format(
//                        "Invalid terminal type (%s). The terminal must be instance of (%s) " +
//                        "(CTI_Port on the CCM side)"
//                        , terminals[0].getName(), CiscoMediaTerminal.class.getName()));
//            terminal = (CiscoMediaTerminal) terminals[0];
//            terminal.register(codec.getCiscoMediaCapabilities());
//            terminal.addObserver(this);
//            observingTerminal.set(true);
//            terminalAddress.addObserver(this);
//            observingTerminalAddress.set(true);
//            terminalAddress.addCallObserver(this);
//            observingCall.set(true);
//        } catch(Exception e) {
//            throw new Exception(
//                    String.format("Error initializing IVR endpoint (%s)", getPath()), e);
//        }
//    }

////    public void invite(
////            String opponentNumber, IvrConversationScenario conversationScenario
////            , ConversationCompletionCallback callback
////            , Map<String, Object> bindings) throws IvrEndpointException
////    {
////
////        try {
////            if (lock.tryLock()) {
////                try {
////                    if (   !getStatus().equals(Status.STARTED)
////                        && endpointState.getId()!=IvrEndpointState.IN_SERVICE)
////                    {
////                        throw new IvrEndpointException(
////                                "Can't invite oppenent to conversation. Endpoint not ready");
////                    }
////                    try {
////                        endpointState.setState(IvrEndpointState.INVITING);
////                        if (isLogLevelEnabled(LogLevel.DEBUG))
////                            debug(String.format(
////                                    "Inviting opponent with number (%s) to conversation (%s)"
////                                    , opponentNumber, conversationScenario));
////                        resetConversationFields();
////                        handlingOutgoingCall = true;
////
////                        conversation = new IvrEndpointConversationImpl(
////                                this, executor, conversationScenario, rtpStreamManager
////                                , enableIncomingRtp, bindings);
////                        conversation.addConversationListener(this);
////
////                        conversationResult = new ConversationCdrImpl();
////                        conversationResult.setCallStartTime(System.currentTimeMillis());
////                        completionCallback = callback;
////                        Call call = provider.createCall();
//////                        CallControlCall ciscoCall = (CiscoCall) call;
////                        call.connect(terminal, terminalAddress, opponentNumber);
////                        if (isLogLevelEnabled(LogLevel.DEBUG))
////                            debug("Invite process successfully started");
////                    } catch (Throwable e) {
////                        if (isLogLevelEnabled(LogLevel.DEBUG))
////                            debug(String.format(
////                                    "Error inviting opponent (%s) to conversation", opponentNumber)
////                                , e);
////                        if (conversation!=null)
////                            //it is neñessary to close incoming rtp stream.
////                            conversation.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
////                        //stopConversation will not fire the conversationStopped event so we call this event
////                        //manually
////                        conversationStopped(new IvrEndpointConversationStoppedEventImpl(
////                                conversation, CompletionCode.OPPONENT_UNKNOWN_ERROR));
////                    }
////                } finally {
////                    lock.unlock();
////                }
////            } else {
////                throw new IvrEndpointException(
////                        "Can't invite oppenent to conversation. Endpoint not ready");
////            }
////        } catch (IvrEndpointException e) {
////            if (isLogLevelEnabled(LogLevel.WARN))
////                getLogger().warn(e.getMessage(), e);
////            throw e;
////        }
////    }
//
//    private synchronized void checkStatus() {
//        if (endpointState.getId()==IvrEndpointState.OUT_OF_SERVICE) {
//            if (terminalInService && terminalAddressInService)
//                endpointState.setState(IvrEndpointState.IN_SERVICE);
//        } else if (!terminalAddressInService || !terminalInService)
//            endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
//    }

//    @Parameter(readOnly=true)
//    public String getState() {
//        return endpointState.getIdName();
//    }

    public IvrEndpointState getEndpointState() {
        return endpointState;
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

//    public RtpStreamManagerNode getRtpStreamManager() {
//        return rtpStreamManager;
//    }
//
//    public void setRtpStreamManager(RtpStreamManagerNode rtpStreamManager) {
//        this.rtpStreamManager = rtpStreamManager;
//    }
//
//    public String getAddress() {
//        return address;
//    }
//
//    public RtpAddress getRtpAddress() {
//        return rtpAddress!=null? rtpAddress : null;
//    }
//
//    public void setAddress(String address) {
//        this.address = address;
//    }
//
//    public Integer getRtpInitialBuffer() {
//        return rtpInitialBuffer;
//    }
//
//    public void setRtpInitialBuffer(Integer rtpInitialBuffer) {
//        this.rtpInitialBuffer = rtpInitialBuffer;
//    }
//
//    public Integer getRtpMaxSendAheadPacketsCount() {
//        return rtpMaxSendAheadPacketsCount;
//    }
//
//    public void setRtpMaxSendAheadPacketsCount(Integer rtpMaxSendAheadPacketsCount) {
//        this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;
//    }
//
//    public Codec getCodec() {
//        return codec;
//    }
//
//    public void setCodec(Codec codec) {
//        this.codec = codec;
//    }
//
//    public Integer getRtpPacketSize() {
//        return rtpPacketSize;
//    }
//
//    public void setRtpPacketSize(Integer rtpPacketSize) {
//        this.rtpPacketSize = rtpPacketSize;
//    }
//
//    public IvrConversationScenarioNode getConversationScenario() {
//        return conversationScenario;
//    }
//
//    public void setConversationScenario(IvrConversationScenarioNode conversationNode) {
//        this.conversationScenario = conversationNode;
//    }

    public Boolean getEnableIncomingCalls() {
        return enableIncomingCalls;
    }

    public void setEnableIncomingCalls(Boolean enableIncomingCalls) {
        this.enableIncomingCalls = enableIncomingCalls;
    }

//    public void terminalChangedEvent(TermEv[] events) {
//        if (isLogLevelEnabled(LogLevel.DEBUG))
//            debug("Recieved terminal events: "+eventsToString(events));
//        for (TermEv event: events)
//            switch (event.getID()) {
//                case CiscoTermInServiceEv.ID: terminalInService = true; checkStatus(); break;
//                case CiscoTermOutOfServiceEv.ID: terminalInService = false; checkStatus(); break;
//                case CiscoMediaOpenLogicalChannelEv.ID:
//                    initIncomingRtp((CiscoMediaOpenLogicalChannelEv)event);
////                    openLogicalChannel((CiscoMediaOpenLogicalChannelEv)event);
//                    break;
//                case CiscoRTPOutputStartedEv.ID:
//                    initAndStartOutgoingRtp((CiscoRTPOutputStartedEv) event); break;
////                    initConversation((CiscoRTPOutputStartedEv) event); break;
//                case CiscoRTPInputStartedEv.ID: startIncomingRtp(); break;
//                case CiscoRTPOutputStoppedEv.ID: stopOutgoingRtp(); break;
//                case CiscoRTPInputStoppedEv.ID: stopIncomingRtp(); break;
//                case TermObservationEndedEv.ID: observingTerminal.set(false); break;
//            }
//    }
//
//    public void addressChangedEvent(AddrEv[] events) {
//        if (isLogLevelEnabled(LogLevel.DEBUG))
//            debug("Recieved address events: "+eventsToString(events));
//        for (AddrEv event: events)
//        {
//            switch (event.getID())
//            {
//                case CiscoAddrInServiceEv.ID:
//                    terminalAddressInService = true; checkStatus(); break;
//                case CiscoAddrOutOfServiceEv.ID:
//                    terminalAddressInService = false; checkStatus(); break;
//                case AddrObservationEndedEv.ID:
//                    observingTerminalAddress.set(false); break;
//            }
//        }
//    }
//
//    public void callChangedEvent(CallEv[] events) {
//        if (isLogLevelEnabled(LogLevel.DEBUG))
//            debug("Recieved call events: "+eventsToString(events));
//        for (CallEv event: events)
//        {
//            switch (event.getID())
//            {
//                case CallActiveEv.ID: createConversation(((CallActiveEv)event).getCall()); break;
//                case CallCtlConnOfferedEv.ID: acceptIncomingCall((CallCtlConnOfferedEv) event); break;
//                case TermConnRingingEv.ID: answerOnIncomingCall((TermConnRingingEv)event); break;
////                case CallCtlConnEstablishedEv.ID: startConversation(); break;
//                case TermConnDroppedEv.ID: stopConversation(CompletionCode.COMPLETED_BY_OPPONENT); break;
//                case MediaTermConnDtmfEv.ID:
//                    continueConversation(((MediaTermConnDtmfEv)event).getDtmfDigit());
//                    break;
//                case CallObservationEndedEv.ID:
//                    observingCall.set(false);
//                    break;
//                case CallCtlConnFailedEv.ID:
//                    if (handlingOutgoingCall)
//                    {
//                        CallCtlConnFailedEv ev = (CallCtlConnFailedEv) event;
//                        int cause = ev.getCallControlCause();
////                        System.out.println("        >>> CAUSE: "+cause+" : "+ev.getCause());
//                        CompletionCode code = CompletionCode.OPPONENT_UNKNOWN_ERROR;
//                        switch (cause)
//                        {
//                            case CallCtlConnFailedEv.CAUSE_BUSY:
//                                code = CompletionCode.OPPONENT_BUSY;
//                                break;
//                            case CallCtlConnFailedEv.CAUSE_CALL_NOT_ANSWERED:
//                            case CallCtlConnFailedEv.CAUSE_NORMAL:
//                                code = CompletionCode.OPPONENT_NOT_ANSWERED;
//                                break;
//                        }
//                        stopConversation(code);
//                    }
//                    break;
//                case CallCtlTermConnDroppedEv.ID:
//                    CallCtlTermConnDroppedEv ev = (CallCtlTermConnDroppedEv) event;
////                    switch (ev.getCause())
////                    {
////                        case CallCtlTermConnDroppedEv.CAUSE_NORMAL :
////                            System.out.println("    >>>> NORMAL <<<< :"+ev.getCause()); break;
////                        case CallCtlTermConnDroppedEv.CAUSE_CALL_CANCELLED :
////                            System.out.println("    >>>> CANCELED <<<< :"+ev.getCause()); break;
////                        default: System.out.println("    >>>> UNKNOWN <<<< :"+ev.getCause());
////                    }
//                    break;
//            }
//        }
//    }

//    private void acceptIncomingCall(CallCtlConnOfferedEv event)
//    {
//        try {
//            if (isLogLevelEnabled(LogLevel.DEBUG))
//                getLogger().debug("Accepting incoming call");
//            if (lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                try {
//                    if (!handlingOutgoingCall && enableIncomingCalls) {
//                        CallCtlConnOfferedEv offEvent = (CallCtlConnOfferedEv)event;
//                        CallControlConnection conn = (CallControlConnection) offEvent.getConnection();
//                            conn.accept();
//                            endpointState.setState(IvrEndpointState.ACCEPTING_CALL);
//                            if (isLogLevelEnabled(LogLevel.DEBUG))
//                                getLogger().debug("Call accepted");
//                    } else if (isLogLevelEnabled(LogLevel.DEBUG)) {
//                        if (!enableIncomingCalls)
//                            getLogger().debug("Can't accept. Handling of incoming calls are disabled");
//                        if (handlingOutgoingCall)
//                            getLogger().debug("Can't accept. Already handling a call");
//                    }
//                } finally {
//                    lock.unlock();
//                }
//            } else if (isLogLevelEnabled(LogLevel.WARN)) {
//                getLogger().warn("Lock wait timeout while accepting call");
//
//            }
//        } catch(Exception e) {
//            if (isLogLevelEnabled(LogLevel.DEBUG))
//                getLogger().debug("Lock wait error", e);
//        }
//    }
//
//    private void answerOnIncomingCall(TermConnRingingEv event) {
//        lock.lock();
//        try {
//            if (isLogLevelEnabled(LogLevel.DEBUG)) {
//                debug("Answering on incoming call");
//
//            }
//            try {
//                event.getTerminalConnection().answer();
//            } catch (Exception e) {
//                if (isLogLevelEnabled(LogLevel.ERROR)) {
//                    error("Error answering on call", e);
//
//                }
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public void continueConversation(char dtmfChar) {
//        if (lock.tryLock()) {
//            try {
//                if (conversation!=null)
//                    conversation.continueConversation(dtmfChar);
//            } finally {
//                lock.unlock();
//            }
//        } else if (isLogLevelEnabled(LogLevel.WARN))
//            getLogger().warn(
//                    "Can't continue the conversation with dtmf ({}). Lock wait timeout", dtmfChar);
//    }
//
//    public void stopConversation(CompletionCode completionCode)
//    {
//        lock.lock();
//        try {
//            if (conversation!=null) {
//                conversation.stopConversation(completionCode);
//                conversation = null;
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private String eventsToString(Object[] events) {
//        StringBuilder buf = new StringBuilder();
//        for (int i=0; i<events.length; ++i)
//            buf.append(i > 0 ? ", " : "").append(events[i].toString());
//        return buf.toString();
//    }
//
//    public ExecutorService getExecutor() {
//        return executor;
//    }
//
//    public void setExecutor(ExecutorService executorService) {
//        this.executor = executorService;
//    }
//
//    public Boolean getEnableIncomingRtp() {
//        return enableIncomingRtp;
//    }
//
//    public void setEnableIncomingRtp(Boolean enableIncomingRtp) {
//        this.enableIncomingRtp = enableIncomingRtp;
//    }

    //TODO: use conversation event listener to change endpoint status to TALKING
//    private void startConversation() {
//        lock.lock();
//        try {
//            if (conversation!=null) {
//                conversation.startConversation();
//                if (conversation.getState().getId()==IvrEndpointConversationState.TALKING)
//                    endpointState.setState(IvrEndpointState.TALKING);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }

//    private void createConversation(Call call) {
//        lock.lock();
//        try {
//            if (conversationCall!=null) {
//                if (isLogLevelEnabled(LogLevel.WARN))
//                    getLogger().warn("Can't create conversation, because of "
//                            + "of already handling a call (id:{})", conversationCall);
//                return;
//            }
//            IvrConversationScenarioNode scenario = conversationScenario;
//            if (!handlingOutgoingCall && scenario==null) {
//                if (isLogLevelEnabled(LogLevel.WARN))
//                    getLogger().warn(
//                        "Can not open logical channel for incoming call because of "
//                        + "does not have conversation scenario");
//                return;
//            }
//            try {
//                if (!handlingOutgoingCall) {
//                    conversation = new IvrEndpointConversationImpl(
//                                    this, executor, conversationScenario
//                                    , rtpStreamManager, enableIncomingRtp, null);
//                    conversation.addConversationListener(this);
//                } else if (conversation==null) {
//                    if (isLogLevelEnabled(LogLevel.DEBUG))
//                        getLogger().debug("Can't open logical channel for outgoing call "
//                                + "because of conversation already stopped");
//                    conversationCall = null;
//                    return;
//                }
//                conversation.setCall((CallControlCall)call);
//                conversationCall = call;
//            } catch (Exception e){
//                if (isLogLevelEnabled(LogLevel.ERROR))
//                    error("Error creating conversation", e);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private void initIncomingRtp(CiscoMediaOpenLogicalChannelEv event) {
//        lock.lock();
//        try {
//            if (conversationCall==null) {
//                if (isLogLevelEnabled(LogLevel.WARN))
//                    getLogger().warn("Can't init incoming RTP because of no active conversation call");
//                return;
//            }
//            try {
//                IncomingRtpStream rtp = conversation.initIncomingRtp();
//                CiscoRTPParams params = new CiscoRTPParams(rtp.getAddress(), rtp.getPort());
//                terminal.setRTPParams(event.getCiscoRTPHandle(), params);
//            } catch (Exception e) {
//                if (isLogLevelEnabled(LogLevel.ERROR))
//                    getLogger().error("Error initializing incoming RTP stream", e);
//                conversation.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private void openLogicalChannel(CiscoMediaOpenLogicalChannelEv event) {
//        if (isLogLevelEnabled(LogLevel.DEBUG))
//            debug("Creating conversation");
//        try {
//            if (lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                try {
//                    try {
//                        if (callId>0) {
//                            if (isLogLevelEnabled(LogLevel.WARN))
//                                getLogger().warn("Can't open logical channel because "
//                                        + "of already handling a call (id:{})", callId);
//                            return;
//                        }
//                        IvrConversationScenarioNode scenario = conversationScenario;
//                        if (!handlingOutgoingCall && scenario==null) {
//                            if (isLogLevelEnabled(LogLevel.WARN))
//                                getLogger().warn(
//                                    "Can not open logical channel for incoming call because of "
//                                    + "does not have conversation scenario");
//                            return;
//                        }
//
//                        callId = event.getCiscoRTPHandle().getHandle();
//                        if (!handlingOutgoingCall) {
//                            conversation = new IvrEndpointConversationImpl(
//                                            this, executor, conversationScenario
//                                            , rtpStreamManager, enableIncomingRtp, null);
//                            conversation.addConversationListener(this);
//                        } else if (conversation==null) {
//                            if (isLogLevelEnabled(LogLevel.DEBUG))
//                                getLogger().debug("Can't open logical channel for outgoing call "
//                                        + "because of conversation already stopped");
//                            callId=0;
//                            return;
//                        }
//                        CiscoRTPParams params = new CiscoRTPParams(
//                                conversation.getIncomingRtpStream().getAddress()
//                                , conversation.getIncomingRtpStream().getPort());
//                        if (isLogLevelEnabled(LogLevel.DEBUG))
//                            debug("Conversation created. Call id - {}", callId);
//                        terminal.setRTPParams(event.getCiscoRTPHandle(), params);
//                    }catch(Exception e){
//                        callId = 0;
//                        if (conversation!=null)
//                            //it is neñessary to close incoming rtp stream
//                            conversation.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//                        conversation = null;
//                        throw e;
//                    }
//                } finally {
//                    lock.unlock();
//                }
//            }
//        } catch (Exception ex) {
//            if (isLogLevelEnabled(LogLevel.ERROR))
//                error("Error creating conversation", ex);
//        }
//    }
//
//    private void stopIncomingRtp() {
//        lock.lock();
//        try {
//            if (conversation!=null)
//                conversation.stopIncomingRtp();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private void stopOutgoingRtp() {
//        lock.lock();
//        try {
//            if (conversation!=null)
//                conversation.stopOutgoingRtp();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private void startIncomingRtp() {
//        lock.lock();
//        try {
//            if (conversation==null)
//                return;
//            try {
//                conversation.startIncomingRtp();
//            } catch (Throwable e) {
//                if (isLogLevelEnabled(LogLevel.ERROR))
//                    error("Error creating conversation", e);
//                conversation.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    private void initAndStartOutgoingRtp(CiscoRTPOutputStartedEv ev) {
//        lock.lock();
//        try {
//            if (conversation==null)
//                return;
//            try {
//                CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
//                Integer psize = rtpPacketSize;
//                if (psize==null)
//                    psize = props.getPacketSize()*8;
//                Codec streamCodec = Codec.getCodecByCiscoPayload(props.getPayloadType());
//                if (streamCodec==null)
//                    throw new Exception(String.format(
//                            "Not supported payload type (%s)", props.getPayloadType()));
//                if (isLogLevelEnabled(LogLevel.DEBUG))
//                    debug(String.format(
//                        "Choosed RTP params: packetSize (%s), codec (%s), audioFormat (%s)"
//                        , psize, streamCodec, streamCodec.getAudioFormat()));
//                conversation.initOutgoingRtp(props.getRemoteAddress().getHostAddress(), props.getRemotePort()
//                        , psize, rtpMaxSendAheadPacketsCount, streamCodec);
//                conversation.startOutgoingRtp();
//            } catch (Throwable e) {
//                if (isLogLevelEnabled(LogLevel.ERROR))
//                    getLogger().error("Error initializing and starting outgoing RTP stream", e);
//                conversation.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }

//    private void initConversation(CiscoRTPOutputStartedEv ev) {
//        lock.lock();
//        try {
//            try {
//                if (conversation!=null) {
//                    if (conversationCall!=null)
//                        getLogger().debug(String.format("%s == %s is %s", conversationCall, ev.getCallID().getCall(), conversationCall==ev.getCallID().getCall()));
//                    else
//                        conversationCall=ev.getCallID().getCall();
//                    if (IvrEndpointConversationState.INVALID==conversation.getState().getId()) {
//                        CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
//                        Integer psize = rtpPacketSize;
//                        if (psize==null)
//                            psize = props.getPacketSize()*8;
//                        Codec streamCodec = Codec.getCodecByCiscoPayload(props.getPayloadType());
//                        if (streamCodec==null)
//                            throw new Exception(String.format(
//                                    "Not supported payload type (%s)", props.getPayloadType()));
//                        if (isLogLevelEnabled(LogLevel.DEBUG))
//                            debug(String.format(
//                                "Choosed RTP params: packetSize (%s), codec (%s), audioFormat (%s)"
//                                , psize, streamCodec, streamCodec.getAudioFormat()));
//                        conversation.init(
//                                ev.getCallID().getCall()
//                                , props.getRemoteAddress().getHostAddress(), props.getRemotePort()
//                                , psize, rtpInitialBuffer, rtpMaxSendAheadPacketsCount
//                                , streamCodec);
//                        if (isLogLevelEnabled(LogLevel.DEBUG))
//                            debug("Conversation initialized");
//                    }
////                    startConversation();
//                }
//            } catch (Exception e) {
//                if (isLogLevelEnabled(LogLevel.ERROR))
//                    error("Error initializing/start conversation", e);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }

//    public String getObjectName() {
//        return getPath();
//    }
//
//    public String getObjectDescription() {
//        return getPath();
//    }
//
//    private void unregisterTerminal() {
//        try {
//            if (terminal!=null)
//                terminal.unregister();
//        } catch (CiscoUnregistrationException e) {
//            if (isLogLevelEnabled(LogLevel.ERROR))
//                error("Error unregistering terminal", e);
//        }
//    }

////    public Node getOwner() {
////        return this;
////    }
//
//    public void addConversationListener(IvrEndpointConversationListener listener) {
//        listenersLock.writeLock().lock();
//        try {
//            conversationListeners.add(listener);
//        } finally {
//            listenersLock.writeLock().unlock();
//        }
//    }
//
//    public void removeConversationListener(IvrEndpointConversationListener listener) {
//        listenersLock.writeLock().lock();
//        try {
//            conversationListeners.remove(listener);
//        } finally {
//            listenersLock.writeLock().unlock();
//        }
//    }
//
//    public void listenerAdded(IvrEndpointConversationEvent event)  {
//        listenersLock.readLock().lock();
//        try {
//            for (IvrEndpointConversationListener listener : conversationListeners)
//                listener.listenerAdded(event);
//        } finally {
//            listenersLock.readLock().unlock();
//        }
//    }
//
//    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }
//
//    public void conversationStarted(IvrEndpointConversationEvent event) {
//        listenersLock.readLock().lock();
//        try {
//            if (handlingOutgoingCall)
//                conversationResult.setConversationStartTime(System.currentTimeMillis());
//            endpointState.setState(IvrEndpointState.TALKING);
//            for (IvrEndpointConversationListener listener: conversationListeners)
//                listener.conversationStarted(event);
//        } finally {
//            listenersLock.readLock().unlock();
//        }
//    }
//
//    public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
//        if (handlingOutgoingCall) {
//            conversationResult.setTransferAddress(event.getTransferAddress());
//            conversationResult.setTransferTime(System.currentTimeMillis());
//        }
//        listenersLock.readLock().lock();
//        try {
//            for (IvrEndpointConversationListener listener: conversationListeners)
//                listener.conversationTransfered(event);
//        } finally {
//            listenersLock.readLock().unlock();
//        }
//    }
//
//    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
//        if (handlingOutgoingCall) {
//            conversationResult.setCompletionCode(event.getCompletionCode());
//            conversationResult.setCallEndTime(System.currentTimeMillis());
//        }
//        //saving state of the conversation. This needed to make conversation completion callback
//        ConversationCdr _conversationResult = conversationResult;
//        ConversationCompletionCallback _completionCallback = completionCallback;
//        boolean _handlingOutgoingCall = handlingOutgoingCall;
//        //reseting conversation state
//        resetConversationFields();
//        endpointState.setState(IvrEndpointState.IN_SERVICE);
//        //using saved state to make conversation completion callback
//        if (_handlingOutgoingCall)
//            _completionCallback.conversationCompleted(_conversationResult);
//        listenersLock.readLock().lock();
//        try {
//            for (IvrEndpointConversationListener listener: conversationListeners)
//                listener.conversationStopped(event);
//        } finally {
//            listenersLock.readLock().unlock();
//        }
//    }
}