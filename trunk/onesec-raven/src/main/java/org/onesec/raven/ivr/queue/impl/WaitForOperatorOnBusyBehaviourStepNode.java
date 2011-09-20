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

import org.onesec.raven.ivr.queue.BehaviourResult;
import org.onesec.raven.ivr.queue.BehaviourResult.StepPolicy;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviourStep;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOnBusyBehaviourNode.class)
public class WaitForOperatorOnBusyBehaviourStepNode extends BaseNode implements CallsQueueOnBusyBehaviourStep
{
    @NotNull @Parameter
    private Integer waitTimeout;

    public BehaviourResult handleBehaviour(CallsQueue queue, CallQueueRequestWrapper request)
    {
        StepPolicy stepPolicy = System.currentTimeMillis()-request.getLastQueuedTime()<=waitTimeout*1000?
            StepPolicy.LEAVE_AT_THIS_STEP : StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP;
        if (stepPolicy==StepPolicy.LEAVE_AT_THIS_STEP) 
            request.setOperatorIndex(-1);
        return new BehaviourResultImpl(true, stepPolicy);
    }

    public Integer getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(Integer waitTimeout) {
        this.waitTimeout = waitTimeout;
    }
}
