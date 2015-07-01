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

import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.impl.IvrEndpointPoolNode;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.LazyCallQueueRequest;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;

/**
 *
 * @author Mikhail Titov
 */
public class GenerateLazyCallQueueRequestNodeTest extends OnesecRavenTestCase {
    
    @Test
    public void test() throws Exception {
        IvrEndpointPoolNode pool = new IvrEndpointPoolNode();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        
        IvrConversationScenarioNode scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        GenerateLazyCallQueueRequestNode gen = new GenerateLazyCallQueueRequestNode();
        gen.setName("request generator");
        tree.getRootNode().addAndSaveChildren(gen);
        gen.setDataSource(ds);
        gen.setEndpointPool(pool);
        gen.setConversationScenario(scenario);
        gen.setQueueId("test");
        gen.setPriority(10);
        gen.getAttr("abonentNumber").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        gen.setAbonentNumber("data");
        assertTrue(gen.start());
        
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(gen);
        assertTrue(collector.start());
        
        ds.pushData("1234");
        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof AbonentCommutationManager);
        assertTrue(data instanceof LazyCallQueueRequest);
        LazyCallQueueRequest req = (LazyCallQueueRequest) data;
        assertEquals("1234", req.getAbonentNumber());
        assertEquals("test", req.getQueueId());
        assertEquals(10, req.getPriority());
    }
    
}
