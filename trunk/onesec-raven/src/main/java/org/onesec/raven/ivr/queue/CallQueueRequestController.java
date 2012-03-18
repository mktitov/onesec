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

import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.queue.event.NumberChangedQueueEvent;
import org.raven.ds.DataContext;

/**
 *
 * @author Mikhail Titov
 */
public interface CallQueueRequestController extends CallQueueRequest
{
    /**
     * Returns the request id
     */
    public long getRequestId();
    /**
     * 
     */
    public void setForceResetCallsQueueFlag();
    /**
     * Sets the call queue for request
     */
    public void setCallsQueue(CallsQueue queue);
    /**
     * Returns the calls queue for the request
     */
    public CallsQueue getCallsQueue();
    /**
     * Returns the target (first) queue for request. 
     * The first call of {@link #setCallsQueue} specifies the
     * return value for this method
     */
    public CallsQueue getTargetQueue();
    /**
     * Fires "rejected" event for the request
     */
    public void fireRejectedQueueEvent();
    /**
     * Fires "queued" event for the request
     */
    public void fireCallQueuedEvent();
    /**
     * Fires "ReadyToCommutateQueueEvent" for the request.
     * @return <b>true</b> if the request is not assigned to operator. If method return <b>false</b> then
     *      request is already handling by operator (commutated or commutation is in process), in this case
     *      commutation manager call must terminate the call to operator
     */
    public boolean fireReadyToCommutateQueueEvent(CommutationManagerCall operator);
    /**
     * Fires when operator and abonent where commutated
     */
    public void fireCommutatedEvent();
    /**
     * Fires when commutation was disconnected
     */
    public void fireDisconnectedQueueEvent();
    /**
     * Fires when request assigned to operator
     */
    public void fireOperatorQueueEvent(String operatorId);
    /**
     * Fires when queue tries to call to operator using number passed in the parameter
     */
//    public void fireOperatorNumberQueueEvent(String operatorNumber);
    /**
     * Sets the operator greeting audio file
     */
    public void fireOperatorGreetingQueueEvent(AudioFile greeting);
    /**
     * Fires when call were transfered to other operator
     */
    public void fireCallTransfered(String operatorId, String operatorNumber);
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
     * Returns <b>true</b> if the request where assigned to the operator and operator are ready to commutate
     */
    public boolean isHandlingByOperator();
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
    /**
     * Returns the time of the last {@link #fireCallQueuedEvent()} call
     * @return
     */
    public long getLastQueuedTime();
    /**
     * Increases the number of operators who passed through the request in current queue
     */
    public void incOperatorHops();
    /**
     * Returns the number of operators who passed through the request in current queue
     */
    public int getOperatorHops();
    /**
     * Adds a request wrapper listener
     */
    public void addRequestWrapperListener(RequestWrapperListener listener);
}
