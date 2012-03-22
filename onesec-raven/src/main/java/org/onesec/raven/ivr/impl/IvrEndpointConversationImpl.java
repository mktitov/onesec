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

import org.onesec.raven.ivr.BufferCache;
import java.util.Map;
import com.cisco.jtapi.extensions.CiscoAddress;
import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Address;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.events.TermEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.core.services.StateListenersCoordinator;
import org.onesec.raven.ivr.*;
import org.raven.conv.ConversationScenario;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.onesec.raven.ivr.actions.ContinueConversationAction;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.ConversationScenarioState;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import static org.onesec.raven.ivr.IvrEndpointConversationState.*;

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
    private final ExecutorService executor;
    private final ConversationScenario scenario;
    private final RtpStreamManager streamManager;
    private final boolean enableIncomingRtpStream;
    private final AtomicBoolean audioStreamJustCreated = new AtomicBoolean();
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
    private String callingNumber;
    private String calledNumber;
    private Collection<IvrEndpointConversationListener> listeners;
    private boolean stopping = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointConversationImpl(
            Node owner, ExecutorService executor
            , ConversationScenario scenario, RtpStreamManager streamManager
            , boolean enableIncomingRtpStream
            , Map<String, Object> additionalBindings)
        throws Exception
    {
        this.owner = owner;
        this.executor = executor;
        this.scenario = scenario;
        this.streamManager = streamManager;
        this.additionalBindings = additionalBindings;
        this.enableIncomingRtpStream = enableIncomingRtpStream;

        state = new IvrEndpointConversationStateImpl(this);
        state.setState(INVALID);
//        stateListenersCoordinator.addListenersToState(state, IvrEndpointConversationState.class);
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
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Incoming RTP successfully created"));
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
            outRtp.setLogPrefix(callId+" : ");
            outRtpStatus = RtpStatus.CREATED;
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Outgoing RTP successfully created"));
            checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void logicalConnectionCreated(String opponentNumber) throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            checkForOpponentPartyTransfered(opponentNumber);
            if (state.getId()==CONNECTING)
                checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void checkForOpponentPartyTransfered(String opponentNumber) {
        if (!ObjectUtils.in(opponentNumber, callingNumber, calledNumber)) {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Call transfered to number (%s)", opponentNumber));
            fireTransferedEvent(opponentNumber);
        }
//        String transferNumber = getPartyNumber(true);
//        if (!ObjectUtils.equals(transferNumber, callingNumber)) 
//            callingNumber = transferNumber;
//        else {
//            transferNumber = getPartyNumber(false);
//            if (!ObjectUtils.equals(transferNumber, calledNumber))
//                calledNumber = transferNumber;
//            else
//                transferNumber = null;
//        }
//        if (transferNumber!=null) {
//        }
    }
    
    private String getPartyNumber(boolean callingParty) {
        Address addr = callingParty? call.getCallingAddress() : call.getCalledAddress();
        return addr==null? null : addr.getName();
    }
    
    private boolean isAllLogicalConnectionEstablished() {
        Connection[] cons = call.getConnections();
        if (cons!=null) {
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
                } else
                    inRtpStatus = RtpStatus.WAITING_FOR_START;
            } catch (RtpStreamException e){
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(callLog("Error starting incoming RTP"), e);
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void startOutgoingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
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
                } else 
                    outRtpStatus = RtpStatus.WAITING_FOR_START;
            } catch (Exception e) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(callLog("Error starting outgoing RTP"), e);
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopIncomingRtp() {
        lock.writeLock().lock();
        try {
//            if (state.getId()!=CONNECTING || state.getId()!=TALKING)
//                throw new IvrEndpointConversationStateException(
//                        "Can't stop outgoing RTP", "CONNECTING, TALKING", state.getIdName());
//            if (inRtpStatus!=RtpStatus.CREATED || inRtpStatus!=RtpStatus.CONNECTED)
//                throw new IvrEndpointConversationRtpStateException(
//                        "Can't stop outgoing RTP stream", "CREATED, CONNECTED", inRtpStatus.name());
            try {
                if (inRtp!=null) {
                    inRtp.release();
                    inRtp = null;
                    inRtpStatus = RtpStatus.INVALID;
                    checkState();
                }
            } catch (Throwable e) {
                if (owner.isLogLevelEnabled(LogLevel.WARN))
                    owner.getLogger().warn("Problem with stopping incoming rtp stream", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopOutgoingRtp() {
        lock.writeLock().lock();
        try {
//            if (state.getId()!=CONNECTING || state.getId()!=TALKING)
//                throw new IvrEndpointConversationStateException(
//                        "Can't stop outgoing RTP", "CONNECTING, TALKING", state.getIdName());
//            if (outRtpStatus!=RtpStatus.CREATED || outRtpStatus!=RtpStatus.CONNECTED)
//                throw new IvrEndpointConversationRtpStateException(
//                        "Can't stop outgoing RTP stream", "CREATED, CONNECTED", outRtpStatus.name());
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
                if (owner.isLogLevelEnabled(LogLevel.WARN))
                    owner.getLogger().warn("Problem with stopping outgoing rtp stream", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void startConversation() throws IvrEndpointConversationException {
        try {
            boolean initialized = initConversation();
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Conversation %s", initialized?"started":"restarted"));
            continueConversation(EMPTY_DTMF);
        } catch (Throwable e) {
            stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
        
    }

    private boolean initConversation() throws Exception {
        if (conversationState==null) {
            conversationState = scenario.createConversationState();
            conversationState.setBinding(DTMF_BINDING, "-", BindingScope.CONVERSATION);
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

//    public void init(CallControlCall call, String remoteAddress, int remotePort, int packetSize
//            , int initialBufferSize, int maxSendAheadPacketsCount, Codec codec)
//        throws Exception
//    {
//        this.remoteAddress = remoteAddress;
//        this.remotePort = remotePort;
//        this.call = (CiscoCall)call;
//        this.callingNumber = call.getCallingAddress().getName();
//        this.calledNumber = call.getCalledAddress().getName();
//        callId = getCallId();
//
//        outRtpStream = streamManager.getOutgoingRtpStream(owner);
//        outRtpStream.setLogPrefix(callId+" : ");
////        inRtpStream = .setLogPrefix(callId+" : ");
//        conversationState = scenario.createConversationState();
//        conversationState.setBinding(DTMF_BINDING, "-", BindingScope.REQUEST);
//        conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
//        conversationState.setBinding(VARS_BINDING, new HashMap(), BindingScope.CONVERSATION);
//        conversationState.setBinding(
//                CONVERSATION_STATE_BINDING, conversationState, BindingScope.CONVERSATION);
//        conversationState.setBinding(NUMBER_BINDING, callingNumber, BindingScope.CONVERSATION);
//        conversationState.setBinding(CALLED_NUMBER_BINDING, calledNumber, BindingScope.CONVERSATION);
//        if (additionalBindings!=null)
//            for (Map.Entry<String, Object> b: additionalBindings.entrySet())
//                conversationState.setBinding(b.getKey(), b.getValue(), BindingScope.CONVERSATION);
//        additionalBindings = null;
//
//        audioStream = new ConcatDataSource(
//                FileTypeDescriptor.WAVE, executor, codec, packetSize, initialBufferSize
//                , maxSendAheadPacketsCount, owner, bufferCache);
//        audioStream.setLogPrefix(callId+" : ");
//        actionsExecutor = new IvrActionsExecutor(this, executor);
//        actionsExecutor.setLogPrefix(callId+" : ");
//        this.bindingSupport = new BindingSupportImpl();
//
//        state.setState(CONNECTING);
//    }
//
//    public IvrEndpointConversationState startConversation()
//    {
//        lock.writeLock().lock();
//        try {
//            if (IvrEndpointConversationState.CONNECTING!=state.getId()) {
//                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                    owner.getLogger().debug(String.format(
//                            "Can't start conversation. Conversation is already started or not ready. " +
//                            "Current conversation state is %s", state.getIdName()));
//                return state;
//            } else if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                owner.getLogger().debug(callLog("Trying to start conversation"));
//            int activeConnections = 0;
//            Connection[] connections = call.getConnections();
//            if (connections!=null)
//                for (Connection connection: connections)
////                    if (connection.getState() == Connection.CONNECTED)
//                    if (((CallControlConnection)connection).getCallControlState() == CallControlConnection.ESTABLISHED)
//                        ++activeConnections;
//
//            if (activeConnections!=2) {
//                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                    owner.getLogger().debug(callLog(
//                            "Not all participant are ready for conversation. Ready (%s) participant(s)"
//                            , activeConnections));
//                return state;
//            }
//
//            try {
//                outRtpStream.open(remoteAddress, remotePort, audioStream);
//                outRtpStream.start();
//                if (enableIncomingRtpStream)
//                    inRtpStream.open(remoteAddress);
//
//                state.setState(TALKING);
//                fireEvent(true, null);
//
//                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                    owner.getLogger().debug(callLog("Conversation successfully started"));
//
//                continueConversation(EMPTY_DTMF);
//            } catch (RtpStreamException ex) {
//                if (owner.isLogLevelEnabled(LogLevel.ERROR))
//                    owner.getLogger().error(ex.getMessage(), ex);
//                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//            }
//
//            return state;
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }

    public void continueConversation(char dtmfChar) {
        lock.writeLock().lock();
        try {
            try {
                if (IvrEndpointConversationState.TALKING!=state.getId()) {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(callLog(
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
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(callLog("Invalid dtmf (%s). Skipping", dtmfChar));
                    return;
                }

                if (actionsExecutor.hasDtmfProcessPoint(dtmfChar)) {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug("Collecting DTMF chars. Collected: "
                                +actionsExecutor.getCollectedDtmfs().toString());
                    return;
                }

                conversationState.enableDtmfProcessing();

                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog(
                            "Continue conversation using dtmf ("+dtmfChar+")"));
                
                if (!audioStreamJustCreated.compareAndSet(true, false))
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
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(callLog("Error continue conversation using dtmf %s", dtmfChar), e);
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
                if (owner.isLogLevelEnabled(LogLevel.WARN))
                    owner.getLogger().warn(callLog("Problem with stopping conversation"), e);
            }
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Conversation stopped (%s)", completionCode));
        } finally {
            lock.writeLock().unlock();
        }
        fireEvent(false, completionCode);
    }
    
    private void dropCallConnections()  {
        try {
            if (call.getState() == Call.ACTIVE) {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Dropping the call"));
                Connection[] connections = call.getConnections();
                if (connections != null && connections.length > 0)
                    for (Connection connection : connections) {
                        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                            owner.getLogger().debug(callLog("Disconnecting connection for address (%s)"
                                    , connection.getAddress().getName()));
                        if (((CiscoAddress) connection.getAddress()).getState() == CiscoAddress.IN_SERVICE)
                            connection.disconnect();
                        else if (owner.getLogger().isDebugEnabled())
                            owner.getLogger().debug(callLog("Can't disconnect address not IN_SERVICE"));
                    }
            }
        } catch (Throwable e) {
            if (owner.isLogLevelEnabled(LogLevel.WARN))
                owner.getLogger().warn(callLog("Can't drop call connections"), e);

        }
    }

//    public void _stopConversation(CompletionCode completionCode)
//    {
//        lock.writeLock().lock();
//        try {
//            stopIncomingRtp();
//            if (state.getId()==INVALID)
//                return;
//            try {
//                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                    owner.getLogger().debug(callLog("Stoping the conversation"));
//                stopOutgoingRtp();
//                audioStream.close();
//                try {
//                    actionsExecutor.cancelActionsExecution();
//                } catch (InterruptedException ex) {
//                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
//                        owner.getLogger().error(callLog(
//                                "Error canceling actions executor while stoping conversation"), ex);
//                }
//                try {
//                    dropCallConnections();
//                } catch (Exception ex) {
//                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
//                        owner.getLogger().error(callLog(
//                                "Error droping a call while stoping conversation"), ex);
//                }
//            }
//            finally {
//                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                    owner.getLogger().debug(callLog("Conversation stoped (%s)", completionCode));
//            }
//        } finally {
//            state.setState(INVALID);
//            lock.writeLock().unlock();
//        }
//        fireEvent(false, completionCode);
//    }
//

    public void sendMessage(String message, String encoding, SendMessageDirection direction) {
        try{
            String address = direction==SendMessageDirection.CALLED_PARTY?
                getCalledNumber() : getCallingNumber();
            ProviderController controller =  providerRegistry.getProviderController(address);
            CiscoTerminal term = (CiscoTerminal)controller.getProvider().getAddress(address).getTerminals()[0];
            term.addObserver(new SendTerminalObserver(message, encoding));
        } catch (Throwable e){
            if (owner.isLogLevelEnabled(LogLevel.WARN))
                owner.getLogger().warn(callLog("Can't send message to %s", direction), e);
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

    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        lock.writeLock().lock();
        try
        {
            if (state.getId()!=TALKING)
            {
                if (owner.isLogLevelEnabled(LogLevel.WARN))
                    owner.getLogger().warn(callLog(
                            "Can't transfer call to the address (%s). Invalid call state (%s)"
                            , address, state.getIdName()));
                return;
            }
            try
            {
                audioStream.reset();
                try {
                    call.transfer(address);
                    fireTransferedEvent(address);
                } catch (Exception ex)
                {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(
                                callLog("Error transferring call to the address %s", address)
                                , ex);
                } 
            }
            finally
            {
                stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private String callLog(String mess, Object... args)
    {
        return callId+" : Conversation. "+String.format(mess, args);
    }

    public String getObjectName()
    {
        return callId;
    }

    public String getObjectDescription()
    {
        return callId;
    }

    @Override
    public String toString() {
        return callId;
    }

    private void fireEvent(boolean conversationStartEvent, CompletionCode completionCode)
    {
        if (listeners!=null && !listeners.isEmpty()) {
            IvrEndpointConversationEvent event = conversationStartEvent? 
                new IvrEndpointConversationEventImpl(this) :
                new IvrEndpointConversationStoppedEventImpl(this, completionCode);
            for (IvrEndpointConversationListener listener: listeners)
                if (conversationStartEvent)
                    listener.conversationStarted(event);
                else
                    listener.conversationStopped((IvrEndpointConversationStoppedEvent)event);
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
            this.encoding = encoding;
        }

        public void terminalChangedEvent(TermEv[] eventList)
        {
            for (TermEv ev: eventList){
                if (ev.getID()==CiscoTermInServiceEv.ID) {
                    try {
                        try {
                            ((CiscoTerminal) ev.getTerminal()).sendData(message.getBytes(encoding));
                        } finally {
                            ev.getTerminal().removeObserver(this);
                        }
                    } catch (Exception ex) {
                        if (owner.isLogLevelEnabled(LogLevel.WARN))
                            owner.getLogger().warn(callLog("Can't send message"), ex);
                    }
                }
            }
        }
    }
}
