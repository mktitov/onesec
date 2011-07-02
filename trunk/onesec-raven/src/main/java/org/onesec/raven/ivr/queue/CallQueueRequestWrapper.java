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

import org.onesec.raven.ivr.queue.event.NumberChangedQueueEvent;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public interface CallQueueRequestWrapper extends CallQueueRequest
{
    /**
     * Sets the unique (in queue) for this request
     */
    public void setRequestId(long requestId);
    /**
     * Returns the request id
     */
    public long getRequestId();
    /**
     * Sets the call queue for request
     */
    public void setCallsQueue(CallsQueue queue);
    /**
     * Returns the calls queue for the request
     */
    public CallsQueue getCallsQueue();
    /**
     * Fires "rejected" event for the request
     */
    public void fireRejectedQueueEvent();
    /**
     * Fires "queued" event for the request
     */
    public void fireCallQueuedEvent();
    /**
     * Returns the wrapped request
     */
    public CallQueueRequest getWrappedRequest(); 
    /**
     * Sets the position of the request in the queue and fires {@link NumberChangedQueueEvent} if 
     * the position changed.
     */
    public void setPositionInQueue(Integer pos);
    /**
     * Returns the current position in the queue
     */
    public Integer getPositionInQueue();
    /**
     * Adds message to log for this request
     */
    public void addToLog(String message);
    /**
     * Returns the data context of the request
     */
    public DataContext getContext();
    /**
     * Sets the on busy behaviour for this request
     */
    public void setOnBusyBehaviour(CallsQueueOnBusyBehaviour onBusyBehaviour);
    /**
     * Returns the on busy behaviour for this request
     */
    public CallsQueueOnBusyBehaviour getOnBusyBehaviour();
    /**
     * Sets the current on busy behaviour step number
     */
    public void setOnBusyBehaviourStep(int step);
    /**
     * Returns the current on busy behaviour step number
     */
    public int getOnBusyBehaviourStep();
}
