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
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.DataProcessor;
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
public abstract class AbstractPacketDispatcher<P extends PacketProcessor> implements PacketDispatcher<P>, Task{
    private static int BUFFER_SIZE = 512;
    
    protected final ExecutorService executor;
    protected final Node owner;
    protected final LoggerHelper logger;
    protected final String name;
    protected final ByteBufferPool byteBufferPool;
    private final DataProcessor[] dataProcessors;
    private final boolean[] runningFlag;
    
    protected final LinkedList<PacketProcessor> pendingProcessors = new LinkedList<PacketProcessor>();
    private final ReentrantLock pendingProcessorsLock = new ReentrantLock();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    protected volatile String statusMessage;
    private volatile boolean hasPendingProcessors = false;
    private boolean hasNotStartedWorkers = false;

    public AbstractPacketDispatcher(ExecutorService executor, int workersCount, String name, Node owner
            , LoggerHelper logger, ByteBufferPool byteBufferPool) 
    {
        this.executor = executor;
        this.logger = logger;
        this.owner = owner;
        this.name = name;
        this.byteBufferPool = byteBufferPool;
        this.dataProcessors = new DataProcessor[workersCount];
        this.runningFlag = new boolean[workersCount];
    }
    
    public void addPacketProcessor(P packetProcessor) {
        pendingProcessorsLock.lock();
        try {
            pendingProcessors.add(packetProcessor);
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

    public void run() {
        final Selector selector = createSelector();
        if (selector==null)
            return;
        try {
            createWorkers();
            while (!stopFlag.get()) {
                if (hasPendingProcessors)
                    processPendingProcessors(selector);
                if (hasNotStartedWorkers)
                    startNotStartedWorkers();
                
            }
        } finally {
            closeWorkers();
            closeSelector(selector);
        }
    }
    
    private void processSelection(Selector selector) {
        try {
            selector.select(1);
            Set<SelectionKey> keys = selector.selectedKeys();
            if (keys!=null && !keys.isEmpty()) {
                Iterator<SelectionKey> it = keys.iterator();
                while(it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isAcceptable()) {
                        
                        it.remove();
                    } else if (key.isConnectable()) {
                        ((SocketChannel)key.channel()).finishConnect();
                        it.remove();
                    } else if (key.isReadable() || key.isWritable()) {
                        
                    }
                }
            }
                
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error in selection process", ex);
        }
    }
    
    private void acceptIncomingConnection(Selector selector, SelectionKey key) {
        PacketProcessor pp = (PacketProcessor) key.attachment();
        try {
            try {
                SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
//                socketChannel.
            } catch (IOException ex) {
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
                    Channel channel = null;
                    SelectionKey key = null;
                    try {
                        if (pp.isServerSideProcessor()) {
                            ServerSocketChannel serverSocket = ServerSocketChannel.open();
                            channel = serverSocket;
                            serverSocket.configureBlocking(false);
                            serverSocket.socket().bind(pp.getAddress());
                            serverSocket.register(selector, SelectionKey.OP_ACCEPT, pp);
                        } else {
                            SocketChannel socketChannel = SocketChannel.open();
                            channel = socketChannel;
                            socketChannel.configureBlocking(false);
                            key = socketChannel.register(selector, genOpsForKey(SelectionKey.OP_CONNECT, pp), pp);
                            socketChannel.connect(pp.getAddress());
                        }
                    } catch (Throwable e) {
                        if (key!=null)
                            key.cancel();
                        else if (channel!=null)
                            closeChannel(channel);
                        pp.stopUnexpected(e);
                    }
                }
                pendingProcessors.clear();
                hasPendingProcessors = false;
            } finally {
                pendingProcessorsLock.unlock();
            }
        } 
    }
    
    private int genOpsForKey(int initialOps, PacketProcessor pp) {
        return initialOps 
                | (pp.isNeedInboundProcessing()?SelectionKey.OP_READ:0)
                | (pp.isNeedOutboundProcessing()?SelectionKey.OP_WRITE:0);
    }
    
    private void closeKey(SelectionKey key) {
        try {
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
            dataProcessors[i] = new DataProcessorImpl(
                    owner, new LoggerHelper(logger, "Data processor "+i+". "), BUFFER_SIZE, byteBufferPool);
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
    
    protected abstract void executeOneCycle(Selector selector);
    
    private Selector createSelector() {
        try {
            return Selector.open();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error creating SELECTOR for {}", name);
            return null;
        }
    }
    
    private void closeSelector(Selector selector) {
        try {
            selector.close();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error closing SELECTOR for {}", name);
        }
    }
}
