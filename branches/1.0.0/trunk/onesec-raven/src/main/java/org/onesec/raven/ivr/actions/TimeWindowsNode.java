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
package org.onesec.raven.ivr.actions;

import org.raven.annotations.NodeClass;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, childNodes=TimeWindowNode.class)
public class TimeWindowsNode extends BaseNode {
    
    public final static String NAME = "Time windows";
    
    public TimeWindowsNode() {
        super(NAME);
    }

    @Override
    public void setName(String name) {
    }

    public boolean isCurrentTimeInPeriod() {
        boolean res = false;
        for (TimeWindowNode node: NodeUtils.getChildsOfType(this, TimeWindowNode.class))
            if (node.isCurrentTimeInPeriod()) {
                res = true;
                break;
            }
        return ((TimeWindowsGroupActionNode)getParent()).getInvertResult()? !res : res;
    }
}
