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

import java.util.List;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface CallsQueuePrioritySelector extends Node
{
    /**
     * Returns the priority with which or higher this selector work
     */
    public Integer getPriority();
    /**
     * Returns the operators for this priority. Never returns null (returns empty list if 
     * no operators for this priority selector)
     */
    public List<CallsQueueOperatorRef> getOperatorsRefs();
    /**
     * Returns "on busy" behaviour for this selector
     */
    public CallsQueueOnBusyBehaviour getOnBusyBehaviour();

    public void rebuildIndex(List<CallsQueueOperatorRef> operators, int handledByOperator);

    public int getStartIndex(CallQueueRequestWrapper request);
}
