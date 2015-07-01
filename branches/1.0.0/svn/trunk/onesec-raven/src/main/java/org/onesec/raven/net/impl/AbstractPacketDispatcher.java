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
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.DataProcessor;
import org.onesec.raven.net.KeysSet;
import org.onesec.raven.net.PacketDispatcher;
import org.onesec.raven.net.PacketProcessor;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractPacketDispatcher<P extends PacketProcessor> 
        implements PacketDispatcher<P>, Task
{
    private static int BUFFER_SIZE = 512;
    
    protected final ExecutorService executor;
    protected final Node owner;
    protected final LoggerHelper logger;
    protected final ByteBufferPool byteBufferPool;
    private final DataProcessor[] dataProcessors;
    private final int dataProcessorsCount;
    private final boolean[] runningFlag;
    private final KeysSet[] keysSet;
    private int curKeySet = 0;
    private final Queue<SelectionKey> keysQueue = new ConcurrentLinkedQueue<SelectionKey>();
    private final AtomicInteger keysQueueSize = new AtomicInteger();
    private final AtomicBoolean selectionThreadWaiting = new AtomicBoolean(false);
    
    protected final LinkedList<PacketProcessor> pendingProcessors = new LinkedList<PacketProcessor>();
    private final ReentrantLock pendingProcessorsLock = new ReentrantLock();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    protected volatile String statusMessage;
    private volatile boolean hasPendingProcessors = false;
    private boolean hasNotStartedWorkers = false;
    private int nextDataProcessor = 0;

    public AbstractPacketDispatcher(ExecutorService executor, int workersCount, Node owner
            , LoggerHelper logger, ByteBufferPool byteBufferPool) 
    {
        this.executor = executor;
        this.logger = logger;
        this.owner = owner;
        this.byteBufferPool = byteBufferPool;
        this.dataProcessors = new DataProcessor[workersCount];
        this.runningFlag = new boolean[workersCount];
        this.dataProcessorsCount = workersCount;
        keysSet = new KeysSet[10];
        for (int i=0; i<keysSet.length; ++i)
            keysSet[i] = new KeysSetImpl(100);
    }
    
    public void addPacketProcessor(P packetProcessor) {
        pendingProcessorsLock.lock();
        try {
            pendingProcessors.add(packetProcessor);
            hasPendingProcessors = true;
        } finally {
            pendingProcessorsLock.unlock();
        }
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void stop() {
        stopFlag.compareAndSet(false, true);
    }

    public void run() {
        if (logger.isErrorEnabled())
            logger.info("Initializing");
        final Selector selector = createSelector();
        if (selector==null)
            return;
        try {
            createWorkers();
            if (logger.isInfoEnabled())
                logger.info("Successfully started");
            long lastCheckForInvalidPP = System.currentTimeMillis();
            while (!stopFlag.get()) {
                try {
                    if (hasPendingProcessors)
                        processPendingProcessors(selector);
                    if (hasNotStartedWorkers)
                        startNotStartedWorkers();
                    long ts = System.currentTimeMillis();
                    if (lastCheckForInvalidPP+20 <= ts) {
                        closeKeysForInvalidPacketProcessors(selector);
                        lastCheckForInvalidPP = ts;
                    }
                    processSelection(selector);
//                    Thread.sleep(1);
                } catch (Throwable e) {
                    if (logger.isErrorEnabled())
                        logger.error("Unexpected error in processing cycle", e);
                }
            }
        } finally {
            closeWorkers();
            closeSelector(selector);
            if (logger.isInfoEnabled())
                logger.info("Stopped");
        }
    }
    
    private void closeKeysForInvalidPacketProcessors(Selector selector) {
        for (SelectionKey key: selector.keys()) {
            PacketProcessor pp = (PacketProcessor) key.attachment();
            if (!pp.isProcessing()) {
                if (!pp.isValid())
                    closeKey(key);
//                else if ( (key.interestOps() & (SelectionKey.OP_ACCEPT | SelectionKey.OP_CONNECT))==0 ) {
//                    if (pp.isNeedOutboundProcessing() && pp.hasPacketForOutboundProcessing())
//                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
//                    if (pp.isNeedInboundProcessing())
//                        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
//                }
            }
        }
    }
    
    private void processSelection(Selector selector) throws Exception {
        try {
            selector.select(3);
//            selector.selectNow();
            Set<SelectionKey> keys = selector.selectedKeys();
            if (keys!=null && !keys.isEmpty()) {
                Iterator<SelectionKey> it = keys.iterator();
                while(it.hasNext()) {
                    SelectionKey key = it.next();
                    PacketProcessor pp = (PacketProcessor) key.attachment();
                    if (!pp.isValid())
                        continue;
                    if (key.isReadable() || key.isWritable()) {
                        if (pp.isProcessing() || pushKeyToKeysSet(key))
                            it.remove();
                    } else if (key.isAcceptable()) {
                        acceptIncomingConnection(selector, key);
                        it.remove();
                    } else if (key.isConnectable()) {
                        ((SocketChannel)key.channel()).finishConnect();
                        key.interestOps(genOpsForKey(0, (PacketProcessor)key.attachment()));
                        it.remove();
                    }
                }
                submitPartialKeysSet();
            } 
            else
                Thread.sleep(1);
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error in selection process", ex);
        }
    }
    
    private void submitPartialKeysSet() {
        if (curKeySet==keysSet.length)
            curKeySet = 0;
        if (keysSet[curKeySet].isFree() && keysSet[curKeySet].hasKeys()) 
            submitKeysSetToDataProcessor(keysSet[curKeySet++].switchToWaitingForProcess());
    }
    
    private boolean pushKeyToKeysSet(SelectionKey key) {
        PacketProcessor pp = (PacketProcessor) key.attachment();
        if (key.isReadable() || (key.isWritable() && pp.hasPacketForOutboundProcessing())) {
            int attempt = 0;
            while(attempt<keysSet.length) {
                if (curKeySet==keysSet.length)
                    curKeySet = 0;
                if (keysSet[curKeySet].isFree()) {
                    if (!keysSet[curKeySet].add(key)) 
                        submitKeysSetToDataProcessor(keysSet[curKeySet++]);
                    return true;
                }
                curKeySet++;
                attempt++;
            }
        }
        return false;
    }
    
    private void submitKeysSetToDataProcessor(KeysSet keys) {
        int attempt = 0;
        while (attempt<dataProcessors.length) {
            if (nextDataProcessor>=dataProcessors.length)
                nextDataProcessor = 0;
            if (runningFlag[nextDataProcessor] && dataProcessors[nextDataProcessor++].processData(keys)) 
                return;
            ++attempt;
        }
    }
    
//    private boolean submitOperationToDataProcessor(SelectionKey key) {
//        int attempt = 0;
//        while (attempt<dataProcessors.length) {
//            if (nextDataProcessor>=dataProcessors.length)
//                nextDataProcessor = 0;
//            if (runningFlag[nextDataProcessor] && dataProcessors[nextDataProcessor++].processData(key)) {
//                key.interestOps(0);
//                return true;
//            }
//            ++attempt;
//        }
//        return false;
//    }
        
        
//        if (keysQueueSize.get()<=dataProcessorsCount*2)
//            for (int i=0; i<dataProcessors.length; ++i)
//                if (runningFlag[i] && dataProcessors[i].processData(key))
//                    return;
//        PacketProcessor pp = (PacketProcessor) key.attachment();
//        if (pp.changeToProcessing()) {
//            key.interestOps(0);
//            keysQueue.add(key);
//            keysQueueSize.incrementAndGet();
//        }
    
//    public SelectionKey getNextKey() {
//        SelectionKey key = keysQueue.poll();
//        if (key!=null) {
//            int queueSize = keysQueueSize.decrementAndGet();
//            if (queueSize<=dataProcessorsCount*5 && selectionThreadWaiting.compareAndSet(true, false))
//                synchronized(keysQueue) {
//                    keysQueue.notify();
//                }
//        }
//        return key;
//    }
//    
//    private boolean _submitOperationToDataProcessor(SelectionKey key) {
//        PacketProcessor pp = (PacketProcessor) key.attachment();
//        if ( !pp.isProcessing() && 
//             (key.isReadable() || (key.isWritable() && pp.hasPacketForOutboundProcessing())) )
//        {
//            for (int i=0; i<dataProcessors.length; ++i)
//                if (runningFlag[i] && dataProcessors[i].processData(key))
//                    return true;
//        }
//        return false;
//    }
    
    private void acceptIncomingConnection(Selector selector, SelectionKey key) {
        PacketProcessor pp = (PacketProcessor) key.attachment();
        try {
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Accepting incoming connection");
                SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept(); //can return null
                socketChannel.configureBlocking(false);
//                socketChannel.register(selector, 0, pp);
                socketChannel.register(selector, genOpsForKey(0, pp), pp);
            } catch (Throwable ex) {
                pp.stopUnexpected(ex);
            }
        } finally {
            closeKey(key);
        }
    }
    
    private void processPendingProcessors(Selector selector) {
        if (pendingProcessorsLock.tryLock()) {
            try {
                for (PacketProcessor pp: pendingProcessors) {
                    SelectableChannel channel = null;
                    SelectionKey key = null;
                    if (pp.isServerSideProcessor()) {
                        if (logger.isDebugEnabled())
                            logger.debug("Registering new server PacketProcessor: {}", pp);
                        if (!pp.isDatagramProcessor())
                            registerServerChannel(selector, pp);
                        else
                            registerDatagramServerChannel(selector, pp);
                    } else {
                        if (logger.isDebugEnabled())
                            logger.debug("Registering new client PacketProcessor: {}", pp);
                        if (!pp.isDatagramProcessor())
                            registerClientChannel(selector, pp);
                        else 
                            registerDatagramClientChannel(selector, pp);
                    }
                }
                pendingProcessors.clear();
                hasPendingProcessors = false;
            } finally {
                pendingProcessorsLock.unlock();
            }
        } 
    }
    
    private void registerDatagramClientChannel(Selector selector, PacketProcessor pp) {
        SelectionKey key = null; DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().setTrafficClass(22);
            channel.connect(pp.getAddress());
            key = channel.register(selector, genOpsForKey(0, pp), pp);
        } catch (Throwable e) {
            stopPacketProcessorUnexpected(pp, key, channel, e);
        }
    }
    
    private void registerDatagramServerChannel(Selector selector, PacketProcessor pp) {
        SelectionKey key = null; DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(pp.getAddress());
            key = channel.register(selector, genOpsForKey(0, pp), pp);
//            key = channel.register(selector, genOpsForKey(0, pp), pp);
        } catch (Throwable e) {
            stopPacketProcessorUnexpected(pp, key, channel, e);
        }
    }
    
    private void registerServerChannel(Selector selector, PacketProcessor pp) {
        SelectionKey key = null; ServerSocketChannel channel = null;
        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(pp.getAddress());
            channel.register(selector, SelectionKey.OP_ACCEPT, pp);
        } catch (Throwable e) {
            stopPacketProcessorUnexpected(pp, key, channel, e);
        }
    }
    
    private void registerClientChannel(Selector selector, PacketProcessor pp) {
        SelectionKey key = null; SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            key = channel.register(selector, SelectionKey.OP_CONNECT, pp);
            channel.connect(pp.getAddress());
        } catch (Throwable e) {
            stopPacketProcessorUnexpected(pp, key, channel, e);
        }
    }
        
    private void stopPacketProcessorUnexpected(PacketProcessor pp, SelectionKey key
            , Channel channel, Throwable e) 
    {
        if (key!=null)
            key.cancel();
        else if (channel!=null)
            closeChannel(channel);
        pp.stopUnexpected(e);
    }
    
    private int genOpsForKey(int initialOps, PacketProcessor pp) {
        return initialOps
                | (pp.isNeedInboundProcessing()?SelectionKey.OP_READ:0)
                | (pp.isNeedOutboundProcessing()?SelectionKey.OP_WRITE:0);
    }
    
    private void closeKey(SelectionKey key) {
        try {
            if (logger.isDebugEnabled()) 
                logger.debug("Unregistering packet processor: {}", ((PacketProcessor)key.attachment()));
            key.cancel();
            key.channel().close();
        } catch (Throwable ex) {
            if (logger.isErrorEnabled())
                logger.error("Error closing SelectionKey and associated channel", ex);
        }
    }
    
    private void closeChannel(Channel channel) {
        try {
            channel.close();
        } catch (Throwable ex) {
            if (logger.isErrorEnabled())
                logger.error("Error closing channel", ex);
        }
        
    }
    
    private void startNotStartedWorkers() {
        hasNotStartedWorkers = false;
        for (int i=0; i<dataProcessors.length; ++i)
            if (!runningFlag[i]) {
                if (executor.executeQuietly(dataProcessors[i])) 
                    runningFlag[i] = true;
                else
                    hasNotStartedWorkers = true;
            }
    }
    
    private void createWorkers() {
        for (int i=0; i<dataProcessors.length; ++i) {
            dataProcessors[i] = new DataProcessorImpl(owner, this, new LoggerHelper(logger, "Data processor "+i+". "));
            runningFlag[i] = executor.executeQuietly(dataProcessors[i]);
            if (!runningFlag[i])
                hasNotStartedWorkers = true;
        }
    }
    
    private void closeWorkers() {
        for (DataProcessor dataProcessor: dataProcessors)
            if (dataProcessor!=null)
                dataProcessor.stop();
    }
    
//    protected abstract void executeOneCycle(Selector selector);
    
    private Selector createSelector() {
        try {
            return Selector.open();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error creating SELECTOR");
            return null;
        }
    }
    
    private void closeSelector(Selector selector) {
        try {
            Set<SelectionKey> keys = selector.keys();
            if (keys!=null) {
                Exception e  = new Exception("SelectorDispatcher was closed");
                for (SelectionKey key: keys) {
                    ((PacketProcessor)key.attachment()).stopUnexpected(e);
                    closeChannel(key.channel());
                }
            }
            selector.close();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error closing SELECTOR");
        }
    }
}
