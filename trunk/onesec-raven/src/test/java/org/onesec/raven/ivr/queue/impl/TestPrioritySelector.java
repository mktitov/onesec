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

import java.util.ArrayList;
import java.util.List;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestPrioritySelector extends BaseNode implements CallsQueuePrioritySelector
{
    private Integer priority;
    private List<CallsQueueOperatorRef> operatorRefs;
    private CallsQueueOnBusyBehaviour onBusyBehaviour;

    public TestPrioritySelector(
            String name, Integer priority, CallsQueueOnBusyBehaviour onBusyBehaviour) 
    {
        super(name);
        this.priority = priority;
        this.onBusyBehaviour = onBusyBehaviour;
        operatorRefs = new ArrayList<CallsQueueOperatorRef>();
    }

    public Integer getPriority() {
        return priority;
    }
    
    public void addOperatorRef(CallsQueueOperatorRef operatorRef) {
        operatorRefs.add(operatorRef);
    }

    public List<CallsQueueOperatorRef> getOperatorsRefs() {
        return operatorRefs;
    }

    public CallsQueueOnBusyBehaviour getOnBusyBehaviour() {
        return onBusyBehaviour;
    }
    
}
