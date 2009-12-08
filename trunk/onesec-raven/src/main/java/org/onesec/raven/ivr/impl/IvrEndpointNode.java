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
import com.cisco.jtapi.extensions.CiscoMediaCapability;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputStoppedEv;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.cisco.jtapi.extensions.CiscoUnregistrationException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.protocol.FileTypeDescriptor;
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
import org.onesec.core.ObjectDescription;
import org.onesec.core.call.AddressMonitor;
import org.onesec.core.call.CallCompletionCode;
import org.onesec.core.call.CallResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.Operator;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationScenarioPoint;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.actions.ContinueConversationAction;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrEndpointNode extends BaseNode 
        implements IvrEndpoint, ObjectDescription, CiscoTerminalObserver, AddressObserver
            , CallControlCallObserver, MediaCallObserver
{
    @Service
    protected static ProviderRegistry providerRegistry;

    @Service
    protected static StateListenersCoordinator stateListenersCoordinator;

    @Service
    protected static Operator operator;

    @NotNull @Parameter
    private String address;
    
    @Parameter
    private IvrConversationScenarioNode conversationScenario;

    private IvrEndpointStateImpl endpointState;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executorService;

    @NotNull @Parameter
    private String ip;

    @NotNull @Parameter(defaultValue="1234")
    private Integer port;

    private Address terminalAddress;
    private CiscoMediaTerminal terminal;
    private Call call;

    private boolean terminalInService;
    private boolean terminalAddressInService;
    private boolean handlingIncomingCall;
    private boolean transfering;
    private AtomicBoolean observingCall;
    private AtomicBoolean observingTerminalAddress;
    private AtomicBoolean observingTerminal;
    private IvrConversationScenario currentConversation;
    private RTPSession rtpSession;
    private ConcatDataSource audioStream;
    private IvrActionsExecutor actionsExecutor;
    private ConversationScenarioState conversationState;
    private Provider provider;
    private String remoteAddress;
    private int remotePort;
    private int connected;
    private boolean rtpInitialized;
    private ConversationResultImpl conversationResult;
    private ConversationCompletionCallback completionCallback;
    private Map<String, Object> inviteBindings;
    private String opponentNumber;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        terminal = null;
        terminalAddress = null;
        endpointState = new IvrEndpointStateImpl(this);
        stateListenersCoordinator.addListenersToState(endpointState);
        bindingSupport = new BindingSupportImpl();
        resetStates();
        resetConversationFields();
    }

    public void resetStates()
    {
        terminalInService = false;
        terminalAddressInService = false;
        handlingIncomingCall = false;
        observingTerminal = new AtomicBoolean(false);
        observingTerminalAddress = new AtomicBoolean(false);
        observingCall = new AtomicBoolean(false);
        endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
    }

    private void resetConversationFields()
    {
        connected = 0;
        rtpInitialized = false;
        remotePort = 0;
        remoteAddress = null;
        handlingIncomingCall = false;
        transfering = false;
        conversationResult = null;
        completionCallback = null;
        opponentNumber = null;
        inviteBindings = null;
    }

//    private synchronized void setEndpointStatus(IvrEndpointState endpointStatus)
//    {
//        if (isLogLevelEnabled(LogLevel.DEBUG))
//            debug(String.format(
//                    "Changing endpoint status from (%s) to (%s)"
//                    , this.endpointState.toString(), endpointStatus.toString()));
//        this.endpointState = endpointStatus;
//    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        actionsExecutor = new IvrActionsExecutor(this, executorService);
        initializeEndpoint();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
        unregisterTerminal();
        removeCallObserver();
        removeTerminalAddressObserver();
        removeTerminalObserver();
        terminal = null;
        terminalAddress = null;
        actionsExecutor = null;
        resetStates();
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

    private synchronized void initializeEndpoint() throws Exception
    {
        try
        {
            endpointState.setState(IvrEndpointState.OUT_OF_SERVICE);
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
            if (!(terminals[0] instanceof CiscoMediaTerminal))
                throw new Exception(String.format(
                        "Invalid terminal type (%s). The terminal must be instance of (%s) " +
                        "(CTI_Port on the CCM side)"
                        , terminals[0].getName(), CiscoMediaTerminal.class.getName()));
            terminal = (CiscoMediaTerminal) terminals[0];
            CiscoMediaCapability[] caps =
                    new CiscoMediaCapability[]{CiscoMediaCapability.G711_64K_30_MILLISECONDS};

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Registering terminal using local ip (%s) and local port (%s)..."
                        , ip, port));
            terminal.register(InetAddress.getByName(ip), port, caps);

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

    public synchronized void invite(
            String opponentNumber, IvrConversationScenario conversationScenario
            , ConversationCompletionCallback callback
            , Map<String, Object> bindings) throws IvrEndpointException
    {
        if (!getStatus().equals(Status.STARTED)
                && endpointState.getId()!=IvrEndpointState.IN_SERVICE)
            throw new IvrEndpointException(
                    "Can't invite oppenent to conversation. Endpoint not ready");
        try
        {
            endpointState.setState(IvrEndpointState.INVITING);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Inviting opponent with number (%s) to conversation (%s)"
                        , opponentNumber, conversationScenario));
            resetConversationFields();
            handlingIncomingCall = true;
            this.opponentNumber = opponentNumber;
            currentConversation = conversationScenario;
            conversationResult = new ConversationResultImpl();
            conversationResult.setCallStartTime(System.currentTimeMillis());
            completionCallback = callback;
            inviteBindings = bindings;
            Call call = provider.createCall();
            call.connect(terminal, terminalAddress, opponentNumber);
        }
        catch (Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format("Error inviting opponent (%s) to conversation", opponentNumber)
                    , e);
            stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    public void transfer(
            String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        if (IvrEndpointState.TALKING!=endpointState.getId() || Status.STARTED!=getStatus())
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Can't transfer call to the address (%s). Endpoint not ready"
                        , address));
        }
        else
        {
            try
            {
                audioStream.reset();
                if (handlingIncomingCall)
                {
                    conversationResult.setCallEndTime(System.currentTimeMillis());
                    transfering = true;
                }
                try
                {
                    try
                    {
                        if (handlingIncomingCall)
                        {
                            conversationResult.setTransferAddress(address);
                            conversationResult.setTransferTime(System.currentTimeMillis());
                            conversationResult.setTransferCompletionCode(CallCompletionCode.NORMAL);
                        }
                        ((CallControlCall) call).transfer(address);
                        if (handlingIncomingCall && monitorTransfer)
                        {
                            AddressMonitor monitor = operator.createAddressMonitor(address);
                            CallResult res = monitor.waitForCallCompletion(
                                    opponentNumber, callStartTimeout, callEndTimeout);
                            conversationResult.setTransferCompletionCode(res.getCompletionCode());
                            conversationResult.setTransferConversationStartTime(
                                    res.getConversationStartTime());
                            conversationResult.setTransferConversationDuration(
                                    res.getConversationDuration());
                        }
                    }
                    catch(Exception e)
                    {
                        if (handlingIncomingCall)
                            conversationResult.setTransferCompletionCode(CallCompletionCode.ERROR);
                        throw e;
                    }
                }
                finally
                {
                    if (transfering)
                    {
                        transfering = false;
                        stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);
                    }
                }
            }
            catch (Exception ex)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format("Error transfering call to the address (%s)", address), ex);
            }
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

    @Parameter(readOnly=true)
    public String getState()
    {
        return endpointState.getIdName();
    }

    public IvrEndpointState getEndpointState()
    {
        return endpointState;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public IvrConversationScenarioNode getConversationScenario()
    {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationNode)
    {
        this.conversationScenario = conversationNode;
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
                case CiscoRTPOutputStartedEv.ID:
                    CiscoRTPOutputStartedEv rtpOutput = (CiscoRTPOutputStartedEv) event;
                    CiscoRTPOutputProperties props = rtpOutput.getRTPOutputProperties();
                    remoteAddress = props.getRemoteAddress().getHostAddress();
                    remotePort = props.getRemotePort();
                    rtpInitialized = true;
                    startRtpSession(remoteAddress, remotePort);
                    break;
                case CiscoRTPOutputStoppedEv.ID: stopRtpSession(); break;
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
                case CallCtlConnOfferedEv.ID:
                    if (!handlingIncomingCall)
                        acceptIncomingCall((CallCtlConnOfferedEv) event);
                    break;
                case TermConnRingingEv.ID:
                    answerOnIncomingCall((TermConnRingingEv)event);
                    break;
                case CallCtlConnEstablishedEv.ID:
                    CallCtlConnEstablishedEv e = (CallCtlConnEstablishedEv) event;
                    ++connected;
                    call = event.getCall();
                    startRtpSession(remoteAddress, remotePort);
                    break;
                case TermConnDroppedEv.ID:
                    stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
                    break;
                case MediaTermConnDtmfEv.ID:
                    continueConversation(((MediaTermConnDtmfEv)event).getDtmfDigit());
                    break;
                case CallObservationEndedEv.ID:
                    observingCall.set(false);
                    break;
                case CallCtlConnFailedEv.ID:
                    if (handlingIncomingCall)
                    {
                        CallCtlConnFailedEv ev = (CallCtlConnFailedEv) event;
                        int cause = ev.getCallControlCause();
                        System.out.println("        >>> CAUSE: "+cause+" : "+ev.getCause());
                        CompletionCode code = CompletionCode.OPPONENT_UNKNOWN_ERROR;
                        switch (cause)
                        {
                            case CallCtlConnFailedEv.CAUSE_BUSY:
                                code = CompletionCode.OPPONENT_BUSY;
                                break;
                            case CallCtlConnFailedEv.CAUSE_CALL_NOT_ANSWERED:
                            case CallCtlConnFailedEv.CAUSE_NORMAL:
                                code = CompletionCode.OPPONENT_NO_ANSWERED;
                                break;
                        }
                        stopConversation(code);
                    }
                    break;
                case CallCtlTermConnDroppedEv.ID:
                    CallCtlTermConnDroppedEv ev = (CallCtlTermConnDroppedEv) event;
                    switch (ev.getCause())
                    {
                        case CallCtlTermConnDroppedEv.CAUSE_NORMAL :
                            System.out.println("    >>>> NORMAL <<<< :"+ev.getCause()); break;
                        case CallCtlTermConnDroppedEv.CAUSE_CALL_CANCELLED :
                            System.out.println("    >>>> CANCELED <<<< :"+ev.getCause()); break;
                        default: System.out.println("    >>>> UNKNOWN <<<< :"+ev.getCause());
                    }
                    break;
            }
        }
    }

    private synchronized void acceptIncomingCall(CallCtlConnOfferedEv event)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Accepting incoming call");
        CallCtlConnOfferedEv offEvent = (CallCtlConnOfferedEv)event;
        CallControlConnection conn = (CallControlConnection) offEvent.getConnection();
        try
        {
            currentConversation = conversationScenario;
            if (currentConversation==null)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(
                        "Can not make conversation because of does not have conversation scenario");
                conn.disconnect();
            }
            else
            {
                conn.accept();
                endpointState.setState(IvrEndpointState.ACCEPTING_CALL);
            }
        }
        catch (Exception ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error accepting call", ex);
        }
    }
        
    private synchronized void answerOnIncomingCall(TermConnRingingEv event)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Answering on incoming call");
        try
        {
            event.getTerminalConnection().answer();
//            endpointState.setState(IvrEndpointState.TALKING);
        }
        catch (Exception e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error answering on call", e);
        }
    }

    public void continueConversation(char dtmfChar)
    {
        try
        {
            IvrConversationScenarioPoint point =
                    (IvrConversationScenarioPoint) conversationState.getNextConversationPoint();
            String validDtmfs = point.getValidDtmfs();
            if (dtmfChar!=EMPTY_DTMF && (validDtmfs==null || validDtmfs.indexOf(dtmfChar)<0))
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format("Invalid dtmf (%s). Skipping", dtmfChar));
                return;
            }
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Continue conversation with dtmf (%s)", dtmfChar));
            audioStream.reset();
            conversationState.getBindings().put(DTMF_BINDING, ""+dtmfChar);
            Collection<Node> actions = currentConversation.makeConversation(conversationState);
            Collection<IvrAction> ivrActions = new ArrayList<IvrAction>(10);
            try
            {
                tree.addGlobalBindings(getId()+"", bindingSupport);
                bindingSupport.putAll(conversationState.getBindings());
                bindingSupport.put(DTMF_BINDING, ""+dtmfChar);
                for (Node node: actions)
                    if (node instanceof IvrActionNode)
                    {
                        IvrAction action = ((IvrActionNode)node).createAction();
                        if (action!=null)
                            ivrActions.add(action);
                    }
                if (conversationState.hasImmediateTransition())
                    ivrActions.add(new ContinueConversationAction());
                actionsExecutor.executeActions(ivrActions);
            }
            finally
            {
                tree.removeGlobalBindings(getId()+"");
                bindingSupport.reset();
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(IvrEndpointNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void stopConversation(CompletionCode completionCode)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Stoping conversation");
        if (endpointState.getId()==IvrEndpointState.IN_SERVICE)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Conversation already stopped");
            return;
        }
        if (transfering)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Can't stop conversation until transfer complete");
            return;
        }
        boolean changeStateToInService = true;
        try
        {
            try
            {
                try
                {
                    try
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug("Canceling actions execution");
                        actionsExecutor.cancelActionsExecution();
                    }
                    finally
                    {
                        TerminalConnection[] connections = terminal.getTerminalConnections();
                        if (connections!=null && connections.length>0)
                        {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                debug("Terminal has active connection. Disconnecting...");
                            Connection connection = connections[0].getConnection();
                            connection.disconnect();
                            changeStateToInService = false;
                            return;
//                                while (connections[0].getState()!=TerminalConnection.DROPPED)
//                                    Thread.sleep(100);
                        }
                    }
                }
                finally
                {
                    if (handlingIncomingCall)
                    {
                        if (!changeStateToInService)
                            conversationResult.setCompletionCode(completionCode);
                        else
                        {
                            if (conversationResult.getCompletionCode()==null)
                                conversationResult.setCompletionCode(completionCode);
                            long curTime = System.currentTimeMillis();
                            conversationResult.setCallEndTime(curTime);
                            completionCallback.conversationCompleted(conversationResult);
                        }
                    }
                }
            } catch (Exception e)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error("Error stoping conversation");
            }
        }
        finally
        {
            if (changeStateToInService)
            {
                resetConversationFields();
                endpointState.setState(IvrEndpointState.IN_SERVICE);
            }
        }
    }

    private String eventsToString(Object[] events)
    {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append((i>0? ", ":"")+events[i].toString());
        return buf.toString();
    }

    public ConcatDataSource getAudioStream()
    {
        return audioStream;
    }

    public ExecutorService getExecutorService() 
    {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    private synchronized void startRtpSession(String remoteHost, int remotePort)
    {
        if (connected<2 || !rtpInitialized)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Can't start rtp session. Not ready");
            return;

        }
        endpointState.setState(IvrEndpointState.TALKING);
        if (handlingIncomingCall)
            conversationResult.setConversationStartTime(System.currentTimeMillis());
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format(
                    "Starting rtp session: remoteHost (%s), remotePort (%s)"
                    , remoteHost, remotePort));
        audioStream = new ConcatDataSource(FileTypeDescriptor.WAVE, executorService, 160, this);
        try
        {
            conversationState = currentConversation.createConversationState();
            conversationState.setBinding(DTMF_BINDING, "-", BindingScope.REQUEST);
            conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
            conversationState.setBinding(VARS_BINDING, new HashMap(), BindingScope.CONVERSATION);
            conversationState.setBinding(
                    CONVERSATION_STATE_BINDING, conversationState, BindingScope.CONVERSATION);
            if (inviteBindings!=null)
                for (Map.Entry<String, Object> b: inviteBindings.entrySet())
                    conversationState.setBinding(
                            b.getKey(), b.getValue(), BindingScope.CONVERSATION);

            rtpSession = new RTPSession(remoteHost, remotePort, audioStream);
            rtpSession.start();
        } catch (Exception ex)
        {
            //TODO: ����� �������������� ���������? �������� �������� ������
            audioStream.close();
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error creating rtp session", ex);
        }
        //Starting the conversation
        continueConversation(EMPTY_DTMF);
    }

    private void stopRtpSession()
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Stoping rtp session");
        try
        {
            if (rtpSession!=null)
                rtpSession.stop();
        } catch (IOException ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error stoping rtp session", ex);
        }
    }

    public String getObjectName()
    {
        return getPath();
    }

    public String getObjectDescription()
    {
        return getPath();
    }

    private void unregisterTerminal()
    {
        try
        {
            terminal.unregister();
        } catch (CiscoUnregistrationException e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
            {
                error("Error unregistering terminal", e);
            }
        }
    }

    public Node getOwner()
    {
        return this;
    }
}