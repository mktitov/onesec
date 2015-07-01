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

import org.onesec.core.call.CallCompletionCode;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.ConversationCdr;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationCdrImpl implements ConversationCdr
{
    private long callStartTime;
    private long callEndTime;
    private CompletionCode completionCode;
    private long conversationDuration = 0;
    private long conversationStartTime = 0;
    private CallCompletionCode transferCompletionCode;
    private String transferAddress;
    private long transferTime = 0;
    private long transferConversationStartTime = 0;
    private long transferConversationDuration = 0;


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
        if (this.callEndTime>0)
            return;
        
        this.callEndTime = callEndTime;
        if (conversationStartTime>0)
            conversationDuration = (callEndTime - conversationStartTime) / 1000;
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

    public String getTransferAddress()
    {
        return transferAddress;
    }

    public void setTransferAddress(String transferAddress)
    {
        this.transferAddress = transferAddress;
    }

    public CallCompletionCode getTransferCompletionCode()
    {
        return transferCompletionCode;
    }

    public void setTransferCompletionCode(CallCompletionCode transferCompletionCode)
    {
        this.transferCompletionCode = transferCompletionCode;
    }

    public long getTransferConversationDuration()
    {
        return transferConversationDuration;
    }

    public void setTransferConversationDuration(long transferConversationDuration)
    {
        this.transferConversationDuration = transferConversationDuration;
    }

    public long getTransferConversationStartTime()
    {
        return transferConversationStartTime;
    }

    public void setTransferConversationStartTime(long transferConversationStartTime)
    {
        this.transferConversationStartTime = transferConversationStartTime;
    }

    public long getTransferTime()
    {
        return transferTime;
    }

    public void setTransferTime(long transferTime)
    {
        this.transferTime = transferTime;
    }
}