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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ChildsAsTableViewableObject;
import org.raven.tree.impl.InvisibleNode;
import static org.onesec.raven.ivr.queue.impl.AbstractOperatorNode.*;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class CallsQueueOperatorsNode extends BaseNode implements Viewable
{
    public static final String NAME = "Operators";
    
    public CallsQueueOperatorsNode() {
        super(NAME);
    }

    @Override
    public void setName(String name) {
    }
    
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        String[] attrNames = new String[]{"nodeTitle", ACTIVE_ATTR, BUSY_ATTR
                , PROCESSING_REQUEST_COUNT, TOTAL_REQUESTS_ATTR, HANDLED_REQUESTS_ATTR
                , ON_BUSY_REQUESTS_ATTR, ON_NOT_STARTED_REQUESTS
                , ON_NO_FREE_ENDPOINTS_REQUESTS_ATTR, ON_NOT_STARTED_REQUESTS
                , PROCESSING_REQUEST_ATTR};
        return Arrays.asList((ViewableObject)new ChildsAsTableViewableObject(
                this, attrNames, attrNames));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}
