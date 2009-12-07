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
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.InvalidStateException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.PrivilegeViolationException;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.callcontrol.CallControlCall;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.conv.ConversationScenario;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import static org.onesec.raven.ivr.IvrEndpointConversationState.*;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationImpl implements IvrEndpointConversation
{
    private final Node owner;
    private final ExecutorService executor;
    private final ConversationScenario scenario;
    private final OutgoingRtpStream rtpStream;
    private final CallControlCall call;
    private final IvrActionsExecutor actionsExecutor;
    private final AudioStream audioStream;
    private final ConversationScenarioState conversationState;
    private final String remoteAddress;
    private final int remotePort;
    private IvrEndpointConversationState state;


    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IvrEndpointConversationImpl(
            Node owner, ExecutorService executor, ConversationScenario scenario
            , OutgoingRtpStream rtpStream, CallControlCall call, String remoteAddress, int remotePort)
        throws Exception
    {
        this.owner = owner;
        this.executor = executor;
        this.scenario = scenario;
        this.rtpStream = rtpStream;
        this.call = call;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        conversationState = this.scenario.createConversationState();
        conversationState.setBinding(DTMF_BINDING, "-", BindingScope.REQUEST);
        conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
        conversationState.setBinding(VARS_BINDING, new HashMap(), BindingScope.CONVERSATION);
        conversationState.setBinding(
                CONVERSATION_STATE_BINDING, conversationState, BindingScope.CONVERSATION);
        audioStream = new ConcatDataSource(FileTypeDescriptor.WAVE, executor, 160, owner);
        actionsExecutor = new IvrActionsExecutor(this, executor);
        
        state = READY;
    }

    public IvrEndpointConversationState startConversation()
    {
        lock.writeLock().lock();
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
                    owner.getLogger().debug(String.format(
                            "Conversation. Not all participant are ready for conversation. Ready (%s) " +
                            "participant(s)"
                            , activeConnections));
                return READY;
            }

            try
            {
                rtpStream.open(remoteAddress, remotePort, audioStream);
            }
            catch (RtpStreamException ex)
            {
                stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }

            state = TALKING;
            
            return state;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void continueConversation(char dtmfChar)
    {
        
    }

    public void stopConversation(CompletionCode completionCode)
    {
        lock.writeLock().lock();
        try
        {
            rtpStream.release();
            audioStream.close();
            try
            {
                actionsExecutor.cancelActionsExecution();
            }
            catch (InterruptedException ex)
            {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(
                            String.format(
                                "%s. Error canceling actions executor while stoping conversation"
                                , getCallId())
                            , ex);
            }
            try
            {
                if (call.getState()==Call.ACTIVE)
                    call.drop();
            }
            catch (Exception ex)
            {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(
                            String.format(
                                "%s. Error droping a call while stoping conversation"
                                , getCallId())
                            , ex);
            }
        }
        finally
        {
            state = INVALID;
            lock.writeLock().unlock();
        }
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

    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getCallId()
    {
        return "Conversation <- "+((CiscoCall)call).getCurrentCalledPartyDisplayName()+". ";
    }

}
