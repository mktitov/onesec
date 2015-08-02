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

package org.onesec.raven.ivr;

/**
 * The listener interface for the {@link IvrEndpointConversation}
 * @author Mikhail Titov
 */
public interface IvrEndpointConversationListener
{
    /**
     * Fires when listener added to the conversation. The advantage to execute code inside 
     * this method is that the conversation state can not change at the time of execution of this
     * method
     */
    public void listenerAdded(IvrEndpointConversationEvent event);
    /**
     * Fires when logical connection was established
     */
    public void connectionEstablished(IvrEndpointConversationEvent event);
    /**
     * Fires when conversation was started.
     */
    public void conversationStarted(IvrEndpointConversationEvent event);
    /**
     * Fires when conversation was stopped.
     */
    public void conversationStopped(IvrEndpointConversationStoppedEvent event);
    /**
     * Fires when conversation was transfered to the number passed in the parameter
     * @address number to which conversation was transfered
     */
    public void conversationTransfered(IvrEndpointConversationTransferedEvent event);
    /**
     * Fires when incoming RTP of the conversation were started
     */
    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event);
    /**
     * Fires when outgoing RTP of the conversation were started
     */
    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event);
    
    public void dtmfReceived(IvrDtmfReceivedConversationEvent event);
}
