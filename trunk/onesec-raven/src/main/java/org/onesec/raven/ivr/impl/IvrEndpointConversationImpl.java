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

import com.cisco.jtapi.extensions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.*;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaTerminalConnection;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.*;
import static org.onesec.raven.ivr.IvrEndpointConversationState.*;
import org.onesec.raven.ivr.actions.ContinueConversationAction;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenario;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.ConversationScenarioState;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import static org.onesec.raven.impl.CCMUtils.*;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationImpl implements IvrEndpointConversation
{
    private enum RtpStatus {INVALID, CREATED, CONNECTED, WAITING_FOR_START}

    @Service private static Tree tree;
    @Service private static ProviderRegistry providerRegistry;
    @Service private static BufferCache bufferCache;
    @Service private static StateListenersCoordinator stateListenersCoordinator;
    @Service private static CodecManager codecManager;

    private final Node owner;
    private final CiscoJtapiTerminal terminal;
    private final LoggerHelper logger;
    private final ExecutorService executor;
    private final ConversationScenario scenario;
    private final RtpStreamManager streamManager;
    private final boolean enableIncomingRtpStream;
    private final AtomicBoolean audioStreamJustCreated = new AtomicBoolean();
    private final String terminalAddress;
    private Map<String, Object> additionalBindings;
    private Codec codec;
    private int packetSize;
    private int maxSendAheadPacketsCount;

    private OutgoingRtpStream outRtp;
    private IncomingRtpStream inRtp;
    private RtpStatus inRtpStatus = RtpStatus.INVALID;
    private RtpStatus outRtpStatus = RtpStatus.INVALID;
    private ConversationScenarioState conversationState;
    private IvrActionsExecutor actionsExecutor;
    private ConcatDataSource audioStream;
    private CiscoCall call;
    private String remoteAddress;
    private int remotePort;
    private String callId;
    private BindingSupportImpl bindingSupport;
    private IvrEndpointConversationStateImpl state;
    private volatile String callingNumber;
    private volatile String calledNumber;
    private Collection<IvrEndpointConversationListener> listeners;
    private volatile boolean stopping = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointConversationImpl(
            Node owner
            , CiscoJtapiTerminal terminal
            , ExecutorService executor
            , ConversationScenario scenario, RtpStreamManager streamManager
            , boolean enableIncomingRtpStream
            , String terminalAddress
            , Map<String, Object> additionalBindings)
        throws Exception
    {
        this.owner = owner;
        this.terminal = terminal;
        this.logger = new LoggerHelper(owner, null);
        this.executor = executor;
        this.scenario = scenario;
        this.streamManager = streamManager;
        this.additionalBindings = additionalBindings;
        this.enableIncomingRtpStream = enableIncomingRtpStream;
        this.terminalAddress = terminalAddress;

        state = new IvrEndpointConversationStateImpl(this);
        state.setState(INVALID);
//        call.getConnections()[0].getTerminalConnections()[0].
//        stateListenersCoordinator.addListenersToState(state, IvrEndpointConversationState.class);
    }

    public CiscoCall getCall() {
        return call;
    }

    public void addConversationListener(IvrEndpointConversationListener listener) {
        lock.writeLock().lock();
        try {
            if (listeners==null)
                listeners = new HashSet<IvrEndpointConversationListener>();
            listeners.add(listener);
            listener.listenerAdded(new IvrEndpointConversationEventImpl(this));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeConversationListener(IvrEndpointConversationListener listener) {
        lock.writeLock().lock();
        try {
            if (listeners!=null)
                listeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public String getCalledNumber() {
        return calledNumber;
    }

    public IvrEndpointConversationState getState() {
        return state;
    }

    private void checkState() throws IvrEndpointConversationException {
        switch (state.getId()) {
            case INVALID:
                if (call!=null) state.setState(READY); break;
            case READY:
                if (call==null)
                    state.setState(INVALID);
                else if (inRtpStatus==RtpStatus.CREATED || outRtpStatus==RtpStatus.CREATED)
                    state.setState(CONNECTING);
                break;
            case CONNECTING:
                if (call==null) {
                    stopIncomingRtp();
                    stopOutgoingRtp();
                    state.setState(INVALID);
                } else if (inRtpStatus == RtpStatus.CONNECTED && outRtpStatus == RtpStatus.CONNECTED) {
                    if (isAllLogicalConnectionEstablished()) {
                        state.setState(TALKING);
                        fireEvent(true, null);
                        startConversation();
                    }
                } else if (inRtpStatus==RtpStatus.WAITING_FOR_START || outRtpStatus==RtpStatus.WAITING_FOR_START && isAllLogicalConnectionEstablished()) {
                    if (inRtpStatus==RtpStatus.WAITING_FOR_START && outRtpStatus.ordinal()>RtpStatus.CREATED.ordinal())
                        startIncomingRtp();
                    if (outRtpStatus==RtpStatus.WAITING_FOR_START)
                        startOutgoingRtp();
                }
//                else if (inRtpStatus==RtpStatus.WAITING_FOR_START && outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal())
//                    startIncomingRtp();
                else if (inRtpStatus==RtpStatus.INVALID && outRtpStatus==RtpStatus.INVALID)
                    state.setState(READY);
                break;
            case TALKING:
                if (call==null) {
                    state.setState(CONNECTING);
                    checkState();
                } else if (inRtpStatus!=RtpStatus.CONNECTED || outRtpStatus!=RtpStatus.CONNECTED)
                    state.setState(CONNECTING);
                break;
        }
    }

    public void setCall(CallControlCall call) throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (state.getId()!=INVALID)
                throw new IvrEndpointConversationStateException("Can't setCall", "INVALID", state.getIdName());
            this.call = (CiscoCall) call;
            callId = "[call id: " + this.call.getCallID().intValue()+", calling number: "
                    + call.getCallingAddress().getName() + "]";
            callingNumber = getPartyNumber(true);
            calledNumber = getPartyNumber(false);
            checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public IncomingRtpStream initIncomingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (state.getId()!=READY && state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't init incoming RTP", "READY, CONNECTING", state.getIdName());
            if (inRtpStatus!=RtpStatus.INVALID)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't create incoming RTP stream", "INVALID", inRtpStatus.name());
            inRtp = streamManager.getIncomingRtpStream(owner);
            inRtp.setLogPrefix(callId+" : ");
            inRtpStatus = RtpStatus.CREATED;
            if (logger.isDebugEnabled())
                logger.debug(callLog("Incoming RTP successfully created"));
            checkState();
            return inRtp;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void initOutgoingRtp(String remoteAddress, int remotePort, int packetSize
            , int maxSendAheadPacketsCount, Codec codec)
        throws IvrEndpointConversationException
    {
        lock.writeLock().lock();
        try {
            if (state.getId()!=READY && state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't init outgoing RTP", "READY, CONNECTING", state.getIdName());
            if (outRtpStatus!=RtpStatus.INVALID)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't create outgoing RTP stream", "INVALID", outRtpStatus.name());
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
            this.packetSize = packetSize;
            this.maxSendAheadPacketsCount = maxSendAheadPacketsCount;
            this.codec = codec;
            outRtp = streamManager.getOutgoingRtpStream(owner);
            if (outRtp!=null) {
                outRtp.setLogPrefix(callId+" : ");
                outRtpStatus = RtpStatus.CREATED;
                if (logger.isDebugEnabled())
                    logger.debug(callLog("Outgoing RTP successfully created"));
                checkState();
            } else
                throw new IvrEndpointConversationException("Error creating outgoing rtp stream");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void logicalConnectionCreated(String opponentNumber) throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog(
                        "Logical connection created for opponent number (%s)", opponentNumber));
            checkForOpponentPartyTransfered(opponentNumber);
            if (state.getId()==CONNECTING)
                checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void makeLogicalTransfer(String opponentNumber) {
        lock.writeLock().lock();
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog(
                        "Handling logical transfer to number (%s)", opponentNumber));
            checkForOpponentPartyTransfered(opponentNumber);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkForOpponentPartyTransfered(String opponentNumber) {
        if (!terminalAddress.equals(opponentNumber) && !ObjectUtils.in(opponentNumber, callingNumber, calledNumber)) {
            String newNumber = getPartyNumber(true);
            if (terminalAddress.equals(callingNumber))
                calledNumber = opponentNumber;
            else
                callingNumber = opponentNumber;
//            if (newNumber!=null && !newNumber.equals(callingNumber))
//                callingNumber = newNumber;
//            newNumber = getPartyNumber(false);
//            if (newNumber!=null && !newNumber.equals(calledNumber))
//                calledNumber = newNumber;
            if (logger.isDebugEnabled())
                logger.debug(callLog("Call transfered to number (%s). calledNumber: %s, callingNumber: %s", 
                        opponentNumber, calledNumber, callingNumber));
            fireTransferedEvent(opponentNumber);
        }
    }

    private String getPartyNumber(boolean callingParty) {
        Address addr = callingParty? getCallingAddress() : call.getCalledAddress();
        return addr==null? null : addr.getName();
    }
    
    private Address getCallingAddress() {
        Address addr = call.getModifiedCallingAddress();
        return addr != null? addr : call.getCallingAddress();
    }

    private boolean isAllLogicalConnectionEstablished() {
        Connection[] cons = call.getConnections();
        if (cons!=null) {
            if (logger.isDebugEnabled())
                for (Connection con: cons)
                    logger.debug(callLog(
                            "Call connection: address=%s; state=%s; callControlState=%s"
                            , con.getAddress().getName(), con.getState()
                            , ((CallControlConnection)con).getCallControlState()));
            for (Connection con: cons)
                if (((CallControlConnection)con).getCallControlState()!=CallControlConnection.ESTABLISHED)
                    return false;
            return true;
        }
        return false;
    }

    public void startIncomingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog("Trying to start incoming RTP stream"));
            if (state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't start incoming RTP", "CONNECTING", state.getIdName());
            if (inRtpStatus!=RtpStatus.CREATED && inRtpStatus!=RtpStatus.WAITING_FOR_START)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't start incoming RTP stream", "CREATED, WATING_FOR_START", inRtpStatus.name());
            try {
//                if (outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal() && isAllLogicalConnectionEstablished()) {
                if (outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal()) {
                    if (enableIncomingRtpStream)
                        inRtp.open(remoteAddress);
                    fireIncomingRtpStartedEvent();
                    inRtpStatus = RtpStatus.CONNECTED;
                    checkState();
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(callLog(
                                "Incoming RTP. Can't start. Outgoing RTP not created, waiting..."));
                    inRtpStatus = RtpStatus.WAITING_FOR_START;
                }
            } catch (RtpStreamException e){
                if (logger.isErrorEnabled())
                    logger.error(callLog("Error starting incoming RTP"), e);
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void startOutgoingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (logger.isDebugEnabled())
                logger.debug(callLog("Trying to start outgoing RTP stream"));
            if (state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't start incoming RTP", "CONNECTING", state.getIdName());
            if (outRtpStatus!=RtpStatus.CREATED && outRtpStatus!=RtpStatus.WAITING_FOR_START)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't start incoming RTP stream", "CREATED, WAITING_FOR_START", outRtpStatus.name());
            try {
                if (isAllLogicalConnectionEstablished()) {
                    audioStream = new ConcatDataSource(
                            FileTypeDescriptor.WAVE, executor, codecManager, codec, packetSize, 0
                            , maxSendAheadPacketsCount, owner, bufferCache);
                    audioStream.setLogPrefix(callId+" : ");
                    audioStreamJustCreated.set(true);
                    outRtp.open(remoteAddress, remotePort, audioStream);
                    outRtp.start();
                    outRtpStatus = RtpStatus.CONNECTED;
                    fireOutgoingRtpStartedEvent();
                    checkState();
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(callLog(
                                "Outgoing RTP. Can't start. "
                                + "Not all logical connections are established. Waiting..."));
                    outRtpStatus = RtpStatus.WAITING_FOR_START;
                }
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error(callLog("Error starting outgoing RTP"), e);
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopIncomingRtp() {
        lock.writeLock().lock();
        try {
            try {
                if (inRtp!=null) {
                    inRtp.release();
                    inRtp = null;
                    inRtpStatus = RtpStatus.INVALID;
                    checkState();
                }
            } catch (Throwable e) {
                if (logger.isWarnEnabled())
                    logger.warn("Problem with stopping incoming rtp stream", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopOutgoingRtp() {
        lock.writeLock().lock();
        try {
            try {
                if (outRtp!=null) {
                    if (actionsExecutor!=null)
                        actionsExecutor.cancelActionsExecution();
                    outRtp.release();
                    outRtp = null;
                    outRtpStatus = RtpStatus.INVALID;
                    if (audioStream!=null) {
                        audioStream.close();
                        audioStream = null;
                    }
//                    actionsExecutor = null;
                    checkState();
                }
            } catch (Throwable e) {
                if (logger.isWarnEnabled())
                    logger.warn("Problem with stopping outgoing rtp stream", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void startConversation() throws IvrEndpointConversationException {
        try {
            boolean initialized = initConversation();
            if (logger.isDebugEnabled())
                logger.debug(callLog("Conversation %s", initialized?"started":"restarted"));
            continueConversation(EMPTY_DTMF);
        } catch (Throwable e) {
            stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }

    }

    private boolean initConversation() throws Exception {
        if (conversationState==null) {
            conversationState = scenario.createConversationState();
            conversationState.setBinding(DTMF_BINDING, "-", BindingScope.CONVERSATION);
            conversationState.setBinding(DISABLE_AUDIO_STREAM_RESET, false, BindingScope.CONVERSATION);
            conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
            conversationState.setBinding(VARS_BINDING, new HashMap(), BindingScope.CONVERSATION);
            conversationState.setBinding(
                    CONVERSATION_STATE_BINDING, conversationState, BindingScope.CONVERSATION);
            conversationState.setBinding(NUMBER_BINDING, callingNumber, BindingScope.CONVERSATION);
            conversationState.setBinding(CALLED_NUMBER_BINDING, calledNumber, BindingScope.CONVERSATION);
            if (additionalBindings!=null)
                for (Map.Entry<String, Object> b: additionalBindings.entrySet())
                    conversationState.setBinding(b.getKey(), b.getValue(), BindingScope.CONVERSATION);
            additionalBindings = null;
            actionsExecutor = new IvrActionsExecutor(this, executor);
            actionsExecutor.setLogPrefix(callId+" : ");
            this.bindingSupport = new BindingSupportImpl();
            return true;
        }
        return false;
    }

    public void continueConversation(char dtmfChar) {
        lock.writeLock().lock();
        try {
            try {
                if (dtmfChar!=EMPTY_DTMF)
                    fireDtmfReceived(dtmfChar);
                if (IvrEndpointConversationState.TALKING!=state.getId()) {
                    if (logger.isDebugEnabled())
                        logger.debug(callLog(
                                "Can't continue conversation. "
                                + "Conversation is not started. "
                                + "Current conversation state is %s", state.getIdName()));
                    return;
                }

                IvrConversationScenarioPoint point =
                        (IvrConversationScenarioPoint) conversationState.getConversationPoint();
                String validDtmfs = point.getValidDtmfs();
                if (dtmfChar!=EMPTY_DTMF && (
                        conversationState.isDtmfProcessingDisabled()
                        || validDtmfs==null
                        || validDtmfs.indexOf(dtmfChar)<0))
                {
                    if (logger.isDebugEnabled())
                        logger.debug(callLog("Invalid dtmf (%s). Skipping", dtmfChar));
                    return;
                }

                if (actionsExecutor.hasDtmfProcessPoint(dtmfChar)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Collecting DTMF chars. Collected: "
                                +actionsExecutor.getCollectedDtmfs().toString());
                    return;
                }

                conversationState.enableDtmfProcessing();

                if (logger.isDebugEnabled())
                    logger.debug(callLog(
                            "Continue conversation using dtmf ("+dtmfChar+")"));
                Boolean disableAudioStreamReset = (Boolean) conversationState.getBindings().get(
                        DISABLE_AUDIO_STREAM_RESET);
                if (!audioStreamJustCreated.compareAndSet(true, false) && !disableAudioStreamReset)
                    audioStream.reset();
                conversationState.getBindings().put(DTMF_BINDING, ""+dtmfChar);
                Collection<Node> actions = scenario.makeConversation(conversationState);
                Collection<IvrAction> ivrActions = new ArrayList<IvrAction>(10);
                String bindingId = null;
                try {
                    bindingId = tree.addGlobalBindings(bindingSupport);
                    bindingSupport.putAll(conversationState.getBindings());
                    bindingSupport.put(DTMF_BINDING, ""+dtmfChar);
                    for (Node node: actions)
                        if (node instanceof IvrActionNode) {
                            IvrAction action = ((IvrActionNode)node).createAction();
                            if (action!=null)
                                ivrActions.add(action);
                        } else if (node instanceof GotoNode || node instanceof ConversationScenarioPoint)
                            ivrActions.add(new ContinueConversationAction());
                    actionsExecutor.executeActions(ivrActions);
                } finally {
                    if (bindingId!=null)
                        tree.removeGlobalBindings(bindingId);
                    bindingSupport.reset();
                }
            } catch(Exception e) {
                if (logger.isErrorEnabled())
                    logger.error(callLog("Error continue conversation using dtmf %s", dtmfChar), e);
            }
        } finally  {
            lock.writeLock().unlock();
        }
    }

    public void stopConversation(CompletionCode completionCode) {
        lock.writeLock().lock();
        try {
            if (state.getId()==INVALID || stopping)
                return;
            stopping = true;
            if (state.getId()==TALKING || state.getId()==CONNECTING || state.getId()==READY)
                dropCallConnections();
            call = null;
            try {
                checkState();
            } catch (IvrEndpointConversationException e) {
                if (logger.isWarnEnabled())
                    logger.warn(callLog("Problem with stopping conversation"), e);
            }
            if (logger.isDebugEnabled())
                logger.debug(callLog("Conversation stopped (%s)", completionCode));
        } finally {
            lock.writeLock().unlock();
        }
        fireEvent(false, completionCode);
    }

    private void dropCallConnections()  {
        try {
            if (call.getState() == Call.ACTIVE) {
                if (logger.isDebugEnabled())
                    logger.debug(callLog("Dropping the call"));
                try {
                    call.drop();
                    long ts = System.currentTimeMillis();
                    while (call.getState()==Call.ACTIVE && ts+5000>=System.currentTimeMillis()) 
                        TimeUnit.MILLISECONDS.sleep(10);
                    if (call.getState() != Call.ACTIVE)
                        return;
                } catch (Throwable e) {
                    if (logger.isWarnEnabled())
                        logger.warn(callLog("Error dropping entire call"), e);
                }
                Connection[] connections = call.getConnections();
                if (connections != null && connections.length > 0)
                    for (Connection connection : connections) {
                        if (logger.isDebugEnabled())
                            logger.debug(callLog("Disconnecting connection for address (%s)"
                                    , connection.getAddress().getName()));
                        if (((CiscoAddress) connection.getAddress()).getState() == CiscoAddress.IN_SERVICE)
                            connection.disconnect();
                        else if (logger.isDebugEnabled())
                            logger.debug(callLog("Can't disconnect address not IN_SERVICE"));
                    }
                long ts = System.currentTimeMillis();
                while (call.getState()==Call.ACTIVE) {
                    if (ts+5000<System.currentTimeMillis())
                        throw new Exception("Timeout while waiting for call drop");
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }
        } catch (Throwable e) {
            if (logger.isWarnEnabled())
                logger.warn(callLog("Can't drop call connections"), e);

        }
    }

    public void sendMessage(String message, String encoding, SendMessageDirection direction) {
        String address = null;
        try{
            address = direction==SendMessageDirection.CALLED_PARTY?
                getCalledNumber() : getCallingNumber();
            ProviderController controller =  providerRegistry.getProviderController(address);
            CiscoTerminal term = (CiscoTerminal)controller.getProvider().getAddress(address).getTerminals()[0];
            term.addObserver(new SendTerminalObserver(message, encoding));
        } catch (Throwable e){
            if (logger.isWarnEnabled())
                logger.warn(callLog("Can't send message to %s (%s)", direction, address), e);
        }
    }

    public void sendDTMF(String digits) {
        try {
            CiscoCall _call = call;
            if (_call==null)
                return;
            Connection[] cons = _call.getConnections();
            MediaTerminalConnection termCon = null;
            if (cons!=null)
                for (Connection con: cons)
                    if (terminalAddress.equals(con.getAddress().getName())) {
                        TerminalConnection[] termCons = con.getTerminalConnections();
                        if (termCons!=null && termCons.length>0)
                            termCon = (MediaTerminalConnection) termCons[0];
                    }
            if (termCon==null)
                throw new Exception("Not found terminal connection");
            termCon.generateDtmf(digits);
        } catch(Throwable e) {
            if (logger.isWarnEnabled())
                logger.warn(callLog("Error sending DTMF (%s)", digits), e);
        }
    }

    public ConversationScenarioState getConversationScenarioState() {
        return conversationState;
    }

    public Node getOwner() {
        return owner;
    }

    public ExecutorService getExecutorService() {
        return executor;
    }

    public AudioStream getAudioStream()  {
        return audioStream;
    }

    public IncomingRtpStream getIncomingRtpStream()
    {
        return inRtp;
    }
    
    public void transfer(String address) throws IvrEndpointConversationException {
        try {
            terminal.transfer(call, address);
        } catch (IvrEndpointException ex) {
            throw new IvrEndpointConversationException(callLog("Transfer error"), ex);
        }
    }

    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        lock.writeLock().lock();
        try {
            if (state.getId()!=TALKING) {
                if (logger.isWarnEnabled())
                    logger.warn(callLog(
                            "Can't transfer call to the address (%s). Invalid call state (%s)"
                            , address, state.getIdName()));
                return;
            }
            try {
                audioStream.reset();
                try {
                    call.transfer(address);
                    fireTransferedEvent(address);
                } catch (Exception ex) {
                    if (logger.isErrorEnabled())
                        logger.error(ccmExLog(
                                callLog("Error transferring call to the address %s", address), ex), ex);
                }
            } finally {
                stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void unpark(String parkDN) throws IvrEndpointConversationException {
        try {
            terminal.unpark(call, parkDN);
        } catch (IvrEndpointException ex) {
            throw new IvrEndpointConversationException(callLog("Unpark error"), ex);
        }
    }
    
    public String park() throws IvrEndpointConversationException {
        try {
            CiscoConnection conn = getOwnConnection();
            if (conn==null) 
                throw new Exception("Call connection not found");
            if (logger.isDebugEnabled())
                logger.debug(callLog("Found own connection (%s)", conn));
            String parkDn = conn.park();
            if (logger.isDebugEnabled())
                logger.debug(callLog("Parked at (%s)", parkDn));
            return parkDn;
        } catch (Throwable e) {
            String mess = ccmExLog(callLog("Parking error"), e);
            if (logger.isErrorEnabled())
                logger.error(mess);
            throw new IvrEndpointConversationException(mess, e);
        }
    }

    private String callLog(String mess, Object... args) {
        return callId+" : Conversation. "+String.format(mess, args);
    }

    public String getObjectName() {
        return callId;
    }

    public String getObjectDescription() {
        return callId;
    }

    @Override
    public String toString() {
        return callId;
    }
    
    private CiscoConnection getOwnConnection() {
        Connection[] conns = call.getConnections();
        if (conns!=null && conns.length>0)
            for (Connection con: conns)
                if (terminalAddress.equals(con.getAddress().getName()))
                    return (CiscoConnection)con;
        return null;
    }

    private void fireEvent(final boolean conversationStartEvent, CompletionCode completionCode)
    {
        final List<IvrEndpointConversationListener> _listeners = new ArrayList(listeners);
        if ( _listeners!=null && ! _listeners.isEmpty()) {
            final IvrEndpointConversationEvent event = conversationStartEvent?
                new IvrEndpointConversationEventImpl(this) :
                new IvrEndpointConversationStoppedEventImpl(this, completionCode);
            String mess = String.format("Sending conversation %s event"
                    , conversationStartEvent? "started" : "stopped");
            executor.executeQuietly(new AbstractTask(owner, mess) {
                @Override public void doRun() throws Exception {
                    for (IvrEndpointConversationListener listener:  _listeners)
                        if (conversationStartEvent)
                            listener.conversationStarted(event);
                        else
                            listener.conversationStopped((IvrEndpointConversationStoppedEvent)event);
                }
            });
        }
    }

    private void fireTransferedEvent(String address) {
        if (listeners!=null && !listeners.isEmpty()) {
            final IvrEndpointConversationTransferedEvent event =
                    new IvrEndpointConversationTransferedEventImpl(this, address);
            executor.executeQuietly(new AbstractTask(owner, "Sending conversation transfered event") {
                @Override public void doRun() throws Exception {
                    for (IvrEndpointConversationListener listener: listeners)
                        listener.conversationTransfered(event);
                }
            });
        }
    }

    private void fireDtmfReceived(char dtmf) {
        if (listeners!=null && !listeners.isEmpty()) {
            final IvrDtmfReceivedConversationEventImpl event = 
                    new IvrDtmfReceivedConversationEventImpl(this, dtmf);
            executor.executeQuietly(new AbstractTask(owner, "Sending dtmf received event") {
                @Override public void doRun() throws Exception {
                    for (IvrEndpointConversationListener listener: listeners)
                        listener.dtmfReceived(event);
                }
            });
        }
    }

    private void fireIncomingRtpStartedEvent() {
        if (listeners!=null && !listeners.isEmpty()) {
            IvrIncomingRtpStartedEventImpl ev = new IvrIncomingRtpStartedEventImpl(this);
            for (IvrEndpointConversationListener listener: listeners)
                listener.incomingRtpStarted(ev);
        }
    }

    private void fireOutgoingRtpStartedEvent() {
        if (listeners!=null && !listeners.isEmpty()) {
            IvrOutgoingRtpStartedEventImpl ev = new IvrOutgoingRtpStartedEventImpl(this);
            for (IvrEndpointConversationListener listener: listeners)
                listener.outgoingRtpStarted(ev);
        }
    }

    private class SendTerminalObserver implements TerminalObserver
    {
        private final String message;
        private final String encoding;

        public SendTerminalObserver(String message, String encoding) {
            this.message = String.format("<CiscoIPPhoneText><Text>%s</Text></CiscoIPPhoneText>", message);
//            this.message = String.format("<CiscoIPPhoneStatus><Text>%s</Text></CiscoIPPhoneStatus>", message);
            this.encoding = encoding;
        }

        public void terminalChangedEvent(TermEv[] eventList) {
            for (TermEv ev: eventList){
                if (ev.getID()==CiscoTermInServiceEv.ID) {
                    try {
                        try {
                            ((CiscoTerminal) ev.getTerminal()).sendData(message.getBytes(encoding));
                        } finally {
                            ev.getTerminal().removeObserver(this);
                        }
                    } catch (Exception ex) {
                        if (logger.isWarnEnabled())
                            logger.warn(callLog("Can't send message"), ex);
                    }
                }
            }
        }
    }
}
