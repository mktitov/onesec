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

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.queue.BehaviourResult;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueOnBusyBehaviourNodeTest extends OnesecRavenTestCase
{
    private CallsQueueOnBusyBehaviourNode behaviourNode;
    
    @Before
    public void prepare()
    {
        behaviourNode = new CallsQueueOnBusyBehaviourNode();
        behaviourNode.setName("on busy");
        tree.getRootNode().addAndSaveChildren(behaviourNode);
        assertTrue(behaviourNode.start());
    }
    
    @Test
    public void noStepsTest()
    {
        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(1);
        request.addToLog("reached the end of the \"on busy behaviour steps\" sequence");
        request.fireRejectedQueueEvent();
        
        replay(queue, request);
        
        behaviourNode.handleBehaviour(queue, request);
        
        verify(queue, request);
    }
    
    @Test
    public void leaveInQueueTest()
    {
        addTestStep(new BehaviourResultImpl(true, BehaviourResult.StepPolicy.GOTO_NEXT_STEP));
        
        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(1);
        
        replay(queue, request);
        
        assertTrue(behaviourNode.handleBehaviour(queue, request));
        
        verify(queue, request);
    }
    
    @Test
    public void removeFromQueueTest()
    {
        addTestStep(new BehaviourResultImpl(false, BehaviourResult.StepPolicy.GOTO_NEXT_STEP));
        
        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(1);
        
        replay(queue, request);
        
        assertFalse(behaviourNode.handleBehaviour(queue, request));
        
        verify(queue, request);
    }

    @Test
    public void leaveAtThisStep()
    {
        addTestStep(new BehaviourResultImpl(true, BehaviourResult.StepPolicy.LEAVE_AT_THIS_STEP));

        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(0);

        replay(queue, request);

        assertTrue(behaviourNode.handleBehaviour(queue, request));

        verify(queue, request);
    }
    
    @Test
    public void immediatelyExecuteNextStep()
    {
        addTestStep(new BehaviourResultImpl(true, BehaviourResult.StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP));

        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createStrictMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(1);
        expect(request.getOnBusyBehaviourStep()).andReturn(1);
        request.addToLog(CallsQueueOnBusyBehaviourNode.REACHED_THE_END_OF_SEQ);
        request.fireRejectedQueueEvent();
        request.setOnBusyBehaviourStep(2);

        replay(queue, request);

        assertFalse(behaviourNode.handleBehaviour(queue, request));

        verify(queue, request);
    }

    @Test
    public void replaceByTest()
    {
        CallsQueueOnBusyBehaviourNode b2 = new CallsQueueOnBusyBehaviourNode();
        b2.setName("another behaviour");
        tree.getRootNode().addAndSaveChildren(b2);
        assertTrue(b2.start());

        behaviourNode.setReplaceBy(b2);

        addTestStep(b2, new BehaviourResultImpl(true, BehaviourResult.StepPolicy.LEAVE_AT_THIS_STEP));

        CallsQueue queue = createMock(CallsQueue.class);
        CallQueueRequestController request = createMock(CallQueueRequestController.class);
        expect(request.getOnBusyBehaviourStep()).andReturn(0);
        request.setOnBusyBehaviourStep(0);

        replay(queue, request);

        assertTrue(behaviourNode.handleBehaviour(queue, request));

        verify(queue, request);
    }

    private TestOnBusyBehaviourStep addTestStep(BehaviourResult behaviourResult)
    {
        return addTestStep(behaviourNode, behaviourResult);
    }
    
    private TestOnBusyBehaviourStep addTestStep(Node owner, BehaviourResult behaviourResult)
    {
        TestOnBusyBehaviourStep step = new TestOnBusyBehaviourStep();
        step.setName("step");
        owner.addAndSaveChildren(step);
        step.setBehaviourResult(behaviourResult);
        assertTrue(step.start());

        return step;
    }
}