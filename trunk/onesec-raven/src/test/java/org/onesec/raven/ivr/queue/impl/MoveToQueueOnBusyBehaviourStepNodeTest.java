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

import org.easymock.IArgumentMatcher;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.queue.BehaviourResult;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.test.ExecutorServiceWrapperNode;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class MoveToQueueOnBusyBehaviourStepNodeTest extends OnesecRavenTestCase {

    private MoveToQueueOnBusyBehaviourStepNode moveOp;
    private TestCallsQueueNode moveTo;
    private ExecutorServiceWrapperNode executorService;

    @Before 
    public void prepare() {
        moveTo = new TestCallsQueueNode();
        moveTo.setName("move to queue");
        tree.getRootNode().addAndSaveChildren(moveTo);
        assertTrue(moveTo.start());

        executorService = new ExecutorServiceWrapperNode();
        executorService.setName("executor");
        tree.getRootNode().addAndSaveChildren(executorService);
        assertTrue(executorService.start());

        moveOp = new MoveToQueueOnBusyBehaviourStepNode();
        moveOp.setName("move");
        tree.getRootNode().addAndSaveChildren(moveOp);
        moveOp.setCallsQueue(moveTo);
        moveOp.setExecutor(executorService);
        assertTrue(moveOp.start());
    }

    @Test
    public void test(){
        ExecutorService executor = createMock(ExecutorService.class);
        CallsQueue sourceQueue = createMock(CallsQueue.class);
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        expect(executor.executeQuietly(executeTask())).andReturn(true);
        expect(request.logMess("Moving to the queue (%s)", "move to queue")).andReturn("status");
        replay(executor, sourceQueue, request);

        executorService.setWrapper(executor);
        BehaviourResult res = moveOp.handleBehaviour(sourceQueue, request);
        assertFalse(res.isLeaveInQueue());
        assertEquals(BehaviourResult.StepPolicy.GOTO_NEXT_STEP, res.getNextStepPolicy());
        assertSame(request, moveTo.lastRequest);
        verify(executor, sourceQueue, request);
    }

    @Test
    public void test2(){
        ExecutorService executor = createMock(ExecutorService.class);
        CallsQueue sourceQueue = createMock(CallsQueue.class);
        CallQueueRequestWrapper request = createMock(CallQueueRequestWrapper.class);
        expect(executor.executeQuietly(executeTask())).andReturn(true);
        request.setOnBusyBehaviour(null);
        expect(request.logMess("Moving to the queue (%s)", "move to queue")).andReturn("status");
        replay(executor, sourceQueue, request);

        moveOp.setResetOnBusyBehaviour(true);
        executorService.setWrapper(executor);
        BehaviourResult res = moveOp.handleBehaviour(sourceQueue, request);
        assertFalse(res.isLeaveInQueue());
        assertEquals(BehaviourResult.StepPolicy.GOTO_NEXT_STEP, res.getNextStepPolicy());
        assertSame(request, moveTo.lastRequest);
        verify(executor, sourceQueue, request);
    }

    public static Task executeTask() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Task task = (Task) argument;
                task.run();
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}