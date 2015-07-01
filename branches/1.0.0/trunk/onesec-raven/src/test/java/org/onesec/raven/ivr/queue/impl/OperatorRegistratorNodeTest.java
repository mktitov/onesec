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
package org.onesec.raven.ivr.queue.impl;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.PushOnDemandDataSource;

/**
 *
 * @author Mikhail Titov
 */
public class OperatorRegistratorNodeTest extends OnesecRavenTestCase {
    private CallsQueuesNode queues;
    private PushOnDemandDataSource ds;
    private OperatorRegistratorNode registrator;
    private CallsQueueOperatorNode oper;
    
    @Before
    public void prepare() {
        ds = new PushOnDemandDataSource();
        ds.setName("auth ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        queues = new CallsQueuesNode();
        queues.setName("queues");
        tree.getRootNode().addAndSaveChildren(queues);
        assertTrue(queues.start());
        registrator = queues.getOperatorRegistrator();
        registrator.setDataSource(ds);
        assertTrue(registrator.start());
        
        TestEndpointPool pool = new TestEndpointPool();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        assertTrue(pool.start());

        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());
        
        TestConversationsBridgeManager bridgeManager = new TestConversationsBridgeManager();
        bridgeManager.setName("conversation bridge");
        tree.getRootNode().addAndSaveChildren(bridgeManager);
        assertTrue(bridgeManager.start());
        
        oper = new CallsQueueOperatorNode();
        oper.setName("oper");
        queues.getOperatorsNode().addAndSaveChildren(oper);
        oper.setPhoneNumbers("000");        
        oper.setEndpointPool(pool);
        oper.setExecutor(executor);
        oper.setConversationsBridgeManager(bridgeManager);
        assertTrue(oper.start());
    }
    
    @Test
    public void currentOperatorTest() {
        assertNull(registrator.getCurrentOperator("000"));
        assertNull(registrator.getCurrentOperator("0"));
        
        oper.setPersonDesc("Operator");
        oper.setPersonId("operator id");
        OperatorDesc desc = registrator.getCurrentOperator("000");
        assertNotNull(desc);
        assertEquals("Operator", desc.getDesc());
        assertEquals("operator id", desc.getId());
        
        registrator.stop();
        assertNull(registrator.getCurrentOperator("000"));
    }

    @Test
    public void successAuthTest() {
        HashMap map = new HashMap();
        map.put(OperatorRegistratorNode.OPERATOR_DESC_FIELD, "Pupkin");
        ds.addDataPortion(map);
        ds.addDataPortion(null);
        
        OperatorDesc desc = registrator.register("000", "123");
        assertNotNull(desc);
        assertEquals("Pupkin", desc.getDesc());
        assertEquals("123", desc.getId());

        assertEquals("Pupkin", oper.getOperatorDesc());
        assertEquals("123", oper.getOperatorId());
    }
    
    @Test
    public void unbindOperatorTest() {
        successAuthTest();
        registrator.unregister("000");
        assertNull(oper.getOperatorDesc());
        assertNull(oper.getOperatorId());
    }
    
    @Test
    public void failAuthTest_invalidOperator() {
        HashMap map = new HashMap();
        map.put(OperatorRegistratorNode.OPERATOR_DESC_FIELD, "Pupkin");
        ds.addDataPortion(map);
        ds.addDataPortion(null);
        
        assertNull(registrator.register("0000", "123"));
    }
    
    @Test 
    public void failAuthTest() {
        assertNull(registrator.register("000", "123"));
        
        ds.addDataPortion(null);
        assertNull(registrator.register("000", "123"));
    }
}
