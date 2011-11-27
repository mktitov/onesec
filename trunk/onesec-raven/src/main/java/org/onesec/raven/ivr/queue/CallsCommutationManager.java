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

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public interface CallsCommutationManager {
    public void callFinished(CommutationManagerCall call, boolean successfull);
    public CallsQueueOperator getOperator();
    public long getWaitTimeout();
    public long getInviteTimeout();
    public IvrConversationsBridgeManager getConversationsBridgeManager();
    public IvrEndpointPool getEndpointPool();
    public IvrConversationScenario getConversationScenario();
    public CallQueueRequestWrapper getRequest();
    public CallsQueue getQueue();
    public void incOnNoFreeEndpointsRequests();
    public ExecutorService getExecutor();
}
