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
package org.onesec.raven.net.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.net.ByteBufferPool;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractPacketDispatcherTest extends Assert {
    private final static Logger classLogger = LoggerFactory.getLogger("org.onesec.T");
    private final static int DATA_LEN = 20;
    
    private final static AtomicInteger stoppedThreadsCount = new AtomicInteger();
    private ByteBufferPoolImpl bufferPool;
    private IMocksControl control;
    private LoggerHelper logger;
    
    @Before
    public void prepare() {
        bufferPool = new ByteBufferPoolImpl();
        control = createControl();
        logger = new LoggerHelper(LogLevel.TRACE, "", "", classLogger);
    }
    
//    @Test
    public void startStopTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        Thread.sleep(500);
        dispatcher.stop();
        Thread.sleep(100);
        control.verify();
        assertEquals(3, stoppedThreadsCount.get());
    }
    
    @Test
    public void separateProcessorsForReadWriteUDPTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234);
        ReadPacketProcessor reader = new ReadPacketProcessor(addr, logger, bufferPool, true);
        WritePacketProcessor writer = new WritePacketProcessor(addr, logger, bufferPool, true);
        dispatcher.addPacketProcessor(reader);
        dispatcher.addPacketProcessor(writer);
        Thread.sleep(5000);
        
        dispatcher.stop();
        assertTrue(reader.validSeq);
//        assertEquals(5, reader.data.size());
    }
    
    private ExecutorService trainExecutor(IMocksControl control, int selectorCount, int dataProcessorsCount) throws Exception {
        ExecutorService executor = control.createMock(ExecutorService.class);
        executor.execute(executeTask());
        expectLastCall().times(selectorCount);
        expect(executor.executeQuietly(executeTask())).andReturn(true).times(dataProcessorsCount);
        return executor;
    }
    
    public static Task executeTask() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(final Object arg) {
                new Thread() {
                    @Override public void run() {
                        ((Task)arg).run();
                        stoppedThreadsCount.incrementAndGet();
                    }
                }.start();
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    private class ReadPacketProcessor extends AbstractPacketProcessor {
        private boolean validSeq = true;
        private byte prevVal = 0;

        public ReadPacketProcessor(SocketAddress address, LoggerHelper logger, ByteBufferPool bufferPool, boolean datagram) {
            super(address, true, false, true, datagram, "Reader", new LoggerHelper(logger, "Reader. "), bufferPool, 1);
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
        }

        @Override
        protected void doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            if (buffer==null) {
                logger.debug("Received end of channel");
                if (prevVal!=DATA_LEN)
                    validSeq = false;
                stop();
            } else {
                buffer.flip();
                logger.debug("Processing read operation");
                if (buffer.remaining()>0) {
                    byte val = buffer.get();
                    logger.debug("Received byte: "+val);
                    if (val != prevVal + 1) {
                        validSeq = false;
//                        stopUnexpected(new Exception(String.format(
//                                "Invalid val. Expected %s but received %s", prevVal+1, val)));
                    } else
                        prevVal = val;
                    if (val==DATA_LEN)
                        stop();
                }
                buffer.clear();
            }
        }

        public void processOutboundBuffer(WritableByteChannel channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
 
        public boolean hasPacketForOutboundProcessing() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    private class WritePacketProcessor extends AbstractPacketProcessor {
        private final int[] data = new int[DATA_LEN];
        private volatile int pos = 0;

        public WritePacketProcessor(SocketAddress address, LoggerHelper logger, ByteBufferPool bufferPool, boolean datagram) {
            super(address, false, true, false, datagram, "Writer", new LoggerHelper(logger, "Writer. "), bufferPool, 1);
            for (int i=0; i<data.length; ++i)
                data[i] = i+1;
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
        }

        @Override
        protected void doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processOutboundBuffer(WritableByteChannel channel) {
            logger.debug("Writing byte to the channel: "+data[pos]);
            outBuffer.clear();
            outBuffer.put((byte)data[pos++]);
            outBuffer.flip();
            try {
                channel.write(outBuffer);
            } catch (IOException ex) {
                logger.error("Error write data", ex);
            }
            if (pos==data.length)
                stop();
        }

        public boolean hasPacketForOutboundProcessing() {
            return pos<data.length;
        }
    }
}
