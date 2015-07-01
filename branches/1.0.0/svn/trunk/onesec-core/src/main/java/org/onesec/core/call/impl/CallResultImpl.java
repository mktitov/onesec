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

package org.onesec.core.call.impl;

import org.onesec.core.call.CallCompletionCode;
import org.onesec.core.call.CallResult;

/**
 *
 * @author Mikhail Titov
 */
public class CallResultImpl implements CallResult
{
    private CallCompletionCode completionCode;
    private int conversationDuration;
    private long callStartTime;
    private long conversationStartTime;

    public CallResultImpl(long callStartTime)
    {
        this.callStartTime = callStartTime;
    }

    public CallResultImpl(CallCompletionCode completionCode)
    {
        this.completionCode = completionCode;
    }
    
    public void markConversationEnd()
    {
        if (conversationStartTime>0)
            conversationDuration = (int) (System.currentTimeMillis() - conversationStartTime)/1000;
    }

    public void markConversationStart()
    {
        conversationStartTime = System.currentTimeMillis();
    }

    public CallCompletionCode getCompletionCode()
    {
        return completionCode;
    }

    public void setCompletionCode(CallCompletionCode completionCode)
    {
        this.completionCode = completionCode;
    }

    public int getConversationDuration()
    {
        return conversationDuration;
    }

    public long getCallStartTime()
    {
        return callStartTime;
    }
    
    public void setCallStartTime(long callStartTime)
    {
        this.callStartTime = callStartTime;
    }

    public long getConversationStartTime()
    {
        return conversationStartTime;
    }
}
