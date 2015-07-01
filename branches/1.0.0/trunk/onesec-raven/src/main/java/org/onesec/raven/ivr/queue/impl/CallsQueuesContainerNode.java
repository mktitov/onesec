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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.annotations.NodeClass;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class CallsQueuesContainerNode extends BaseNode implements Viewable {
    
    public static final String NAME = "Queues";
    
    @Message private static String queueNameMessage;
    @Message private static String numberInQueueMessage;
    @Message private static String priorityMessage;
    @Message private static String requestIdMessage;
    @Message private static String queueTimeMessage;
    @Message private static String targetQueueMessage;
    @Message private static String nextOnBusyBehaviourStepMessage;
    @Message private static String lastOperatorIndexMessage;
    @Message private static String requestMessage;

    
    public CallsQueuesContainerNode() {
        super(NAME);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        TableImpl tab = new TableImpl(new String[]{queueNameMessage, numberInQueueMessage, 
                requestIdMessage, priorityMessage, queueTimeMessage, targetQueueMessage, 
                nextOnBusyBehaviourStepMessage, lastOperatorIndexMessage, requestMessage});
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        int numInQueue = 1;
        for (CallsQueue queue: NodeUtils.getChildsOfType(this, CallsQueue.class))
            for (CallQueueRequestController req: queue.getRequests()) 
                    tab.addRow(new Object[]{queue.getName(), numInQueue++, req.getRequestId()
                            , req.getPriority()
                            , fmt.format(new Date(req.getLastQueuedTime()))
                            , req.getTargetQueue().getName()
                            , req.getOnBusyBehaviourStep(), req.getOperatorIndex(), req.toString()
                    });
        return Arrays.asList((ViewableObject)new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, tab));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}
