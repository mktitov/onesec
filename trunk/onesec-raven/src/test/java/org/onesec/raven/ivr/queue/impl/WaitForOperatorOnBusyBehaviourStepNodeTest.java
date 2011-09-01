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

import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.queue.BehaviourResult;
import org.onesec.raven.ivr.queue.BehaviourResult.StepPolicy;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class WaitForOperatorOnBusyBehaviourStepNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test()
    {
        WaitForOperatorOnBusyBehaviourStepNode waiter = new WaitForOperatorOnBusyBehaviourStepNode();
        waiter.setName("waiter");
        tree.getRootNode().addAndSaveChildren(waiter);
        waiter.setWaitTimeout(5);

        CallQueueRequestWrapper req = createStrictMock(CallQueueRequestWrapper.class);
        long time = System.currentTimeMillis();
        expect(req.getLastQueuedTime()).andReturn(time);
        req.setOperatorIndex(-1);
        expect(req.getLastQueuedTime()).andReturn(time-3000);
        req.setOperatorIndex(-1);
        expect(req.getLastQueuedTime()).andReturn(time-6000);

        replay(req);

        BehaviourResult res = waiter.handleBehaviour(null, req);
        assertTrue(res.isLeaveInQueue());
        assertEquals(StepPolicy.LEAVE_AT_THIS_STEP, res.getNextStepPolicy());
        
        res = waiter.handleBehaviour(null, req);
        assertTrue(res.isLeaveInQueue());
        assertEquals(StepPolicy.LEAVE_AT_THIS_STEP, res.getNextStepPolicy());
        
        res = waiter.handleBehaviour(null, req);
        assertTrue(res.isLeaveInQueue());
        assertEquals(StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP, res.getNextStepPolicy());

        verify(req);
    }
}