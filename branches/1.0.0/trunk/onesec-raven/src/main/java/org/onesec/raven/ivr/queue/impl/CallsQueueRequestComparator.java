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

import java.util.Comparator;
import org.onesec.raven.ivr.queue.CallQueueRequestController;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueRequestComparator implements Comparator<CallQueueRequestController>
{
    public int compare(CallQueueRequestController o1, CallQueueRequestController o2)
    {
        int res = new Integer(o1.getPriority()).compareTo(o2.getPriority());
        if (res==0 && o1!=o2)
            res = new Long(o1.getRequestId()).compareTo(o2.getRequestId());
        return res;
    }
}