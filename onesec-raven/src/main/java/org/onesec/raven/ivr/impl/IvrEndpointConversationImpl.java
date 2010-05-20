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

import com.cisco.jtapi.extensions.CiscoCall;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.InvalidArgumentException;
import javax.telephony.InvalidPartyException;
import javax.telephony.InvalidStateException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.PrivilegeViolationException;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.callcontrol.CallControlCall;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.IvrConversationScenarioPoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.conv.ConversationScenario;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.ivr.actions.ContinueConversationAction;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.Tree;
import org.weda.internal.annotations.Service;
import static org.onesec.raven.ivr.IvrEndpointConversationState.*;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationImpl implements IvrEndpointConversation
{
    @Service
    private static Tree tree;

    private final Node owner;
    private final ExecutorService executor;
    private final ConversationScenario scenario;
    private final RtpStreamManager streamManager;

    private OutgoingRtpStream outgoingRtpStream;
    private IncomingRtpStream incomingRtpStream;
    private ConversationScenarioState conversationState;
    private IvrActionsExecutor actionsExecutor;
    private ConcatDataSource audioStream;
    private CiscoCall call;
    private String remoteAddress;
    private int remotePort;
    private String callId;
    private BindingSupportImpl bindingSupport;
    private IvrEndpointConversationStateImpl state;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointConversationImpl(Node owner, ExecutorService executor
            , ConversationScenario scenario, RtpStreamManager streamManager)
        throws Exception
    {
        this.owner = owner;
        this.executor = executor;
        this.scenario = scenario;
        this.streamManager = streamManager;

        incomingRtpStream = streamManager.getIncomingRtpStream(owner);
        
        state = new IvrEndpointConversationStateImpl(this);
        state.setState(INVALID);
    }

    public void init(CallControlCall call, String remoteAddress, int remotePort)
            throws Exception
    {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.call = (CiscoCall)call;
        callId = getCallId();

        outgoingRtpStream = streamManager.getOutgoingRtpStream(owner);
        outgoingRtpStream.setLogPrefix(callId+" : ");
        incomingRtpStream.setLogPrefix(callId+" : ");
        conversationState = scenario.createConversationState();
        conversationState.setBinding(DTMF_BINDING, "-", BindingScope.REQUEST);
        conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
        conversationState.setBinding(VARS_BINDING, new HashMap(), BindingScope.CONVERSATION);
        conversationState.setBinding(
                CONVERSATION_STATE_BINDING, conversationState, BindingScope.CONVERSATION);
        audioStream = new ConcatDataSource(FileTypeDescriptor.WAVE, executor, 240, 5, 5, owner);
        audioStream.setLogPrefix(callId+" : ");
        actionsExecutor = new IvrActionsExecutor(this, executor);
        actionsExecutor.setLogPrefix(callId+" : ");
        this.bindingSupport = new BindingSupportImpl();

        state.setState(READY);
    }

    public IvrEndpointConversationState startConversation()
    {
        lock.writeLock().lock();
        if (IvrEndpointConversationState.READY!=state.getId()) {
            if (owner.isLogLevelEnabled(LogLevel.WARN))
                owner.getLogger().warn(callLog(
                        "Can't start conversation. Conversation is already started or not ready. " +
                        "Current conversation state is %s", state.getIdName()));
            return state;
        } else if (owner.isLogLevelEnabled(LogLevel.DEBUG)) {
            owner.getLogger().debug(callLog("Triyng to start conversation"));
        }
        try
        {
            int activeConnections = 0;
            Connection[] connections = call.getConnections();
            if (connections!=null)
                for (Connection connection: connections)
                    if (connection.getState() == Connection.CONNECTED)
                        ++activeConnections;

            if (activeConnections!=2)
            {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog(
                            "Not all participant are ready for conversation. Ready (%s) participant(s)"
                            , activeConnections));
                return state;
            }

            try
            {
                outgoingRtpStream.open(remoteAddress, remotePort, audioStream);
                outgoingRtpStream.start();
            }
            catch (RtpStreamException ex)
            {
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }

            state.setState(TALKING);

            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(callLog("Conversation successfully started"));

            continueConversation(EMPTY_DTMF);
            
            return state;
        }
        finally
        {
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
                IvrConversationScenarioPoint point =
                        (IvrConversationScenarioPoint) conversationState.getNextConversationPoint();
                String validDtmfs = point.getValidDtmfs();
                if (dtmfChar!=EMPTY_DTMF && (validDtmfs==null || validDtmfs.indexOf(dtmfChar)<0))
                {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(callLog("Invalid dtmf (%s). Skipping", dtmfChar));
                    return;
                }

                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Continue conversation using dtmf ("+dtmfChar+")"));
                
                audioStream.reset();
                conversationState.getBindings().put(DTMF_BINDING, ""+dtmfChar);
                Collection<Node> actions = scenario.makeConversation(conversationState);
                Collection<IvrAction> ivrActions = new ArrayList<IvrAction>(10);
                String bindingId = owner.getId()+"_"+call.getCallID().intValue();
                try
                {
                    tree.addGlobalBindings(bindingId, bindingSupport);
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
                    tree.removeGlobalBindings(bindingId);
                    bindingSupport.reset();
                }
            }
            catch(Exception e)
            {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(callLog("Error continue conversation using dtmf %s", dtmfChar));
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void stopConversation(CompletionCode completionCode)
    {
        lock.writeLock().lock();
        try
        {
            if (incomingRtpStream!=null){
                incomingRtpStream.release();
                incomingRtpStream=null;
            }
            if (state.getId()==INVALID)
                return;
            try
            {

                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Stoping the conversation"));
                outgoingRtpStream.release();
                audioStream.close();
                try
                {
                    actionsExecutor.cancelActionsExecution();
                }
                catch (InterruptedException ex)
                {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(callLog("Error canceling actions executor while stoping conversation"), ex);
                }
                try
                {
                    if (call.getState()==Call.ACTIVE)
                        call.drop();
                }
                catch (Exception ex)
                {
                    if (owner.isLogLevelEnabled(LogLevel.ERROR))
                        owner.getLogger().error(callLog("Error droping a call while stoping conversation"), ex);
                }
            }
            finally
            {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(callLog("Conversation stoped (%s)", completionCode));
            }
        }
        finally
        {
            state.setState(INVALID);
            lock.writeLock().unlock();
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
        return incomingRtpStream;
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
        return getCallId();
    }

    public String getObjectDescription()
    {
        return getCallId();
    }
}
