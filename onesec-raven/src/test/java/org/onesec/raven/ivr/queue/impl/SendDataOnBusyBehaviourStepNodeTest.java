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
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.raven.ds.DataContext;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class SendDataOnBusyBehaviourStepNodeTest extends OnesecRavenTestCase
{
    private SendDataOnBusyBehaviourStepNode sendData;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        sendData = new SendDataOnBusyBehaviourStepNode();
        sendData.setName("send data");
        tree.getRootNode().addAndSaveChildren(sendData);
        assertTrue(sendData.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(sendData);
        assertTrue(collector.start());
    }

    @Test
    public void sendDataTest()
    {
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        DataContext dataContext = createMock(DataContext.class);
        DataHandler dataHandler = createMock(DataHandler.class);

        expect(req.getContext()).andReturn(dataContext);
        dataHandler.handleData(req, dataContext);

        replay(req, dataContext, dataHandler);

        collector.setDataHandler(dataHandler);

        BehaviourResult res = sendData.handleBehaviour(null, req);
        
        assertTrue(res.isLeaveInQueue());
        assertEquals(BehaviourResult.StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP, res.getNextStepPolicy());        

        verify(req, dataContext, dataHandler);
    }

    @Test
    public void expressionTest()
    {
        CallQueueRequestWrapper req = createMock(CallQueueRequestWrapper.class);
        DataContext dataContext = createMock(DataContext.class);
        DataHandler dataHandler = createMock(DataHandler.class);

        expect(req.getContext()).andReturn(dataContext).times(2);
        expect(req.getRequestId()).andReturn(1l);
        expect(dataContext.getAt("test")).andReturn("hello");
        dataHandler.handleData("1hello", dataContext);

        replay(req, dataContext, dataHandler);

        collector.setDataHandler(dataHandler);
        sendData.setUseExpression(true);
        sendData.setExpression("''+request.requestId+context['test']");

        BehaviourResult res = sendData.handleBehaviour(null, req);

        assertTrue(res.isLeaveInQueue());
        assertEquals(BehaviourResult.StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP, res.getNextStepPolicy());

        verify(req, dataContext, dataHandler);
    }
}