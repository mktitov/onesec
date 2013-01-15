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
package org.onesec.raven.ivr.queue.impl;

import java.util.Collection;
import java.util.Collections;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestCallsQueueNode extends BaseNode implements CallsQueue {

    CallQueueRequestController lastRequest;
    
    public void queueCall(CallQueueRequestController request) {
        this.lastRequest = request;
    }

    public Collection<CallQueueRequestController> getRequests() {
        return Collections.EMPTY_LIST;
    }

    public int getActiveOperatorsCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getAvgCallDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCallDuration(int callDuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
