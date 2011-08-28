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
    public enum StepPolicy {
        LEAVE_AT_THIS_STEP(0), GOTO_NEXT_STEP(1), IMMEDIATELY_EXECUTE_NEXT_STEP(1);

        private final int incStepBy;

        private StepPolicy(int incStepBy) {
            this.incStepBy = incStepBy;
        }

        public int getIncStepBy() {
            return incStepBy;
        }
    }
    /**
     * Returns <b>true</b> if the request must stay in the queue else request will be removed from the queue
     */
    public boolean isLeaveInQueue();
    /**
     */
    public StepPolicy getNextStepPolicy();
}
