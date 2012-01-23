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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.onesec.raven.ivr.queue.OperatorsUsagePolicy;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueNode.class)
public class CallsQueuePrioritySelectorNode extends BaseNode implements CallsQueuePrioritySelector
{
    @NotNull @Parameter()
    private Integer priority;
    
    @NotNull @Parameter(defaultValue="SEQUENCE_USAGE")
    private OperatorsUsagePolicy operatorsUsagePolicy;

    private CallsQueueOnBusyBehaviourNode onBusyBehaviour;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes();
    }
    
    private void initNodes() {
        onBusyBehaviour = 
                (CallsQueueOnBusyBehaviourNode)getChildren(CallsQueueOnBusyBehaviourNode.NAME);
        if (onBusyBehaviour==null){
            onBusyBehaviour = new CallsQueueOnBusyBehaviourNode();
            addAndSaveChildren(onBusyBehaviour);
            onBusyBehaviour.start();
        }
    }
    
    public CallsQueueOnBusyBehaviour getOnBusyBehaviour() {
        return onBusyBehaviour;
    }
    
    public List<CallsQueueOperatorRef> getOperatorsRefs() 
    {
        List<CallsQueueOperatorRef> operators = NodeUtils.getChildsOfType(
                this, CallsQueueOperatorRef.class);            
        if (operatorsUsagePolicy==OperatorsUsagePolicy.UNIFORM_USAGE)
            Collections.sort(operators, new OperatorRefComparator());
        return operators;
    }

    public void rebuildIndex(List<CallsQueueOperatorRef> operators, int handledByOperator) 
    {
        if (operatorsUsagePolicy==OperatorsUsagePolicy.SEQUENCE_USAGE)
            return;
        operators.add(operators.remove(handledByOperator));
        int i=0;
        for (CallsQueueOperatorRef operator: operators)
            operator.setSortIndex(i++);
    }

    public int getStartIndex(CallQueueRequestWrapper request, int operatorsCount){
        if (operatorsUsagePolicy==OperatorsUsagePolicy.SEQUENCE_USAGE)
            return request.getOperatorIndex()+1;
        else 
            return request.getOperatorHops()<operatorsCount? 0 : operatorsCount;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public OperatorsUsagePolicy getOperatorsUsagePolicy() {
        return operatorsUsagePolicy;
    }

    public void setOperatorsUsagePolicy(OperatorsUsagePolicy operatorsUsagePolicy) {
        this.operatorsUsagePolicy = operatorsUsagePolicy;
    }

}
