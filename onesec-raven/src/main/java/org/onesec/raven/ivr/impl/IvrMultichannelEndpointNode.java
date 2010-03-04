/*
 *  Copyright 2010 Mikhail Titov.
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
import com.cisco.jtapi.extensions.CiscoMediaCapability;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.cisco.jtapi.extensions.CiscoUnregistrationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.InvalidStateException;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnDroppedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.AddrObservationEndedEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.CallObservationEndedEv;
import javax.telephony.events.TermConnDroppedEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.events.TermObservationEndedEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.IvrMultichannelEndpoint;
import org.onesec.raven.ivr.IvrMultichannelEndpointState;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titiov
 */
public class IvrMultichannelEndpointNode extends BaseNode 
        implements IvrMultichannelEndpoint, CiscoTerminalObserver, AddressObserver
            , CallControlCallObserver, MediaCallObserver
{
    public static final int LOCK_WAIT_TIMEOUT = 500;
    @Service
    protected static ProviderRegistry providerRegistry;

    @Service
    protected static StateListenersCoordinator stateListenersCoordinator;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private RtpStreamManagerNode rtpStreamManager;
    
    @NotNull @Parameter
    private String address;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executorService;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(defaultValue="240")
    private Integer rtpPacketSize;

    @NotNull @Parameter(defaultValue="5")
    private Integer rtpInitialBuffer;

    @NotNull @Parameter(defaultValue="5")
    private Integer rtpMaxSendAheadPacketsCount;

    private Map<Integer, IvrEndpointConversationImpl> calls;
    private ReentrantReadWriteLock callsLock; 
    private IvrMultichannelEndpointStateImpl endpointState;
    private Address terminalAddress;
    private CiscoRouteTerminal terminal;
    private Provider provider;
    private AtomicBoolean observingCall;
    private AtomicBoolean observingTerminalAddress;
    private AtomicBoolean observingTerminal;
    private boolean terminalInService;
    private boolean terminalAddressInService;

    @Override
    protected void initFields()
    {
        super.initFields();
        terminal = null;
        terminalAddress = null;
        endpointState = new IvrMultichannelEndpointStateImpl(this);
        calls = new HashMap<Integer, IvrEndpointConversationImpl>();
        callsLock = new ReentrantReadWriteLock();
        resetStates();
        stateListenersCoordinator.addListenersToState(endpointState);
    }

    public void resetStates()
    {
        terminalInService = false;
        terminalAddressInService = false;
        endpointState.setState(IvrMultichannelEndpointState.OUT_OF_SERVICE);
        observingTerminal = new AtomicBoolean(false);
        observingTerminalAddress = new AtomicBoolean(false);
        observingCall = new AtomicBoolean(false);
        calls.clear();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        initializeEndpoint();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        unregisterTerminal();
        removeCallObserver();
        removeTerminalAddressObserver();
        removeTerminalObserver();
        terminal = null;
        terminalAddress = null;
        resetStates();
    }

    @Override
    public boolean isAutoStart()
    {
        return false;
    }

    private void removeCallObserver()
    {
        try
        {
            if (observingCall.get())
                terminalAddress.removeCallObserver(this);
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Error removing call observer", e);
        }
    }

    private void removeTerminalAddressObserver()
    {
        try
        {
            if (observingTerminalAddress.get())
                terminalAddress.removeObserver(this);
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Error removing terminal address observer", e);
        }
    }

    private void removeTerminalObserver()
    {
        try
        {
            if (observingTerminal.get())
                terminal.removeObserver(this);
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Error removing terminal observer", e);
        }
    }

    private void unregisterTerminal()
    {
        try
        {
            if (terminal!=null)
                terminal.unregister();
        } catch (CiscoUnregistrationException e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
            {
                error("Error unregistering terminal", e);
            }
        }
    }

    private synchronized void initializeEndpoint() throws Exception
    {
        try
        {
            resetStates();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Checking provider...");
            ProviderController providerController = providerRegistry.getProviderController(address);
            provider = providerController.getProvider();
            if (provider==null || provider.getState()!=Provider.IN_SERVICE)
                throw new Exception(String.format(
                        "Provider (%s) not IN_SERVICE", providerController.getName()));
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Checking terminal address...");
            terminalAddress = provider.getAddress(address);
            Terminal[] terminals = terminalAddress.getTerminals();
            if (terminals==null || terminals.length==0)
                throw new Exception(String.format("Address (%s) does not have terminals", address));
            if (!(terminals[0] instanceof CiscoRouteTerminal))
                throw new Exception(String.format(
                        "Invalid terminal type (%s). The terminal must be instance of (%s) " +
                        "(CTI_Port on the CCM side)"
                        , terminals[0].getName(), CiscoRouteTerminal.class.getName()));
            terminal = (CiscoRouteTerminal) terminals[0];
            CiscoMediaCapability[] caps =
                    new CiscoMediaCapability[]{CiscoMediaCapability.G711_64K_30_MILLISECONDS};

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Registering terminal (%s)...", address));
            terminal.register(caps, CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);

            terminal.addObserver(this);
            observingTerminal.set(true);
            terminalAddress.addObserver(this);
            observingTerminalAddress.set(true);
            terminalAddress.addCallObserver(this);
            observingCall.set(true);
        }
        catch(Exception e)
        {
            throw new Exception(
                    String.format("Error initializing IVR endpoint (%s)", getPath()), e);
        }
    }

    private synchronized void checkStatus()
    {
        if (endpointState.getId()==IvrEndpointState.OUT_OF_SERVICE)
        {
            if (terminalInService && terminalAddressInService)
                endpointState.setState(IvrEndpointState.IN_SERVICE);
        }
        else if (!terminalAddressInService || !terminalInService)
        {
            endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
        }
    }

    public IvrMultichannelEndpointState getEndpointState()
    {
        return endpointState;
    }

    public String getObjectDescription() {
        return getPath();
    }

    public String getObjectName() {
        return getPath();
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Integer getRtpInitialBuffer() {
        return rtpInitialBuffer;
    }

    public void setRtpInitialBuffer(Integer rtpInitialBuffer) {
        this.rtpInitialBuffer = rtpInitialBuffer;
    }

    public Integer getRtpMaxSendAheadPacketsCount() {
        return rtpMaxSendAheadPacketsCount;
    }

    public void setRtpMaxSendAheadPacketsCount(Integer rtpMaxSendAheadPacketsCount) {
        this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;
    }

    public Integer getRtpPacketSize() {
        return rtpPacketSize;
    }

    public void setRtpPacketSize(Integer rtpPacketSize) {
        this.rtpPacketSize = rtpPacketSize;
    }

    public RtpStreamManagerNode getRtpStreamManager() {
        return rtpStreamManager;
    }

    public void setRtpStreamManager(RtpStreamManagerNode rtpStreamManager) {
        this.rtpStreamManager = rtpStreamManager;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void terminalChangedEvent(TermEv[] events)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Recieved terminal events: "+eventsToString(events));
        for (TermEv event: events)
        {
            switch (event.getID())
            {
                case CiscoTermInServiceEv.ID: terminalInService = true; checkStatus(); break;
                case CiscoTermOutOfServiceEv.ID: terminalInService = false; checkStatus(); break;
                case CiscoRTPOutputStartedEv.ID: createConversation((CiscoRTPOutputStartedEv)event); break;
//                case CiscoRTPOutputStoppedEv.ID: closeRtpSession(); break;
                case TermObservationEndedEv.ID: observingTerminal.set(false); break;
            }
        }
    }

    public void addressChangedEvent(AddrEv[] events)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Recieved address events: "+eventsToString(events));
        for (AddrEv event: events)
        {
            switch (event.getID())
            {
                case CiscoAddrInServiceEv.ID:
                    terminalAddressInService = true; checkStatus(); break;
                case CiscoAddrOutOfServiceEv.ID:
                    terminalAddressInService = false; checkStatus(); break;
                case AddrObservationEndedEv.ID:
                    observingTerminalAddress.set(false); break;
            }
        }
    }

    public void callChangedEvent(CallEv[] events)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Recieved call events: "+eventsToString(events));
        for (CallEv event: events)
        {
            switch (event.getID())
            {
                case CallCtlConnOfferedEv.ID: acceptIncomingCall((CallCtlConnOfferedEv)event); break;
                case TermConnRingingEv.ID   : answerOnIncomingCall((TermConnRingingEv)event); break;
                case CallCtlConnEstablishedEv.ID: startConversation((CallCtlConnEstablishedEv)event); break;
//                case TermConnDroppedEv.ID:
//                    stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
//                    break;
//                case MediaTermConnDtmfEv.ID:
//                    continueConversation(((MediaTermConnDtmfEv)event).getDtmfDigit());
//                    break;
//                case CallObservationEndedEv.ID:
//                    observingCall.set(false);
//                    break;
//                case CallCtlConnFailedEv.ID:
//                    if (handlingIncomingCall)
//                    {
//                        CallCtlConnFailedEv ev = (CallCtlConnFailedEv) event;
//                        int cause = ev.getCallControlCause();
//                        System.out.println("        >>> CAUSE: "+cause+" : "+ev.getCause());
//                        CompletionCode code = CompletionCode.OPPONENT_UNKNOWN_ERROR;
//                        switch (cause)
//                        {
//                            case CallCtlConnFailedEv.CAUSE_BUSY:
//                                code = CompletionCode.OPPONENT_BUSY;
//                                break;
//                            case CallCtlConnFailedEv.CAUSE_CALL_NOT_ANSWERED:
//                            case CallCtlConnFailedEv.CAUSE_NORMAL:
//                                code = CompletionCode.OPPONENT_NO_ANSWERED;
//                                break;
//                        }
//                        stopConversation(code);
//                    }
//                    break;
//                case CallCtlTermConnDroppedEv.ID:
//                    CallCtlTermConnDroppedEv ev = (CallCtlTermConnDroppedEv) event;
//                    switch (ev.getCause())
//                    {
//                        case CallCtlTermConnDroppedEv.CAUSE_NORMAL :
//                            System.out.println("    >>>> NORMAL <<<< :"+ev.getCause()); break;
//                        case CallCtlTermConnDroppedEv.CAUSE_CALL_CANCELLED :
//                            System.out.println("    >>>> CANCELED <<<< :"+ev.getCause()); break;
//                        default: System.out.println("    >>>> UNKNOWN <<<< :"+ev.getCause());
//                    }
//                    break;
            }
        }
    }

    private static String eventsToString(Object[] events)
    {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append((i>0? ", ":"")+events[i].toString());
        return buf.toString();
    }

    private void acceptIncomingCall(CallCtlConnOfferedEv event)
    {
        CiscoCall call = (CiscoCall) event.getCall();
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(callLog(call, "Accepting call"));
        try{
            ((CallControlConnection) event.getConnection()).accept();
            if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(callLog(call, "Call accpeted"));
        }catch(Exception e){
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(callLog(call, "Error accepting call"), e);
        }
    }

    private static String getCallDesc(CiscoCall call){
        return "[call id: "+call.getCallID().intValue()+", calling number: "+call.getCallingAddress().getName()+"]";
    }

    private void answerOnIncomingCall(TermConnRingingEv event)
    {
        CiscoCall call = (CiscoCall) event.getCall();
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(callLog(call, "Answering on call"));
        try{
            event.getTerminalConnection().answer();
            if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(callLog(call, "Answered"));
        }catch(Exception e){
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(callLog(call, "Error answering on call"), e);
        }
    }

    private static String callLog(Call call, String message, Object... args)
    {
        return getCallDesc((CiscoCall)call)+" : "+String.format(message, args);
    }

    private void startConversation(CallCtlConnEstablishedEv event)
    {
        try {
            if (callsLock.readLock().tryLock(LOCK_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    IvrEndpointConversationImpl conversation = getConversation(event);
                    if (conversation!=null)
                        conversation.startConversation();
                } finally {
                    callsLock.readLock().unlock();
                }
            }
        } catch (InterruptedException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(callLog(event.getCall(), "Error acquire calls read lock"), ex);
        }
    }


    private void createConversation(CiscoRTPOutputStartedEv event)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(callLog(event.getCallID().getCall(), "Creating conversation"));
        try {
            if (callsLock.writeLock().tryLock(LOCK_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    CiscoRTPOutputProperties props = event.getRTPOutputProperties();
                    IvrEndpointConversationImpl conversation = new IvrEndpointConversationImpl(
                                    this, executorService, conversationScenario
                                    , rtpStreamManager.getOutgoingRtpStream(this)
                                    , event.getCallID().getCall()
                                    , props.getRemoteAddress().getHostAddress(), props.getRemotePort());
                    calls.put(event.getCallID().intValue(), conversation);
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(callLog(event.getCallID().getCall(), "Conversation created"));
                    conversation.startConversation();
                } finally {
                    callsLock.writeLock().unlock();
                }
            }
        } catch (Exception ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(callLog(event.getCallID().getCall(), "Error creating conversation"), ex);
        }
    }

    private IvrEndpointConversationImpl getConversation(CallEv event)
    {
        return calls.get(((CiscoCall)event.getCall()).getCallID().intValue());
    }
}
