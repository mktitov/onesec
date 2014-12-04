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
package org.onesec.raven.ivr.queue.event.impl;

import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;

/**
 *
 * @author Mikhail Titov
 */
public class DisconnectedQueueEventImpl extends CallQueueEventImpl implements DisconnectedQueueEvent {
    private final String cause;
    
    public DisconnectedQueueEventImpl(CallsQueue callsQueue, long requestId, String cause) {
        super(callsQueue, requestId);
        this.cause = cause;
    }

    public String getCause() {
        return cause;
    }
}
