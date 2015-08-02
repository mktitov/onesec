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

import org.onesec.raven.ivr.*;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationCdrRegistrator implements IvrEndpointConversationListener {
    protected final ConversationCdrImpl cdr = new ConversationCdrImpl();

    public ConversationCdr getCdr(){
        return cdr;
    }

    @Override
    public void connectionEstablished(IvrEndpointConversationEvent event) {
    }

    public void listenerAdded(IvrEndpointConversationEvent event) {
        cdr.setCallStartTime(System.currentTimeMillis());
    }

    public void conversationStarted(IvrEndpointConversationEvent event) {
        cdr.setConversationStartTime(System.currentTimeMillis());
    }

    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
        cdr.setCallEndTime(System.currentTimeMillis());
        cdr.setCompletionCode(event.getCompletionCode());
    }

    public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
        cdr.setTransferConversationStartTime(System.currentTimeMillis());
    }

    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }

    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }

    public void dtmfReceived(IvrDtmfReceivedConversationEvent event) { }
}
