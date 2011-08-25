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
     * Fires "ReadyToCommutateQueueEvent" for the request
     */
    public void fireReadyToCommutateQueueEvent(CallsCommutationManager operator);
    /**
     * Fires when operator and abonent where commutated
     */
    public void fireCommutatedEvent();
    /**
     * Fires when commutation was disconnected
     */
    public void fireDisconnectedQueueEvent();
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
     * Sets the "on busy" behaviour step number (zero based) that must be executed on the next 
     * "on busy" event
     */
    public void setOnBusyBehaviourStep(int step);
    /**
     * Return the "on busy" behaviour step number (zero based) that must be executed on the next 
     * "on busy" event
     */
    public int getOnBusyBehaviourStep();
    /**
     * Returns <b>true</b> if the request valid at this moment of time. If method returns 
     * <b>false</b> then queue must cancel request processing.
     */
    public boolean isValid();
    /**
     * Sets the index to the last operator that tried to handle the request. The queue must take the
     * next operator in the next attempt.
     */
    public void setOperatorIndex(int index);
    /**
     * Returns the index of last operator that tried to handle the request. The default value is -1
     * @see #setOperatorIndex(Integer) 
     */
    public int getOperatorIndex();
    /**
     * Forms the log message
     */
    public String logMess(String Message, Object... args);
}
