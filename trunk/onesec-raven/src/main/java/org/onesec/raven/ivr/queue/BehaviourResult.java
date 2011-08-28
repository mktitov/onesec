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

/**
 *
 * @author Mikhail Titov
 */
public interface BehaviourResult
{
    /**
     * Returns <b>true</b> if the request must stay in the queue else request will be removed from the queue
     */
    public boolean isLeaveInQueue();
    /**
     * Returns <b>true</b> if the {@link CallsQueueOnBusyBehaviour} must stay at this step. If returns
     * <b>false</b> the {@link CallsQueueOnBusyBehaviour} goes to the next
     * {@link CallsQueueOnBusyBehaviourStep step}
     */
    public boolean isLeaveAtThisStep();
}
