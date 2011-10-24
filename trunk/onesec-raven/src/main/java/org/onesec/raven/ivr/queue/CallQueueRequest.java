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

package org.onesec.raven.ivr.queue;

import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public interface CallQueueRequest
{
    /**
     * Returns the conversation for the call
     */
    public IvrEndpointConversation getConversation();
    /**
     * Informs the request that something changed for this request in the queue.
     * @see CallQueuedEvent
     * @see NumberChangedQueueEvent
     * @see ReadyToCommutateQueueEvent
     * @see CallCommutatedQueueEvent
     * @see DisconnectedQueueEvent
     * @see RejectedQueueEvent
     */
    public void callQueueChangeEvent(CallQueueEvent event);
    /**
     * Returns the request priority
     */
    public int getPriority();
    /**
     * Sets request priority
     */
    public void setPriority(int priority);
    /**
     * Returns the queue id to which request is addressed
     */
    public String getQueueId();
    /**
     * Set's the queue id
     */
    public void setQueueId(String queueId);
    /**
     * Returns <b>true</b> if the request was canceled from the abonent scenario side
     */
    public boolean isCanceled();

    public void addRequestListener(CallQueueRequestListener listener);
    /**
     * Returns data context
     */
    public DataContext getContext();
    /**
     * Returns the operator phone numbers
     */
    public String getOperatorPhoneNumbers();
    /**
     * Sets the operator phone numbers
     */
    public void setOperatorPhoneNumbers(String phoneNumbers);
}
