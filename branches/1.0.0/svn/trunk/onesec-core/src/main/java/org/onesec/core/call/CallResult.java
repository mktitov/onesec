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

package org.onesec.core.call;

/**
 *
 * @author Mikhail Titov
 */
public interface CallResult
{
    /**
     * Returns the call completion code
     */
    public CallCompletionCode getCompletionCode();
    /**
     * Returns the conversation duration. For {@link #getCompletionCode() completion codes}:
     * {@link CallCompletionCode#ERROR}, {@link CallCompletionCode#NO_ANSWER} method always returns
     * 0 (zero)
     */
    public int getConversationDuration();
    /**
     * Returns the call start time.
     */
    public long getCallStartTime();
    /**
     * Returns the conversation start time. For {@link #getCompletionCode() completion codes}:
     * {@link CallCompletionCode#ERROR}, {@link CallCompletionCode#NO_ANSWER} method always returns
     * 0 (zero)
     */
    public long getConversationStartTime();
}
