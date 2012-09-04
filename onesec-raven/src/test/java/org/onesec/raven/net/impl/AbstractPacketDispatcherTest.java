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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
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
    public void readWriteTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234);
        ReadPacketProcessor reader = new ReadPacketProcessor(addr, logger, bufferPool);
        WritePacketProcessor writer = new WritePacketProcessor(addr, logger, bufferPool);
        dispatcher.addPacketProcessor(reader);
        dispatcher.addPacketProcessor(writer);
        Thread.sleep(5000);
        
        dispatcher.stop();
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

        public ReadPacketProcessor(SocketAddress address, LoggerHelper logger, ByteBufferPool bufferPool) {
            super(address, true, false, true, false, "Reader", new LoggerHelper(logger, "Reader. "), bufferPool, 1);
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
        }

        public boolean processInboundBuffer(ByteBuffer buffer) {
            return true;
        }

        public void processInboundBuffer(ReadableByteChannel channel) {
            logger.debug("Processing read operation");
            try {
                inBuffer.clear();
                if (channel.read(inBuffer)==1) {
                    inBuffer.flip();
                    logger.debug("Received byte: "+inBuffer.get());
                }
            } catch (IOException ex) {
                logger.error("Error reading from channel", ex);
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
        private final List<Integer> data = Arrays.asList(1,2,3,4,5);
        private volatile int pos = 0;

        public WritePacketProcessor(SocketAddress address, LoggerHelper logger, ByteBufferPool bufferPool) {
            super(address, false, true, false, false, "Writer", new LoggerHelper(logger, "Writer. "), bufferPool, 1);
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
        }

        public void processInboundBuffer(ReadableByteChannel channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processOutboundBuffer(WritableByteChannel channel) {
            logger.debug("Writing byte to the channel: "+data.get(pos).byteValue());
            outBuffer.clear();
            outBuffer.put(data.get(pos++).byteValue());
            outBuffer.flip();
            try {
                channel.write(outBuffer);
            } catch (IOException ex) {
                logger.error("Error write data", ex);
            }
            if (pos==5)
                validFlag.set(false);
        }

        public boolean hasPacketForOutboundProcessing() {
            return pos<5;
        }
        
    }
}
