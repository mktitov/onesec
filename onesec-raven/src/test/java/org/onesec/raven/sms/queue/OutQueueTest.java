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
import org.onesec.raven.sms.MessageUnitStatus;
/**
 *
 * @author Mikhail Titov
 */
public class OutQueueTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger(OutQueueTest.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "SMS. ", null, logger);
    
    //mocks
    private IMocksControl mocks;
    private SmsConfig config;
    private ShortTextMessage mess1;
    private ShortTextMessage mess2;
    private MessageUnit unit;
    

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
        assertSame(unit, queue.getNext());
    }
    
    @Test
    public void unitSubmitTest() {
        createMocksForSubmitTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit, MessageUnitStatus.READY, MessageUnitStatus.SUBMITTED);
        assertEquals(1, queue.howManyUnconfirmed());
        assertNull(queue.getNext());
        assertSame(unit, queue.getMessageUnit(1));
        assertFalse(queue.isEmpty());
        
        queue.statusChanged(unit, MessageUnitStatus.SUBMITTED, MessageUnitStatus.CONFIRMED);
        assertFalse(queue.isEmpty());
        assertEquals(0, queue.howManyUnconfirmed());
        assertNull(queue.getNext());
        assertTrue(queue.isEmpty());
    }
    
    @Test
    public void unitFatalTest() {
        createMocksForFatalTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit, MessageUnitStatus.SUBMITTED, MessageUnitStatus.FATAL);
        assertNull(queue.getNext());
        assertTrue(queue.isEmpty());
    }
    
    @Test
    public void unitDelayedTest() throws InterruptedException {
        createMocksForDelayedTest();
        OutQueue queue = createQueueAndAddMessage();
        
        queue.statusChanged(unit, MessageUnitStatus.READY, MessageUnitStatus.DELAYED);
        assertNull(queue.getNext());
        Thread.sleep(100);
        assertSame(unit, queue.getNext());
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
        unit = mocks.createMock(MessageUnit.class);
        
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit});
        mess1.addListener(isA(OutQueue.class));
        expect(unit.addListener(isA(OutQueue.class))).andReturn(unit);
        
        //on getNext()
        expect(config.getMaxUnconfirmed()).andReturn(1);
        expect(unit.getDst()).andReturn("123");
        expect(unit.checkStatus()).andReturn(MessageUnitStatus.READY);
        mocks.replay();
    }
    
    private void createMocksForSubmitTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit = mocks.createMock(MessageUnit.class);
        
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit});
        mess1.addListener(isA(OutQueue.class));
        expect(unit.addListener(isA(OutQueue.class))).andReturn(unit);
        
        //on MessageUnit.submitted()
        expect(unit.getSequenceNumber()).andReturn(1);       
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit.getDst()).andReturn("123");
        expect(unit.checkStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        expect(unit.getStatus()).andReturn(MessageUnitStatus.SUBMITTED);
        
        //on MessageUnit.confirmed
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit.getDst()).andReturn("123");
        expect(unit.checkStatus()).andReturn(MessageUnitStatus.CONFIRMED);
        expect(unit.getSequenceNumber()).andReturn(1);
        expect(unit.getStatus()).andReturn(MessageUnitStatus.CONFIRMED);
        
        
        mocks.replay();
    }
    
    private void createMocksForFatalTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit = mocks.createMock(MessageUnit.class);
        
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit});
        mess1.addListener(isA(OutQueue.class));
        expect(unit.addListener(isA(OutQueue.class))).andReturn(unit);
        
        //on MessageUnit.fatal
        expect(unit.getSequenceNumber()).andReturn(1);
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit.getDst()).andReturn("123");
        expect(unit.checkStatus()).andReturn(MessageUnitStatus.FATAL);
        expect(unit.getStatus()).andReturn(MessageUnitStatus.FATAL);
        
        mocks.replay();
    }
    
    private void createMocksForDelayedTest() {
        config = mocks.createMock(SmsConfig.class);
        mess1 = mocks.createMock("mess1", ShortTextMessage.class);
        unit = mocks.createMock(MessageUnit.class);
        
        //on addMessage()
        expect(config.getMaxMessagesInQueue()).andReturn(1).times(1);
        expect(mess1.getUnits()).andReturn(new MessageUnit[]{unit});
        mess1.addListener(isA(OutQueue.class));
        expect(unit.addListener(isA(OutQueue.class))).andReturn(unit);
        
        //on MessageUnit.delayed
        expect(unit.getXTime()).andReturn(System.currentTimeMillis()+100);
        expect(unit.getDst()).andReturn("123");
        
        //on first getNext() when direction locked
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit.getDst()).andReturn("123");
        expect(unit.getStatus()).andReturn(MessageUnitStatus.READY);
        
        //on next getNext() when direction unlocked
        expect(config.getMaxUnconfirmed()).andReturn(2);        
        expect(unit.getDst()).andReturn("123");
        expect(unit.checkStatus()).andReturn(MessageUnitStatus.READY);
        
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