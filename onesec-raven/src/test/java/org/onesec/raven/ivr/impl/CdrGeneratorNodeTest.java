/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import java.util.Set;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrTerminal;
import org.raven.ds.Record;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class CdrGeneratorNodeTest extends OnesecRavenTestCase {
    
    private ExecutorServiceNode executor;
    private CdrGeneratorNode cdrGenerator;
    private CallCdrRecordSchemaNode cdrSchema;
    private DataCollector receiver;
    
    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder); 
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
    }
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(30);
        assertTrue(executor.start());
        
        cdrSchema = new CallCdrRecordSchemaNode();
        cdrSchema.setName("call cdr");
        testsNode.addAndSaveChildren(cdrSchema);
        assertTrue(cdrSchema.start());
        
        cdrGenerator = new CdrGeneratorNode();
        cdrGenerator.setName("cdr generator");
        testsNode.addAndSaveChildren(cdrGenerator);
        cdrGenerator.setCdrSchema(cdrSchema);
        cdrGenerator.setExecutor(executor);
        
        
        receiver = new DataCollector();
        receiver.setName("cdr receiver");
        testsNode.addAndSaveChildren(receiver);
        receiver.setDataSource(cdrGenerator);
        assertTrue(receiver.start());
        
    }
    
    @Test
    public void sendCdrTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final IvrTerminal term) 
        throws Exception 
    {
//        pool.setCdrSchema(cdrSchema);
        cdrGenerator.setCdrSendEvents(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED.name());
        assertTrue(cdrGenerator.start());
        
//        new Expectations(){{
//            cdrSchema.createRecord(); result = cdr2;
//            conv2.getConversationId(); result ="conv_2";
//            conv2.getCallStartTime(); result = System.currentTimeMillis();
//        }};
        
        
        cdrGenerator.registerCallEvent(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED, conv, term));
        Thread.sleep(100); //подождем пока cdr сформируется и отправиться
        assertEquals(1, receiver.getDataListSize());
        assertTrue(receiver.getDataList().get(0) instanceof Record);    
    }
}
