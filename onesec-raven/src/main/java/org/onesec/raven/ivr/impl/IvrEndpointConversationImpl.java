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

import javax.telephony.InvalidStateException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.PrivilegeViolationException;
import javax.telephony.ResourceUnavailableException;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.events.TermEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.IvrConversationScenarioPoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationException;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationRtpStateException;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.ivr.SendMessageDirection;
import org.raven.conv.ConversationScenario;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrEndpointConversationStateException;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.ivr.actions.ContinueConversationAction;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.ConversationScenarioState;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.BindingSupportImpl;
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

    @Service
    private static Tree tree;

    @Service
    private static ProviderRegistry providerRegistry;

    @Service
    private static BufferCache bufferCache;

    private final Node owner;
    private final ExecutorService executor;
    private final ConversationScenario scenario;
    private final RtpStreamManager streamManager;
    private final boolean enableIncomingRtpStream;
    private Map<String, Object> additionalBindings;
    private Codec codec;
    private int packetSize;
    private int maxSendAheadPacketsCount;

    private OutgoingRtpStream outRtpStream;
    private IncomingRtpStream inRtpStream;
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

//        inRtpStream =  = streamManager.getIncomingRtpStream(owner);
        
        state = new IvrEndpointConversationStateImpl(this);
        state.setState(INVALID);
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
                if (call==null)
                    state.setState(INVALID);
                else if (inRtpStatus == RtpStatus.CONNECTED && outRtpStatus == RtpStatus.CONNECTED)
                    state.setState(TALKING);
                else if (inRtpStatus==RtpStatus.WAITING_FOR_START && outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal())
                    startIncomingRtp();
                break;
            case TALKING:
                if (call==null)
                    state.setState(INVALID);
                else if (inRtpStatus!=RtpStatus.CONNECTED && outRtpStatus==RtpStatus.CONNECTED)
                    state.setState(CONNECTING);
                break;
        }
    }

    public void setCall(CallControlCall call) throws IvrEndpointConversationException
    {
        lock.writeLock().lock();
        try {
            if (state.getId()!=INVALID)
                throw new IvrEndpointConversationStateException("Can't setCall", "INVALID", state.getIdName());
            this.call = (CiscoCall) call;
            callId = "[call id: " + this.call.getCallID().intValue()+", calling number: "
                    + call.getCallingAddress().getName() + "]";
            checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void initIncomingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (state.getId()!=READY || state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't init incoming RTP", "READY, CONNECTING", state.getIdName());
            if (inRtpStatus!=RtpStatus.INVALID)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't create incoming RTP stream", "INVALID", inRtpStatus.name());
            inRtpStream = streamManager.getIncomingRtpStream(owner);
            inRtpStream.setLogPrefix(callId+" : ");
            inRtpStatus = RtpStatus.CREATED;
            checkState();
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
            if (state.getId()!=READY || state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't init outgoing RTP", "READY, CONNECTING", state.getIdName());
            if (outRtpStatus!=RtpStatus.INVALID)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't create outgoing RTP stream", "INVALID", outRtpStatus.name());
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
            this.packetSize = packetSize;
            this.maxSendAheadPacketsCount = maxSendAheadPacketsCount;
            outRtpStream = streamManager.getOutgoingRtpStream(owner);
            outRtpStatus = RtpStatus.CREATED;
            checkState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void startIncomingRtp() throws IvrEndpointConversationException {
        lock.writeLock().lock();
        try {
            if (state.getId()!=CONNECTING)
                throw new IvrEndpointConversationStateException(
                        "Can't start incoming RTP", "CONNECTING", state.getIdName());
            if (inRtpStatus!=RtpStatus.CREATED)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't start incoming RTP stream", "CREATED", inRtpStatus.name());
            try {
                if (outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal()) {
                    if (enableIncomingRtpStream)
                        inRtpStream.open(remoteAddress);
                    inRtpStatus = RtpStatus.CONNECTED;
                    checkState();
                } else
                    inRtpStatus = RtpStatus.WAITING_FOR_START;
            } catch (RtpStreamException e){
                //TODO: stop conversation
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
            if (outRtpStatus!=RtpStatus.CREATED)
                throw new IvrEndpointConversationRtpStateException(
                        "Can't start incoming RTP stream", "CREATED", outRtpStatus.name());
            try {
                audioStream = new ConcatDataSource(
                        FileTypeDescriptor.WAVE, executor, codec, packetSize, 0, maxSendAheadPacketsCount
                        , owner, bufferCache);
                audioStream.setLogPrefix(callId+" : ");
                outRtpStream.open(remoteAddress, remotePort, audioStream);
                outRtpStatus = RtpStatus.CONNECTED;
            } catch (Exception e) {
                //TODO: stop conversation
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void init(CallControlCall call, String remoteAddress, int remotePort, int packetSize
            , int initialBufferSize, int maxSendAheadPacketsCount, Codec codec)
        throws Exception
    {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.call = (CiscoCall)call;
        this.callingNumber = call.getCallingAddress().getName();
        this.calledNumber = call.getCalledAddress().getName();
        callId = getCallId();

        outRtpStream = streamManager.getOutgoingRtpStream(owner);
        outRtpStream.setLogPrefix(callId+" : ");
//        inRtpStream = .setLogPrefix(callId+" : ");
        conversationState = scenario.createConversationState();
        conversationState.setBinding(DTMF_BINDING, "-", BindingScope.REQUEST);
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

        audioStream = new ConcatDataSource(
                FileTypeDescriptor.WAVE, executor, codec, packetSize, initialBufferSize
                , maxSendAheadPacketsCount, owner, bufferCache);
        audioStream.setLogPrefix(callId+" : ");
        actionsExecutor = new IvrActionsExecutor(this, executor);
        actionsExecutor.setLogPrefix(callId+" : ");
        this.bindingSupport = new BindingSupportImpl();

        state.setState(CONNECTING);
    }

    public IvrEndpointConversationState startConversation()
    {
        lock.writeLock().lock();
        try {
            if (IvrEndpointConversationState.CONNECTING!=state.getId()) {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(String.format(
                            "Can't start conversation. Conversation is already started or not ready. " +
                            "Current conversation state is %s", state.getIdName()));
                return state;
            } else if (owner.isLogLevelEnabled(LogLevel.DEBUG)) 
                owner.getLogger().debug(callLog("Trying to start conversation"));
            int activeConnections = 0;
            Connection[] connections = call.getConnections();
            if (connections!=null)
                for (Connection connection: connections)
//                    if (connection.getState() == Connection.CONNECTED)
                    if (((CallControlConnection)connection).getCallControlState() == CallControlConnection.ESTABLISHED)
                        ++activeConnections;

            if (activeConnections!=2) {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog(
                            "Not all participant are ready for conversation. Ready (%s) participant(s)"
                            , activeConnections));
                return state;
            }

            try {
                outRtpStream.open(remoteAddress, remotePort, audioStream);
                outRtpStream.start();
                if (enableIncomingRtpStream)
                    inRtpStream.open(remoteAddress);
                
                state.setState(TALKING);
                fireEvent(true, null);

                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Conversation successfully started"));

                continueConversation(EMPTY_DTMF);
            } catch (RtpStreamException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(ex.getMessage(), ex);
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }

            return state;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void continueConversation(char dtmfChar)
    {
        lock.writeLock().lock();
        try
        {
            try
            {
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
                
                audioStream.reset();
                conversationState.getBindings().put(DTMF_BINDING, ""+dtmfChar);
                Collection<Node> actions = scenario.makeConversation(conversationState);
                Collection<IvrAction> ivrActions = new ArrayList<IvrAction>(10);
                String bindingId = null;
                try
                {
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
                }
                finally
                {
                    if (bindingId!=null)
                        tree.removeGlobalBindings(bindingId);
                    bindingSupport.reset();
                }
            }
            catch(Exception e)
            {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(callLog("Error continue conversation using dtmf %s", dtmfChar), e);
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void stopIncomingRtpStream() {
        lock.writeLock().lock();
        try {
            if (inRtpStream!=null){
                inRtpStream.release();
                inRtpStream = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopOutgoingRtpStream() {
        lock.writeLock();
        try {
            if (outRtpStream!=null) {
                outRtpStream.release();
                outRtpStream = null;
            }
        } finally {

        }
    }

    public void stopConversation(CompletionCode completionCode)
    {
        lock.writeLock().lock();
        try {
            stopIncomingRtpStream();
            if (state.getId()==INVALID)
                return;
            try {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Stoping the conversation"));
                stopOutgoingRtpStream();
                audioStream.close();
                try {
                    actionsExecutor.cancelActionsExecution();
                } catch (InterruptedException ex) {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(callLog(
                                "Error canceling actions executor while stoping conversation"), ex);
                }
                try {
                    dropCallConnections();
                } catch (Exception ex) {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(callLog(
                                "Error droping a call while stoping conversation"), ex);
                }
            }
            finally {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Conversation stoped (%s)", completionCode));
            }
        } finally {
            state.setState(INVALID);
            lock.writeLock().unlock();
        }
        fireEvent(false, completionCode);
    }

    private void dropCallConnections() throws PrivilegeViolationException, MethodNotSupportedException, ResourceUnavailableException, InvalidStateException {
        if (call.getState() == Call.ACTIVE) {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG)) {
                owner.getLogger().debug(callLog("Droping the call"));
            }
            Connection[] connections = call.getConnections();
            if (connections != null && connections.length > 0) {
                for (Connection connection : connections) {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG)) {
                        owner.getLogger().debug(callLog("Disconnecting connection for address (%s)", connection.getAddress().getName()));
                    }
                    if (((CiscoAddress) connection.getAddress()).getState() == CiscoAddress.IN_SERVICE) {
                        connection.disconnect();
                    } else if (owner.getLogger().isDebugEnabled()) {
                        owner.getLogger().debug(callLog("Can't disconnect address not IN_SERVICE"));
                    }
                }
            }
        }
    }

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

    public Node getOwner()
    {
        return owner;
    }

    public ExecutorService getExecutorService()
    {
        return executor;
    }

    public AudioStream getAudioStream() 
    {
        return audioStream;
    }

    public IncomingRtpStream getIncomingRtpStream()
    {
        return inRtpStream;
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

    private String getCallId()
    {
        return "[call id: "+call.getCallID().intValue()+", calling number: "+call.getCallingAddress().getName()+"]";
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

    private void fireTransferedEvent(String address)
    {
        if (listeners!=null && !listeners.isEmpty()) {
            IvrEndpointConversationTransferedEvent event =
                    new IvrEndpointConversationTransferedEventImpl(this, address);
            for (IvrEndpointConversationListener listener: listeners)
                listener.conversationTransfered(event);
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
