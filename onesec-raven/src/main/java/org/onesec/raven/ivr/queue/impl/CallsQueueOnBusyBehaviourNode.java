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

import java.util.List;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviourStep;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueOnBusyBehaviourNode extends BaseNode implements CallsQueueOnBusyBehaviour
{
    public static final String NAME = "onBusyBehaviour";
    public static final String REACHED_THE_END_OF_SEQ = 
            "reached the end of the \"on busy behaviour steps\" sequence";

    public CallsQueueOnBusyBehaviourNode() {
        super(NAME);
    }

    public boolean handleBehaviour(CallsQueue queue, CallQueueRequestWrapper request) 
    {
        List<CallsQueueOnBusyBehaviourStep> steps = NodeUtils.getChildsOfType(
                this, CallsQueueOnBusyBehaviourStep.class);
        int step = request.getOnBusyBehaviourStep();
        try {
            if (steps.isEmpty() || step>=steps.size()){
                request.addToLog(REACHED_THE_END_OF_SEQ);
                request.fireRejectedQueueEvent();
                return false;
            } else
                return steps.get(step).handleBehaviour(queue, request);
        } finally {
            request.setOnBusyBehaviourStep(++step);
        }
    }
}