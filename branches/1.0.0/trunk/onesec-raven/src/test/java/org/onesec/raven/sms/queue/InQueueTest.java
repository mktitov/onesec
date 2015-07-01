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
package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.DataSM;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.util.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.sms.SmsConfig;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.onesec.raven.sms.impl.IncomingSmsRecordSchemaNode;
import static org.onesec.raven.sms.impl.IncomingSmsRecordSchemaNode.*;
import org.onesec.raven.sms.impl.SmsIncomingMessageChannel;
import org.onesec.raven.sms.impl.SmsMessageEncoderImpl;
import org.onesec.raven.sms.sm.udh.UDHData;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.sched.ExecutorServiceException;
import org.raven.test.DataCollector;
import org.raven.tree.impl.LoggerHelper;
/**
 *
 * @author Mikhail Titov
 */
public class InQueueTest extends OnesecRavenTestCase {
    private LoggerHelper logger;
    private IMocksControl mocks;
    private SmsIncomingMessageChannel channel;
    private DataCollector collector;
    private InQueue queue;
    private DataCollector channelParent;
    
    static {
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
    }
    
    @Before
    public void prepapre() {
        logger = new LoggerHelper(testsNode, "");
        
        IncomingSmsRecordSchemaNode schema = new IncomingSmsRecordSchemaNode();
        schema.setName("Incoming SMS schema");
        testsNode.addAndSaveChildren(schema);
        assertTrue(schema.start());
        
        channelParent = new DataCollector();
        channelParent.setName("channel parent");
        testsNode.addAndSaveChildren(channelParent);
//        assertTrue(channelParent.start());
        
        channel = new SmsIncomingMessageChannel();
        channel.setName("channel");
        channelParent.addAndSaveChildren(channel);
        channel.setIncomingShortMessageSchema(schema);
        assertTrue(channel.start());
        
        collector = new DataCollector();
        collector.setName("message collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(channel);
        assertTrue(collector.start());
        
        queue = new InQueue("utf-8", channel, 50);
//        queue.setLogger(logger);
    }
    
    @Test
    public void normalSmsTest() throws Exception {
        mocks = createControl();
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
//        SmsConfig smsConfig = createSmsConfig((byte)0);
        mocks.replay();
        
        DeliverSM pdu = new DeliverSM();
        prepareDeliverSM(pdu);
        pdu.setShortMessage("TEST");
        
        queue.init(facade, ctx);
        queue.processData(pdu);
        assertEquals(1, collector.getDataListSize());
        checkRecord(collector.getDataList().get(0), "TEST", null, 0, (byte)0, (byte)0);
        mocks.verify();
    }
    
    @Test
    public void udhSmsTest() throws Exception {
        mocks = createControl();
        SmsConfig smsConfig = createSmsConfig((byte)0);
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
        mocks.replay();
        
        queue.init(facade, ctx);
        SmsMessageEncoderImpl encoder = createMessageEncoder(smsConfig);        
        String message = StringUtils.repeat("Test", 50);
        ByteBuffer buf = encoder.createMessageBuffer(message, smsConfig.getDataCoding());
        UDHData udhd = new UDHData();
        udhd.setMesData(buf);
        List<ByteBuffer> buffers = udhd.getAllData(false);
        assertEquals(2, buffers.size());
        
        for (int i=0; i<buffers.size(); ++i) {
            DeliverSM pdu = new DeliverSM();
            prepareDeliverSM(pdu);
            pdu.setMessagePayload(buffers.get(i));
            pdu.setEsmClass((byte)0x40);
            queue.processData(pdu);
        }
        assertEquals(1, collector.getDataListSize());
        checkRecord(collector.getDataList().get(0), message, 0, 2, (byte)0, (byte)0x40);
        
        mocks.verify();
    }
    
    @Test
    public void udhDataSmSmsTest() throws Exception {
        mocks = createControl();
        SmsConfig smsConfig = createSmsConfig((byte)0);
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
        mocks.replay();
        
        queue.init(facade, ctx);
        SmsMessageEncoderImpl encoder = createMessageEncoder(smsConfig);        
        String message = StringUtils.repeat("Test", 50);
        ByteBuffer buf = encoder.createMessageBuffer(message, smsConfig.getDataCoding());
        UDHData udhd = new UDHData();
        udhd.setMesData(buf);
        List<ByteBuffer> buffers = udhd.getAllData(false);
        assertEquals(2, buffers.size());
        
        for (int i=0; i<buffers.size(); ++i) {
            DataSM pdu = new DataSM();
            prepareDataSM(pdu);
            pdu.setMessagePayload(buffers.get(i));
            pdu.setEsmClass((byte)0x40);
            queue.processData(pdu);
        }
        assertEquals(1, collector.getDataListSize());
        checkRecord(collector.getDataList().get(0), message, 0, 2, (byte)0, (byte)0x40);
        
        mocks.verify();
    }
    
    @Test
    public void sarSmsTest() throws Exception {
        mocks = createControl();
        SmsConfig smsConfig = createSmsConfig((byte)0);
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
        mocks.replay();
        SmsMessageEncoderImpl encoder = createMessageEncoder(smsConfig);
        List<String> bufs = Arrays.asList("Test1", "Test2");
        assertEquals(2, bufs.size());
        
        queue.init(facade, ctx);
        for (int i=0; i<bufs.size(); ++i) {
            DeliverSM pdu = new DeliverSM();
            prepareDeliverSM(pdu);
            pdu.setSarMsgRefNum((short)1);
            pdu.setSarSegmentSeqnum((short)i);
            pdu.setSarTotalSegments((short)2);
            pdu.setShortMessage(bufs.get(i));
            queue.processData(pdu);
        }
        assertEquals(1, collector.getDataListSize());
        checkRecord(collector.getDataList().get(0), "Test1Test2", 1, 2, (byte)0, (byte)0);
        
    }
    
    @Test
    public void sarDataSmSmsTest() throws Exception {
        mocks = createControl();
        SmsConfig smsConfig = createSmsConfig((byte)0);
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
        mocks.replay();
        SmsMessageEncoderImpl encoder = createMessageEncoder(smsConfig);
        
        String message = StringUtils.repeat("Test", 50);
        ByteBuffer buf = encoder.createMessageBuffer(message, smsConfig.getDataCoding());
        List<ByteBuffer> bufs = encoder.getFragments(buf);
        
        queue.init(facade, ctx);
        for (int i=0; i<bufs.size(); ++i) {
            DataSM pdu = new DataSM();
            prepareDataSM(pdu);
            pdu.setSarMsgRefNum((short)1);
            pdu.setSarSegmentSeqnum((short)i);
            pdu.setSarTotalSegments((short)2);
            pdu.setMessagePayload(bufs.get(i));
            queue.processData(pdu);
        }
        assertEquals(1, collector.getDataListSize());
        checkRecord(collector.getDataList().get(0), message, 1, 2, (byte)0, (byte)0);
        
    }
    
    @Test
    public void longMessageTimeoutTest() throws Exception {
        mocks = createControl();
        DataProcessorContext ctx = createDataProcessorContext();
        DataProcessorFacade facade = createDataProcessorFacade();
        mocks.replay();
        queue.init(facade, ctx);
        mocks.verify();
        
        List<String> bufs = Arrays.asList("Test1", "Test2");
        assertEquals(2, bufs.size());
        List<DeliverSM> pdus = new ArrayList<DeliverSM>();
        for (int i=0; i<bufs.size(); ++i) {
            DeliverSM pdu = new DeliverSM();
            prepareDeliverSM(pdu);
            pdu.setSarMsgRefNum((short)1);
            pdu.setSarSegmentSeqnum((short)i);
            pdu.setSarTotalSegments((short)2);
            pdu.setShortMessage(bufs.get(i));
            pdus.add(pdu);
        }
        queue.processData(pdus.get(0));
        Thread.sleep(40);
        queue.processData(InQueue.CHECK_MESSAGE_RECEIVE_TIMEOUT);
        queue.processData(pdus.get(1));
        assertEquals(1, collector.getDataListSize());
        
        collector.getDataList().clear();
        queue.processData(pdus.get(0));
        Thread.sleep(55);
        queue.processData(InQueue.CHECK_MESSAGE_RECEIVE_TIMEOUT);
        queue.processData(pdus.get(1));
        assertEquals(0, collector.getDataListSize());
    }
    
    private void checkRecord(Object objRec, String message, Integer messageId, int segCount, byte dataCoding, byte esmClass) 
            throws RecordException 
    {
        assertTrue(objRec instanceof Record);
        Record rec = (Record) objRec;
        assertEquals(message, rec.getValue(MESSAGE));
        assertEquals("1234", rec.getValue(DST_ADDRESS));
        assertEquals((byte)1, rec.getValue(DST_TON));
        assertEquals((byte)2, rec.getValue(DST_NPI));
        assertEquals("4321", rec.getValue(SRC_ADDRESS));
        assertEquals((byte)2, rec.getValue(SRC_TON));
        assertEquals((byte)1, rec.getValue(SRC_NPI));
        assertEquals(messageId, rec.getValue(MESSAGE_ID));
        assertEquals(segCount, rec.getValue(MESSAGE_SEG_COUNT));        
        assertEquals(dataCoding, rec.getValue(DATA_CODING));
        assertEquals(esmClass, rec.getValue(ESM_CLASS));
    }
    
    private void prepareDeliverSM(DeliverSM pdu) throws Exception {
        pdu.setDestAddr(new Address((byte)1, (byte)2, "1234"));
        pdu.setSourceAddr(new Address((byte)2, (byte)1, "4321"));
        pdu.setEsmClass((byte)0x00);
        pdu.setProtocolId((byte)0);
        pdu.setPriorityFlag((byte)0);
        pdu.setDataCoding((byte)0);
    }
    
    private void prepareDataSM(DataSM pdu) throws Exception {
        pdu.setDestAddr(new Address((byte)1, (byte)2, "1234"));
        pdu.setSourceAddr(new Address((byte)2, (byte)1, "4321"));
        pdu.setEsmClass((byte)0x00);
        pdu.setDataCoding((byte)0);
    }
    
    private SmsConfig createSmsConfig(byte dataCoding) {
        SmsConfig config = mocks.createMock(SmsConfig.class);
        expect(config.getDataCoding()).andReturn(dataCoding).anyTimes();
        expect(config.isUse7bit()).andReturn(false).anyTimes();
        expect(config.getMessageCP()).andReturn("utf-8").anyTimes();
        return config;
    }
    
    private SmsMessageEncoderImpl createMessageEncoder(SmsConfig config) {
        return new SmsMessageEncoderImpl(config, logger);
    }
    
    private DataProcessorContext createDataProcessorContext() {
        DataProcessorContext ctx = mocks.createMock(DataProcessorContext.class);
        expect(ctx.getLogger()).andReturn(logger).anyTimes();
        return ctx;
    }
    
    private DataProcessorFacade createDataProcessorFacade() throws ExecutorServiceException {
        DataProcessorFacade facade = mocks.createMock(DataProcessorFacade.class);
        facade.sendRepeatedly(50l, 50l, 0, InQueue.CHECK_MESSAGE_RECEIVE_TIMEOUT);
        return facade;
    }
}
