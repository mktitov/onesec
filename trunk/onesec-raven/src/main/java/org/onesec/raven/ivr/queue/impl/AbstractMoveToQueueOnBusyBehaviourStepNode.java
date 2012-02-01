/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.queue.BehaviourResult;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviourStep;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractMoveToQueueOnBusyBehaviourStepNode extends BaseNode implements CallsQueueOnBusyBehaviourStep
{
    @NotNull @Parameter(defaultValue="false")
    private Boolean resetOnBusyBehaviour;

    @NotNull @Parameter
    private ExecutorService executor;

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Boolean getResetOnBusyBehaviour() {
        return resetOnBusyBehaviour;
    }

    public void setResetOnBusyBehaviour(Boolean resetOnBusyBehaviour) {
        this.resetOnBusyBehaviour = resetOnBusyBehaviour;
    }
    
    protected abstract CallsQueue getQueue(CallQueueRequestWrapper request);

    public BehaviourResult handleBehaviour(final CallsQueue queue, final CallQueueRequestWrapper request) {
        if (resetOnBusyBehaviour) 
            request.setOnBusyBehaviour(null);
        final CallsQueue _callsQueue = getQueue(request);
        executor.executeQuietly(new AbstractTask(this, request.logMess("Moving to the queue (%s)", _callsQueue.getName())) {
            @Override public void doRun() throws Exception {
                request.setForceResetCallsQueueFlag();
                _callsQueue.queueCall(request);
            }
        });
        return new BehaviourResultImpl(false, BehaviourResult.StepPolicy.GOTO_NEXT_STEP);
    }
}
