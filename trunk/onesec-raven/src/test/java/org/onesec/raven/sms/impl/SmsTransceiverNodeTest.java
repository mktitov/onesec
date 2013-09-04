/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.sms.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;

/**
 *
 * @author Mikhail Titov
 */
public class SmsTransceiverNodeTest extends OnesecRavenTestCase {
    private ExecutorServiceNode executor;
    private SmsRecordSchemaNode schema;
    private PushDataSource ds;
    private SmsTransceiverNode sender;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(100);
        assertTrue(executor.start());
        
        schema = new SmsRecordSchemaNode();
        schema.setName("sms schema");
        testsNode.addAndSaveChildren(schema);
        assertTrue(schema.start());
        
        ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        sender = new SmsTransceiverNode();
        sender.setName("SMS transceiver");
        testsNode.addAndSaveChildren(sender);
        sender.setDataSource(ds);
        sender.setExecutor(executor);
        sender.setBindAddr(privateProperties.getProperty("smsc_address"));
        sender.setBindPort(Integer.parseInt(privateProperties.getProperty("smsc_port")));
        sender.setBindTimeout(9000);
        sender.setReceiveTimeout(100);
        sender.setSystemId(privateProperties.getProperty("sms_system_id"));
        sender.setPassword(privateProperties.getProperty("sms_passwd"));
        sender.setAddrRange(privateProperties.getProperty("sms_addr_range"));
        sender.setFromAddr(privateProperties.getProperty("sms_from_addr"));
        sender.setLogLevel(LogLevel.TRACE);
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(sender);
        assertTrue(collector.start());
    }
    
//    @Test
    public void startTest() throws Exception {
        assertTrue(sender.start());
        Thread.sleep(1000);
        sender.stop();
    }
    
//    @Test
    public void test() throws Exception {
        assertTrue(sender.start());
        Record smsRec = createSmsRecord(1);
        ds.pushData(smsRec);
        assertTrue(collector.waitForData(5000));
        assertSame(smsRec, collector.getLastData());
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, smsRec.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.SEND_TIME));
        Thread.sleep(1000);
    }
    
//    @Test
    public void queueFullTestWithoutTimeout() throws Exception {
        sender.setMaxMessagesInQueue(1);
        sender.setMessageQueueWaitTimeout(0l);
        assertTrue(sender.start());
        Record rec1 = createSmsRecord(1);
        Record rec2 = createSmsRecord(2);
        ds.pushData(rec1);
        ds.pushData(rec2);
        collector.waitForData(5000, 2);
        assertSame(rec2, collector.getDataList().get(0));
        assertSame(rec1, collector.getDataList().get(1));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, rec1.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertEquals(SmsRecordSchemaNode.QUEUE_FULL_STATUS, rec2.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
    }
    
//    @Test
    public void queueFullTestWithTimeout() throws Exception {
        sender.setMaxMessagesInQueue(1);
        sender.setMessageQueueWaitTimeout(1000l);
        assertTrue(sender.start());
        Record rec1 = createSmsRecord(1);
        Record rec2 = createSmsRecord(2);
        ds.pushData(rec1);
        ds.pushData(rec2);
        collector.waitForData(5000, 2);
        assertSame(rec1, collector.getDataList().get(0));
        assertSame(rec2, collector.getDataList().get(1));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, rec1.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, rec2.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
    }
    
    //Must see in th log: "Too many unconfirmed messages. Wating..."
    @Test
    public void tooManyUnconfirmedTest() throws Exception {
        sender.setOnceSend(5);
        sender.setMaxUnconfirmed(1);
        assertTrue(sender.start());
        Record rec1 = createSmsRecord(1);
        Record rec2 = createSmsRecord(2);
        ds.pushData(rec1);
        ds.pushData(rec2);
        assertTrue(collector.waitForData(10000, 2));
        assertSame(rec1, collector.getDataList().get(0));
        assertSame(rec2, collector.getDataList().get(1));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, rec1.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, rec2.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
    }
    
    private Record createSmsRecord(long id) throws RecordException {
        Record rec = schema.createRecord();
        rec.setValue(SmsRecordSchemaNode.ID, id);
        rec.setValue(SmsRecordSchemaNode.ADDRESS, privateProperties.getProperty("sms_dst_addr"));
        rec.setValue(SmsRecordSchemaNode.MESSAGE, "Проверка работы SmsTransceiver'а. id="+id+". ts="+
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
        return rec;
    }
}