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

import java.util.Map;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.RtpAddress;
import org.onesec.raven.ivr.RtpStreamManager;
import org.raven.conv.ConversationScenarioState;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestIvrEndpoint extends BaseNode implements IvrEndpoint
{
    private IvrEndpointStateImpl endpointState;

    @Override
    protected void initFields()
    {
        super.initFields();
        endpointState = new IvrEndpointStateImpl(this);
        endpointState.setState(IvrEndpointState.IN_SERVICE);
    }

    public String getAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrEndpointState getEndpointState() {
        return endpointState;
    }

//    public void invite(String opponentNumber, IvrConversationScenario conversationScenario, ConversationCompletionCallback callback, Map<String, Object> bindings) throws IvrEndpointException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    public Node getOwner() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExecutorService getExecutorService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioStream getAudioStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void continueConversation(char dtmfChar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stopConversation(CompletionCode completionCode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ConversationScenarioState getConversationScenarioState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getActiveCallsCount() {
        return 0;
    }

    public String getObjectName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getObjectDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RtpAddress getRtpAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addConversationListener(IvrEndpointConversationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeConversationListener(IvrEndpointConversationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RtpStreamManager getRtpStreamManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExecutorService getExecutor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrConversationScenario getConversationScenario() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Codec getCodec() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getRtpPacketSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getRtpMaxSendAheadPacketsCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getEnableIncomingRtp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getEnableIncomingCalls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , IvrEndpointConversationListener listener, IvrConversationScenario scenario
            , Map<String, Object> bindings) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
