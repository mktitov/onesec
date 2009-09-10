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

import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationResult;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationResultImpl implements ConversationResult
{
    private long callStartTime;
    private long callEndTime;
    private CompletionCode completionCode;
    private long conversationDuration = 0;
    private long conversationStartTime = 0;

    public void setCompletionCode(CompletionCode completionCode)
    {
        this.completionCode = completionCode;
    }

    public void setConversationDuration(long conversationDuration)
    {
        this.conversationDuration = conversationDuration;
    }

    public CompletionCode getCompletionCode()
    {
        return completionCode;
    }

    public long getConversationDuration()
    {
        return conversationDuration;
    }

    public long getConversationStartTime()
    {
        return conversationStartTime;
    }

    public void setConversationStartTime(long conversationStartTime)
    {
        this.conversationStartTime = conversationStartTime;
    }

    public void setCallEndTime(long callEndTime)
    {
        this.callEndTime = callEndTime;
    }

    public void setCallStartTime(long callStartTime)
    {
        this.callStartTime = callStartTime;
    }

    public long getCallStartTime()
    {
        return callStartTime;
    }

    public long getCallEndTime()
    {
        return callEndTime;
    }

    public long getCallDuration()
    {
        return (callEndTime-callStartTime)/1000;
    }
}