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

import org.onesec.core.call.CallCompletionCode;

/**
 * Contains the information of conversation result
 * @see IvrEndpoint#invite
 * @author Mikhail Titov
 */
public interface ConversationCdr
{
    /**
     * Returns the call start time
     * @return
     */
    public long getCallStartTime();
    /**
     * Returns the call end time
     */
    public long getCallEndTime();
    /**
     * Returns the call duration in seconds
     */
    public long getCallDuration();
    /**
     * Returns the conversation completion code
     */
    public CompletionCode getCompletionCode();

    /**
     * Return the dureation of the conversation in seconds. For completion codes
     * {@link CompletionCode#OPPENT_BUSY} and {@link CompletionCode#OPPENT_NO_ANSWER} method always
     * returns 0.
     */
    public long getConversationDuration();

    /**
     * Returns the conversation start time in milliseconds.
     */
    public long getConversationStartTime();
    /**
     * Returns completion code for transfered call or null if the call was not transfered
     */
    public CallCompletionCode getTransferCompletionCode();
    /**
     * Returns the address to which call was transfered or null if the call was not transfered
     */
    public String getTransferAddress();
    /**
     * Returns the time when transfer was initiated or 0 (zero) if the call was not transfered
     */
    public long getTransferTime();
    /**
     * Returns the time when conversation of the transfered call was started.
     * Method returns 0 (zero) if the call was not transfered or transfer monitor was not enabled.
     */
    public long getTransferConversationStartTime();
    /**
     * Returns the conversation duration of the transfered call.
     * Method returns 0 (zero) if the call was not transfered or transfer monitor was not enabled
     * or number to which call was transfered not ready or not answered.
     */
    public long getTransferConversationDuration();
}
