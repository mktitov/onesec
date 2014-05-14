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
import org.onesec.raven.sms.BindMode;
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
    private DataCollector collector2;
    
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
        
        SmsDeliveryReceiptRecordSchemaNode deliverSchema = new SmsDeliveryReceiptRecordSchemaNode();
        deliverSchema.setName("deliver schema");
        testsNode.addAndSaveChildren(deliverSchema);
        assertTrue(deliverSchema.start());
        
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
        sender.setBindMode(BindMode.TRANSMITTER);
        sender.setReceiveTimeout(100);
        sender.setSystemId(privateProperties.getProperty("sms_system_id"));
        sender.setPassword(privateProperties.getProperty("sms_passwd"));
        sender.setAddrRange(privateProperties.getProperty("sms_addr_range"));
        sender.setFromAddr(privateProperties.getProperty("sms_from_addr"));
        sender.setSystemType(privateProperties.getProperty("sms_systemType"));
        sender.setLogLevel(LogLevel.TRACE);
        sender.getDeliveryReceiptChannel().setDeliveryReceiptSchema(deliverSchema);
        sender.getDeliveryReceiptChannel().setLogLevel(LogLevel.TRACE);
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(sender);
        collector.setLogLevel(LogLevel.TRACE);
        assertTrue(collector.start());
        
        collector2 = new DataCollector();
        collector2.setName("delivery report collector");
        testsNode.addAndSaveChildren(collector2);
        collector2.setDataSource(sender.getDeliveryReceiptChannel());
        collector2.setLogLevel(LogLevel.TRACE);
        assertTrue(collector2.start());
    }
    
//    @Test
    public void startTest() throws Exception {
        assertTrue(sender.start());
        assertNotNull(sender.getAttr("bind").getValue());
        Thread.sleep(1000);
        sender.stop();
    }
    
    @Test
    public void submitTest() throws Exception {
        assertTrue(sender.start());
        Record smsRec = createSmsRecord(1);
        ds.pushData(smsRec);
        assertTrue(collector.waitForData(5000));
        assertSame(smsRec, collector.getDataList().get(0));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, smsRec.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.SEND_TIME));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.MESSAGE_ID));
    }
    
//    @Test
    public void messageExpireTest() throws Exception {
        sender.setBindMode(BindMode.RECEIVER_AND_TRANSMITTER);
        assertTrue(sender.start());
        Record smsRec = createSmsRecord(1);
        smsRec.setValue(SmsRecordSchemaNode.NEED_DELIVERY_RECEIPT, true);
        smsRec.setValue(SmsRecordSchemaNode.MESSAGE_EXPIRE_PERIOD, "00:00:01");
        ds.pushData(smsRec);
        assertTrue(collector.waitForData(5000));
        assertSame(smsRec, collector.getDataList().get(0));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, smsRec.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.SEND_TIME));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.MESSAGE_ID));
        //
        assertTrue(collector2.waitForData(65000));
        assertEquals(1, collector2.getDataListSize());
        assertTrue(collector2.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector2.getDataList().get(0);
        assertEquals(rec.getValue(SmsDeliveryReceiptRecordSchemaNode.MESSAGE_ID), smsRec.getValue(SmsRecordSchemaNode.MESSAGE_ID));
        System.out.println("!!! DELIVERY REPORT: "+rec);
//        Thread.sleep(65000);
    }
    
//    @Test
    public void deliveryReceiptTest() throws Exception {
        sender.setBindMode(BindMode.RECEIVER_AND_TRANSMITTER);
        assertTrue(sender.start());
        Record smsRec = createSmsRecord(1);
        smsRec.setValue(SmsRecordSchemaNode.NEED_DELIVERY_RECEIPT, true);
        ds.pushData(smsRec);
        assertTrue(collector.waitForData(5000));
        assertSame(smsRec, collector.getDataList().get(0));
        assertEquals(SmsRecordSchemaNode.SUCCESSFUL_STATUS, smsRec.getValue(SmsRecordSchemaNode.COMPLETION_CODE));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.SEND_TIME));
        assertNotNull(smsRec.getValue(SmsRecordSchemaNode.MESSAGE_ID));
        System.out.println("\n!!! RECORD: \n"+smsRec);
//        Thread.sleep(30000);
        assertTrue(collector2.waitForData(10000));
        assertEquals(1, collector2.getDataListSize());
        assertTrue(collector2.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector2.getDataList().get(0);
        assertEquals(rec.getValue(SmsDeliveryReceiptRecordSchemaNode.MESSAGE_ID), smsRec.getValue(SmsRecordSchemaNode.MESSAGE_ID));
//        Thread.sleep(30000);
    }
    
//    @Test
    public void enquiryLinkTest() throws Exception {
        sender.setEnquireInterval(10000l);
        sender.setMaxEnquireAttempts(10);
        sender.setRebindOnTimeoutInterval(10000);
        assertTrue(sender.start());
        Thread.sleep(120000);
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
//    @Test
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
        String mess = "Test SmsTransceiver'à. id="+id+". ts="+
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
//        rec.setValue(SmsRecordSchemaNode.MESSAGE, mess + mess + mess + mess);
        rec.setValue(SmsRecordSchemaNode.MESSAGE, mess);
        return rec;
    }
}