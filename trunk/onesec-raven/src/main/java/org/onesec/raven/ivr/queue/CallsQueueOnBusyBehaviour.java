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
public interface CallsQueueOnBusyBehaviour 
{
    /**
     * Handles onBusy behaviour
     * @param queue the current queue of the request
     * @param request the request
     * @return <b>true</b> if request must stay in the current queue or <b>false</b> if request
     *      must be removed from the queue
     */
    public boolean handleBehaviour(CallsQueue queue, CallQueueRequestWrapper request);
}
