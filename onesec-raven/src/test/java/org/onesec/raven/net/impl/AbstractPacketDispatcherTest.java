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

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
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
    private final static int DATA_LEN = 127;
    private final static int DATA_LEN2 = 100;
    private final static int DATA_LEN3 = 500;
    private final static int PACKET_SIZE = 160;
    
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
    
//    @Test
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
    
//    @Test
    public void separateProcessorsForReadWriteTCPTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234);
        ReadPacketProcessor reader = new ReadPacketProcessor(addr, logger, bufferPool, false);
        WritePacketProcessor writer = new WritePacketProcessor(addr, logger, bufferPool, false);
        dispatcher.addPacketProcessor(reader);
        dispatcher.addPacketProcessor(writer);
        Thread.sleep(5000);
        
        dispatcher.stop();
        assertTrue(reader.validSeq);
//        assertEquals(5, reader.data.size());
    }
    
//    @Test
    public void twoWayProcessorsTCPTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234);
        ServerTwoWayPacketProcessor server = new ServerTwoWayPacketProcessor(addr, false, logger);
        ClientTwoWayPacketProcessor client = new ClientTwoWayPacketProcessor(addr, false, logger);
        dispatcher.addPacketProcessor(server);
        dispatcher.addPacketProcessor(client);
        Thread.sleep(5000);
        
        dispatcher.stop();
        assertTrue(client.valid);
        assertEquals(DATA_LEN2, client.lastReceivedVal);
//        assertEquals(5, reader.data.size());
    }
    
//    @Test
    public void realTimeProtocolTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 1, 2);
        control.replay();
        
        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
                executor, 2, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
        executor.execute(dispatcher);
        SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234);
        SocketAddress addr2 = new InetSocketAddress(Inet4Address.getLocalHost(), 2234);
        RealTimeServerPacketProcessor server = new RealTimeServerPacketProcessor(1, 160, addr, true, logger);
        RealTimeClientPacketProcessor client = new RealTimeClientPacketProcessor(1, 160, 200, addr2, true, logger);
        dispatcher.addPacketProcessor(server);
        dispatcher.addPacketProcessor(client);
        Thread.sleep(5000);
        
        dispatcher.stop();
        client.showStat();
        server.showStat();
//        assertEquals(5, reader.data.size());
    }
    
    @Test
    public void realTimeProtocolLoadTest() throws Exception {
        ExecutorService executor = trainExecutor(control, 2, 8);
        control.replay();
        int packetsCount = 1000;
        int processors = 800;
        Dispatcher disp1 = new Dispatcher(1, processors/2, packetsCount, executor, logger);
        Dispatcher disp2 = new Dispatcher(2, processors/2, packetsCount, executor, logger);
        Thread.sleep(2000);
        disp1.start(1234);
        disp2.start(2234);
        Thread.sleep(packetsCount*20+processors*5+500);        
        disp1.stop();
        disp2.stop();
        
//        AbstractPacketDispatcher dispatcher = new AbstractPacketDispatcher(
//                executor, 4, null, new LoggerHelper(logger, "Dispatcher. "), bufferPool);
//        executor.execute(dispatcher);
//        Thread.sleep(2000);
//        
//        Pair[] processors = new Pair[800];
//        int packetsCount = 1000;
//        for (int i=0; i<processors.length; ++i) {
//            SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 1234+i);
//            RealTimeServerPacketProcessor server = new RealTimeServerPacketProcessor(i, 160, addr, true, logger);
//            RealTimeClientPacketProcessor client = new RealTimeClientPacketProcessor(i, 160, packetsCount, addr, true, logger);
//            dispatcher.addPacketProcessor(server);
//            dispatcher.addPacketProcessor(client);
//            Thread.sleep(1);
//            processors[i] = new Pair(server, client);
//        }
//        
//        Thread.sleep(packetsCount*20+processors.length*5+500);        
//        dispatcher.stop();
//        long maxClientDelta = 0;
//        double avgClientDelta = 0.;
//        long maxClientDelta2 = 0;
//        double avgClientDelta2 = 0.;
//        long maxServerDelta = 0;
//        double avgServerDelta = 0;
//        int lostPacketsCount = 0;
//        for (int i=0; i<processors.length; ++i) {
//            Pair p = processors[i];
//            maxClientDelta = Math.max(maxClientDelta, p.client.maxDelta);
//            maxClientDelta2 = Math.max(maxClientDelta2, p.client.maxDelta2);
//            maxServerDelta = Math.max(maxServerDelta, p.server.maxDelta);
//            avgClientDelta += p.client.getAvgDelta();
//            avgClientDelta2 += p.client.getAvgDelta2();
//            avgServerDelta += p.server.getAvgDelta();
//            lostPacketsCount += packetsCount - p.server.countDelta;
//        }
//        avgClientDelta /= processors.length;
//        avgClientDelta2 /= processors.length;
//        avgServerDelta /= processors.length;
//        logger.debug("Client maxDelta: "+maxClientDelta);
//        logger.debug("Client avgDelta: "+avgClientDelta);
//        logger.debug("Client maxDelta2: "+maxClientDelta2);
//        logger.debug("Client avgDelta2: "+avgClientDelta2);
//        logger.debug("Server maxDelta: "+maxServerDelta);
//        logger.debug("Server avgDelta: "+avgServerDelta);
//        logger.debug("Total packets lost: "+lostPacketsCount);
    }
    
    private class Dispatcher {
        private final AbstractPacketDispatcher dispatcher;
        private final Pair[] processors;
        private final LoggerHelper logger;
        private final int packetsCount;
        private final int processorsCount;

        public Dispatcher(int id, int processorsCount, int packetsCount, ExecutorService executor
                , LoggerHelper logger) 
            throws Exception 
        {
            this.logger = new LoggerHelper(logger, "["+id+"]. ");
            this.packetsCount = packetsCount;
            this.processorsCount = processorsCount;
            dispatcher = new AbstractPacketDispatcher(
                    executor, 4, null, new LoggerHelper(this.logger, "Dispatcher. "), bufferPool);
            processors = new Pair[processorsCount];
            executor.execute(dispatcher);
        }
        
        public void start(int startPort) throws Exception {
            for (int i=0; i<processors.length; ++i) {
                SocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), startPort+i);
                RealTimeServerPacketProcessor server = new RealTimeServerPacketProcessor(i, 160, addr, true, logger);
                RealTimeClientPacketProcessor client = new RealTimeClientPacketProcessor(i, 160, packetsCount, addr, true, logger);
                dispatcher.addPacketProcessor(server);
                dispatcher.addPacketProcessor(client);
                Thread.sleep(1);
                processors[i] = new Pair(server, client);
            }
        }
        
        public void stop() {
            dispatcher.stop();
            long maxClientDelta = 0;
            double avgClientDelta = 0.;
            long maxClientDelta2 = 0;
            double avgClientDelta2 = 0.;
            long maxServerDelta = 0;
            double avgServerDelta = 0;
            int lostPacketsCount = 0;
            for (int i=0; i<processors.length; ++i) {
                Pair p = processors[i];
                maxClientDelta = Math.max(maxClientDelta, p.client.maxDelta);
                maxClientDelta2 = Math.max(maxClientDelta2, p.client.maxDelta2);
                maxServerDelta = Math.max(maxServerDelta, p.server.maxDelta);
                avgClientDelta += p.client.getAvgDelta();
                avgClientDelta2 += p.client.getAvgDelta2();
                avgServerDelta += p.server.getAvgDelta();
                lostPacketsCount += packetsCount - p.server.countDelta;
            }
            avgClientDelta /= processors.length;
            avgClientDelta2 /= processors.length;
            avgServerDelta /= processors.length;
            logger.debug("Client maxDelta: "+maxClientDelta);
            logger.debug("Client avgDelta: "+avgClientDelta);
            logger.debug("Client maxDelta2: "+maxClientDelta2);
            logger.debug("Client avgDelta2: "+avgClientDelta2);
            logger.debug("Server maxDelta: "+maxServerDelta);
            logger.debug("Server avgDelta: "+avgServerDelta);
            logger.debug("Total packets lost: "+lostPacketsCount);
        }
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
            super(address, true, false, true, datagram, "Reader", new LoggerHelper(logger, "Reader. "), bufferPool, 512);
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
        }

        @Override
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            if (buffer==null) {
                logger.debug("Received end of channel");
                if (prevVal!=DATA_LEN)
                    validSeq = false;
                return ProcessResult.STOP;
            } 
            logger.debug("Processing read operation");
            while (buffer.remaining()>0) {
                byte val = buffer.get();
                logger.debug("Received byte: "+val);
                if (val != prevVal + 1) 
                    validSeq = false;
                else
                    prevVal = val;
                if (val==DATA_LEN) 
                    return ProcessResult.STOP;
            }
            buffer.clear();
            return ProcessResult.CONT;
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
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
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            logger.debug("Writing byte to the channel: "+data[pos]);
            buffer.clear();
            buffer.put((byte)data[pos++]);
            return pos==data.length? ProcessResult.STOP : ProcessResult.CONT;
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
            return pos<data.length;
        }
    }
    
    private class ServerTwoWayPacketProcessor extends AbstractPacketProcessor {
        private final Queue<Integer> packetsToSend = new LinkedList<Integer>();

        public ServerTwoWayPacketProcessor(SocketAddress address, boolean datagramProcessor, LoggerHelper logger) 
        {
            super(address, true, true, true, datagramProcessor, "Server", new LoggerHelper(logger, "Server. ")
                    , bufferPool, 512);
        }
        
        @Override
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            if (buffer==null) 
                return ProcessResult.STOP;
            while (buffer.hasRemaining()) {
                int val = buffer.getInt();
                logger.debug("Received int: "+val);
                packetsToSend.add(val);
            }
            buffer.clear();
            return ProcessResult.CONT;
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            buffer.compact();
            Integer val = null;
            while (buffer.hasRemaining() && !packetsToSend.isEmpty()) {
                val = packetsToSend.poll();
                buffer.putInt(val);
            }
//            return ProcessResult.CONT;
            return val!=null && val==DATA_LEN2? ProcessResult.STOP : ProcessResult.CONT;
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
            return !packetsToSend.isEmpty();
        }
    }
    
    private class ClientTwoWayPacketProcessor extends AbstractPacketProcessor {
        private int lastSendedVal = 0;
        private int lastReceivedVal = 0;
        private boolean valid = true;
        
        public ClientTwoWayPacketProcessor(SocketAddress address, boolean datagramProcessor, LoggerHelper logger) 
        {
            super(address, true, true, false, datagramProcessor, "Client", new LoggerHelper(logger, "Client. ")
                    , bufferPool, 512);
        }

        @Override
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            if (buffer==null)
                return ProcessResult.STOP;
            int val = 0;
            while (buffer.hasRemaining()) {
                val = buffer.getInt();
                logger.debug("Received response: "+val);
                if (val!=lastReceivedVal+1) {
                    valid = false;
                    return ProcessResult.STOP;
                } else
                    lastReceivedVal = val;
            }
            buffer.clear();
            return val==DATA_LEN2? ProcessResult.STOP : ProcessResult.CONT;
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            buffer.clear();
            if (buffer.hasRemaining()) {
                buffer.putInt(++lastSendedVal);
                logger.debug("Sending request: "+lastSendedVal);
            }
            return ProcessResult.CONT;
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
            return lastSendedVal < DATA_LEN2;
        }
    }
    
    private class RealTimeClientPacketProcessor extends AbstractPacketProcessor {
        private final int id;
        private final int packetSize;
        private final int packetsCount;
        
        private long nextSendTime = 0;
        private long maxDelta = 0;
        private long sumDelta = 0;
        private int countDelta = 0;
        private long maxDelta2 = 0;
        private long sumDelta2 = 0;
        private int countDelta2 = 0;
        private long checkTime;
        private boolean hasPacketToSend = false;
        
        public RealTimeClientPacketProcessor(int id, int packetSize, int packetsCount, SocketAddress address
                , boolean datagramProcessor, LoggerHelper logger) 
        {
            super(address, false, true, false, datagramProcessor, "Client", new LoggerHelper(logger, "Client["+id+"]. ")
                    , bufferPool, 512);
            this.id = id;
            this.packetSize = packetSize;
            this.packetsCount = packetsCount;
        }

        @Override
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            long curTime = System.currentTimeMillis();
            long delta = curTime - checkTime;
            maxDelta2 = Math.max(maxDelta2, delta);
            sumDelta2 += delta;
            ++countDelta2;
            
            buffer.compact();
            if (buffer.hasRemaining()) {
//                logger.debug("Sending packet #"+countDelta2);
                buffer.putInt(id);
                buffer.putInt(countDelta2);
                buffer.putLong(curTime);
                buffer.put((byte)(countDelta2==packetsCount?1:0));
                for (int i=0; i<packetSize; ++i)
                    buffer.put((byte)1);
                hasPacketToSend = false;
            }
            return countDelta2==packetsCount? ProcessResult.STOP : ProcessResult.CONT;
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
            if (hasPacketToSend)
                return true;
            long curTime = System.currentTimeMillis();
            if (curTime>=nextSendTime) {
                if (nextSendTime>0) {
                    maxDelta = Math.max(maxDelta, curTime-nextSendTime);
                    ++countDelta;
                    sumDelta += curTime-nextSendTime;
                    nextSendTime += 20;
                } else
                    nextSendTime = curTime;
                checkTime = curTime;
                hasPacketToSend = true;
                return true;
            } 
            return false;
        }
        
        public double getAvgDelta() {
            return sumDelta/countDelta;
        }
        
        public double getAvgDelta2() {
            return sumDelta2/countDelta2;
        }
        
        public void showStat() {
            logger.debug("avgDelta: "+getAvgDelta());
            logger.debug("maxDelta: "+maxDelta);
            logger.debug("avgDelta2: "+getAvgDelta2());
            logger.debug("maxDelta2: "+maxDelta2);
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    private class RealTimeServerPacketProcessor extends AbstractPacketProcessor {
        private final int id;
        private final int packetSize;
        private long maxDelta = 0;
        private long sumDelta = 0;
        private long countDelta = 0;
        
        public RealTimeServerPacketProcessor(int id, int packetSize, SocketAddress address, boolean datagramProcessor, LoggerHelper logger) 
        {
            super(address, true, false, true, datagramProcessor, "Server", new LoggerHelper(logger, "Server["+id+"]. ")
                    , bufferPool, 512);
            this.id = id;
            this.packetSize = packetSize;
        }

        @Override
        protected ProcessResult doProcessInboundBuffer(ByteBuffer buffer) throws Exception {
            if (buffer==null)
                return ProcessResult.STOP;
            while (buffer.hasRemaining()) {
                int packetId = buffer.getInt();
                if (packetId!=id) {
                    logger.equals("Received packet with invalid id: "+packetId);
                    return ProcessResult.STOP;
                }
                int counter = buffer.getInt();
//                logger.debug("Received packet #"+counter);
                long curTime = System.currentTimeMillis();
                long delta = curTime - buffer.getLong();
                maxDelta = Math.max(maxDelta, delta);
                sumDelta += delta;
                boolean lastPacket = buffer.get()==0? false:true;
                for (int i=0; i<packetSize; ++i) {
                    byte b = buffer.get();
                }
                ++countDelta;
                if (lastPacket)
                    return ProcessResult.STOP;
            }
            buffer.clear();
            return ProcessResult.CONT;
        }
        
        public double getAvgDelta() {
            return sumDelta/countDelta;
        }
        
        public void showStat() {
            logger.debug("avgDelta: "+getAvgDelta());
            logger.debug("maxDelta: "+maxDelta);
        }

        @Override
        protected ProcessResult doProcessOutboundBuffer(ByteBuffer buffer) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected boolean containsPacketsForOutboundProcessing() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void doStopUnexpected(Throwable e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    private class Pair {
        private final RealTimeServerPacketProcessor server;
        private final RealTimeClientPacketProcessor client;

        public Pair(RealTimeServerPacketProcessor server, RealTimeClientPacketProcessor client) {
            this.server = server;
            this.client = client;
        }
    }
}
