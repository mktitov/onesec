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

/**
 *
 * @author Mikhail Titov
 */
public interface QueuedCallStatus extends CallQueueRequest
{
    public enum Status {QUEUEING, READY_TO_COMMUTATE, COMMUTATING, COMMUTATED, DISCONNECTED, REJECTED}

    /**
     * Return current status of the queued call
     */
    public Status getStatus();
    /**
     * Returns <b>true</b> if call is queuing now
     */
    public boolean isQueueing();
    /**
     * Returns <b>true</b> if queue is ready to commutate abonent with operator
     */
    public boolean isReadyToCommutate();
    /**
     * Returns <b>true</b> if abonent was commutated with operator
     */
    public boolean isCommutated();
    /**
     * Returns <b>true</b> if abonent and operator commutation was disconnected
     */
    public boolean isDisconnected();
    /**
     * Returns the serial number of call in the queue or -1 if position in the queue is unknown
     */
    public int getSerialNumber();
    /**
     * Returns the previous serial number of the call or -1 if previous position is unknown
     */
    public int getPrevSerialNumber();

    public void replayToReadyToCommutate();
    /**
     * Returns the operator's greeting audio file.
     */
    public AudioFile getOperatorGreeting();
}
