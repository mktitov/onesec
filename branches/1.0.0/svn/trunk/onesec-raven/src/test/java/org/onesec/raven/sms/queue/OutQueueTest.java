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
package org.onesec.raven.sms.queue;

import java.util.concurrent.TimeUnit;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.sms.MessageUnit;
import org.onesec.raven.sms.ShortTextMessage;
import org.onesec.raven.sms.SmsConfig;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
import org.junit.runner.RunWith;
import org.onesec.raven.sms.MessageUnitStatus;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class OutQueueTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger(OutQueueTest.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "SMS. ", null, logger);
    
    //mocks
    private IMocksControl mocks;
    private SmsConfig config;
    private ShortTextMessage mess1;
    private ShortTextMessage mess2;
    private MessageUnit unit1;
    private MessageUnit unit2;

    @Before
    public void prepare() {
        mocks = createControl();
    }
    
    @After
    public void shutdown() {
        if (mocks!=null)
            mocks.verify();
    }
    
//    @Test
    public void addMessageTest() {
        createMocksForQueueIsFullTest();
        OutQueue queue = new OutQueue(config, loggerHelper);
        assertTrue(queue.addMessage(mess1));
        assertFalse(queue.addMessage(mess2));
    }
    
    @Test
    public void getNextTest() {
        createMocksForGetNextTest();
        OutQueue queue = new OutQueue(config, loggerHelper);
        assertTrue(queue.addMessage(mess1));
        assertSame(unit1, queue.getNext());
    }
    
    @Test
    public void unitSubmitTest() {
        createMocksForSubmitTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit1, MessageUnitStatus.READY, MessageUnitStatus.SUBMITTED);
        assertEquals(1, queue.howManyUnconfirmed());
        assertNull(queue.getNext());
        assertSame(unit1, queue.getMessageUnit(1));
        assertFalse(queue.isEmpty());
        
        queue.statusChanged(unit1, MessageUnitStatus.SUBMITTED, MessageUnitStatus.CONFIRMED);
        assertFalse(queue.isEmpty());
        assertEquals(0, queue.howManyUnconfirmed());
        assertNull(queue.getNext());
        assertTrue(queue.isEmpty());
    }
    
    @Test
    public void queueFullOnTest(
            @Mocked final ShortTextMessage mess1,
            @Mocked final ShortTextMessage mess2,
            @Mocked final MessageUnit unit1_1,
            @Mocked final MessageUnit unit1_2,
            @Mocked final MessageUnit unit2_1,
            @Mocked final SmsConfig config
    ) throws Exception {
        mocks = null;
        final OutQueue queue = new OutQueue(config, loggerHelper);
        new Expectations() {{
            config.getMaxUnconfirmed(); result = 1;
            config.getMaxMessagesInQueue(); result = 10;
            mess1.getUnits(); result = new MessageUnit[]{unit1_1, unit1_2};
            mess2.getUnits(); result = new MessageUnit[]{unit2_1};
//            unit1_1.toString(); result = "unit1_1";
            unit1_1.addListener((OutQueue)any); result = unit1_1; times = 1;
            unit1_2.addListener((OutQueue)any); result = unit1_2; times = 1;
            unit2_1.addListener((OutQueue)any); result = unit2_1; times = 1;
            unit1_1.checkStatus(); returns(MessageUnitStatus.READY, MessageUnitStatus.READY, MessageUnitStatus.CONFIRMED);
            unit1_2.checkStatus(); result = MessageUnitStatus.READY;
            unit2_1.checkStatus(); returns(MessageUnitStatus.READY, MessageUnitStatus.CONFIRMED);
            unit1_1.getSequenceNumber(); result = 1;
            unit1_2.getSequenceNumber(); result = 2;
            unit2_1.getSequenceNumber(); result = 3;
            unit1_1.getMessage(); result = mess1;
            unit1_2.getMessage(); result = mess1;
            unit2_1.getMessage(); result = mess2;
            unit1_1.getDst(); result = "123";
            unit1_2.getDst(); result = "123";
            unit2_1.getDst(); result = "321";
            
        }};

        queue.addMessage(mess1);
        queue.addMessage(mess2);
        assertEquals(1, queue.getNext().getSequenceNumber());
        queue.statusChanged(unit1_1, MessageUnitStatus.READY, MessageUnitStatus.SUBMITTED);
        queue.queueFullOn(1);
        queue.statusChanged(unit1_1, MessageUnitStatus.SUBMITTED, MessageUnitStatus.TRY_WHEN_READY);
        assertEquals(3, queue.getNext().getSequenceNumber());
        assertEquals(1, queue.getNext().getSequenceNumber());
        assertEquals(2, queue.getNext().getSequenceNumber());
        new Verifications(){{
            
        }};
    }
    
    @Test
    public void unitFatalTest() {
        createMocksForFatalTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit1, MessageUnitStatus.SUBMITTED, MessageUnitStatus.FATAL);
        assertNull(queue.getNext());
        assertTrue(queue.isEmpty());
    }
    
    @Test
    public void unitDelayedTest() throws InterruptedException {
        createMocksForDelayedTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit1, MessageUnitStatus.READY, MessageUnitStatus.DELAYED);
        assertNull(queue.getNext());
        Thread.sleep(100);
        assertSame(unit1, queue.getNext());
    }
    
    @Test
    public void maxMessagesPerTimeUnitTest() throws Exception {
        createMocksForMaxMessagesPerTimeUnit();
        OutQueue queue = createQueueAndAddMessage();
        queue.addMessage(mess2);
        
        assertSame(unit1, queue.getNext());
        assertEquals(0l, queue.getUnitsInPeriod());
        queue.statusChanged(unit1, MessageUnitStatus.READY, MessageUnitStatus.SUBMITTED);
        assertEquals(1l, queue.getUnitsInPeriod());        
        assertNull(queue.getNext());
        Thread.sleep(1001);
        assertSame(unit2, queue.getNext());
        assertEquals(0, queue.getUnitsInPeriod());
    }
    
    private void createMocksForMaxMessagesPerTimeUnit() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        mess2 = mocks.createMock("mess2", ShortTextMessage.class);
        unit1 = mocks.createMock("unit1", MessageUnit.class);
        unit2 = mocks.createMock("unit2", MessageUnit.class);
        
        expect(config.getMaxMessagesInQueue()).andReturn(2).anyTimes();
        expect(config.getMaxMessageUnitsPerTimeUnit()).andReturn(1l).anyTimes();
        expect(config.getMaxMessageUnitsTimeQuantity()).andReturn(1l).anyTimes();
        expect(config.getMaxMessageUnitsTimeUnit()).andReturn(TimeUnit.SECONDS).anyTimes();
        expect(config.getMaxUnconfirmed()).andReturn(10).anyTimes();
        
        //on addMessage()
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit1});
        mess1.addListener(isA(OutQueue.class));
        expect(unit1.addListener(isA(OutQueue.class))).andReturn(unit1);
        
        //on addMessage()
        expect(mess2.getUnits()).andReturn(new MessageUnit[]{unit2});
        mess2.addListener(isA(OutQueue.class));
        expect(unit2.addListener(isA(OutQueue.class))).andReturn(unit2);
        
        //on getNext()
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.READY);
        
        //on unit1.submitted()
        expect(unit1.getSequenceNumber()).andReturn(1);
        
        //on getNext()
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        expect(unit1.getStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        expect(unit2.getDst()).andReturn("123");
        expect(unit2.checkStatus()).andReturn(MessageUnitStatus.READY);
        
        
        mocks.replay();
    }

    private void createMocksForQueueIsFullTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        mess2 = mocks.createMock("mess2", ShortTextMessage.class);
        MessageUnit unit = mocks.createMock(MessageUnit.class);
        
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(2);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit});
        mess1.addListener(isA(OutQueue.class));
        expect(unit.addListener(isA(OutQueue.class))).andReturn(unit);
        mocks.replay();
    }
    
    private void createMocksForGetNextTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit1 = mocks.createMock(MessageUnit.class);
        
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit1});
        mess1.addListener(isA(OutQueue.class));
        expect(unit1.addListener(isA(OutQueue.class))).andReturn(unit1);
        
        //on getNext()
        expect(config.getMaxUnconfirmed()).andReturn(1);
        expect(config.getMaxMessageUnitsPerTimeUnit()).andReturn(0l);
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.READY);
        mocks.replay();
    }
    
    private void createMocksForSubmitTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit1 = mocks.createMock(MessageUnit.class);
        
        expect(config.getMaxMessageUnitsPerTimeUnit()).andReturn(0l).anyTimes();
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit1});
        mess1.addListener(isA(OutQueue.class));
        expect(unit1.addListener(isA(OutQueue.class))).andReturn(unit1);
        
        //on MessageUnit.submitted()
        expect(unit1.getSequenceNumber()).andReturn(1);       
        expect(config.getMaxUnconfirmed()).andReturn(2);
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        expect(unit1.getStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        
        //on MessageUnit.confirmed
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.CONFIRMED);
        expect(unit1.getSequenceNumber()).andReturn(1);
        expect(unit1.getConfirmTime()).andReturn(1l);
        expect(unit1.getStatus()).andReturn(MessageUnitStatus.CONFIRMED);
        
        
        mocks.replay();
    }
    
    private void createMocksForFatalTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit1 = mocks.createMock(MessageUnit.class);
        
        expect(config.getMaxMessageUnitsPerTimeUnit()).andReturn(0l).anyTimes();
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit1});
        mess1.addListener(isA(OutQueue.class));
        expect(unit1.addListener(isA(OutQueue.class))).andReturn(unit1);
        
        //on MessageUnit.fatal
        expect(unit1.getSequenceNumber()).andReturn(1);
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.FATAL);
        expect(unit1.getStatus()).andReturn(MessageUnitStatus.FATAL);
        
        mocks.replay();
    }
    
    private void createMocksForDelayedTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit1 = mocks.createMock(MessageUnit.class);
        
        expect(config.getMaxMessageUnitsPerTimeUnit()).andReturn(0l).anyTimes();
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit1});
        mess1.addListener(isA(OutQueue.class));
        expect(unit1.addListener(isA(OutQueue.class))).andReturn(unit1);
        
        //on MessageUnit.delayed
        expect(unit1.getXTime()).andReturn(System.currentTimeMillis()+100);
        expect(unit1.getDst()).andReturn("123");
        
        //on first getNext() when direction locked
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.getStatus()).andReturn(MessageUnitStatus.READY);
        
        //on next getNext() when direction unlocked
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit1.getDst()).andReturn("123");
        expect(unit1.checkStatus()).andReturn(MessageUnitStatus.READY);
        
        mocks.replay();
    }

    private OutQueue createQueueAndAddMessage() {
        OutQueue queue = new OutQueue(config, loggerHelper);
        assertTrue(queue.addMessage(mess1));
        assertEquals(0, queue.howManyUnconfirmed());
        assertFalse(queue.isEmpty());
        return queue;
    }

}