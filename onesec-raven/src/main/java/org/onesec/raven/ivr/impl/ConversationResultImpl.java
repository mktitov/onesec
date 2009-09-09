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
    private CompletionCode completionCode;
    private int conversationDuration;

    public void setCompletionCode(CompletionCode completionCode)
    {
        this.completionCode = completionCode;
    }

    public void setConversationDuration(int conversationDuration)
    {
        this.conversationDuration = conversationDuration;
    }

    public CompletionCode getCompletionCode()
    {
        return completionCode;
    }

    public int getConversationDuration()
    {
        return conversationDuration;
    }
}