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

package org.onesec.raven.ivr;

import org.onesec.core.ObjectDescription;
import org.raven.conv.ConversationScenarioState;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrEndpointConversation extends ObjectDescription
{
    public final static char EMPTY_DTMF = '-';
    public final static String DTMF_BINDING = "dtmf";
    public final static String DTMFS_BINDING = "dtmfs";
    public final static String DISABLE_AUDIO_STREAM_RESET = "disableAudioStreamReset";
    public final static String CONVERSATION_STATE_BINDING = "conversationState";
    public final static String VARS_BINDING = "vars";
    public final static String NUMBER_BINDING = "number";
    public final static String CALLED_NUMBER_BINDING = "calledNumber";    
    public final static String LAST_REDIRECTED_NUMBER = "lastRedirectedNumber";

    /**
     * Adds the listener to this conversation
     * @param listener the listener
     * @see #removeConversationListener
     */
    public void addConversationListener(IvrEndpointConversationListener listener);
    /**
     * Removes the listener from this conversation
     * @param listener the listener
     * @see #addConversationListener
     */
    public void removeConversationListener(IvrEndpointConversationListener listener);
    /**
     * Returns the node owned by this conversation.
     */
    public Node getOwner();
    /**
     * Returns the conversation logger
     */
    public Logger getLogger();
    /**
     * Returns the executor service
     */
    public ExecutorService getExecutorService();
    /**
     * Returns the audio stream
     */
    public AudioStream getAudioStream();
    /**
     * Returns the incoming rtp stream of the conversation
     */
    public IncomingRtpStream getIncomingRtpStream();
    /**
     * Returns the number (address) of the calling side.
     */
    public String getCallingNumber();
    /**
     * Returns the number (address) of the called side.
     */
    public String getCalledNumber();
    /**
     * Returns last redirected number
     */
    public String getLastRedirectedNumber();
    /**
     * Continues the conversation with passed in the parameter dtmf char
     * @param dtmfChar the dtmf char
     */
    public void continueConversation(char dtmfChar);
    /**
     * Stops the current conversation
     */
    public void stopConversation(CompletionCode completionCode);
    /**
     * Returns the conversation scenario state
     */
    public ConversationScenarioState getConversationScenarioState();
    /**
     * Returns the current conversation state
     */
    public IvrEndpointConversationState getState();
    /**
     * Returns true if rtp streams were started and all logical connections established
     */
    public boolean isConnectionEstablished();
    /**
     * Transfers current call to the address passed in the parameter.
     * @param address The destination telephone address string to where the Call is being
     *      transferred
     * @param monitorTransfer if <code>true</code> then method will monitor call transfer, etc will
     *      wait until transfered call end
     * @param callStartTimeout
     * @param callEndTimeout
     */
    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout);
    /**
     * Transfers current call to the address passed in the parameter.
     */
    public void transfer(String address) throws IvrEndpointConversationException;
    /**
     * Parks conversation call and return number where call parked
     * @throws IvrEndpointConversationException 
     */
    public String park() throws IvrEndpointConversationException;
    /**
     * UnParks call parked by {@link #park()} method
     * @param parkDN
     * @throws IvrEndpointConversationException 
     */
    public void unpark(String parkDN) throws IvrEndpointConversationException;
    /**
     * Sends text message to the one of the conversation participants. 
     * @param message the message that will be sent to terminal
     * @param encoding in this encoding message will be sent to the terminal
     * @param direction point to the terminal to which the message will be sent
     */
    public void sendMessage(String message, String encoding, SendMessageDirection direction);
    /**
     * Sends every char in <b>digits</b> as dtmf signals.
     */
    public void sendDTMF(String digits);
    public void makeLogicalTransfer(String opponentNumber);
}
