/*
 * Copyright 2013 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class)
public class InformerTimeWindow extends TimeWindowNode {
    @Parameter
    private Integer maxSessionsCount;
    
    @Parameter
    private Integer priority;

    public Integer getMaxSessionsCount() {
        return maxSessionsCount;
    }

    public void setMaxSessionsCount(Integer maxSessionsCount) {
        this.maxSessionsCount = maxSessionsCount;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
