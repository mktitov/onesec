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

import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.DataProcessor;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataProcessor implements DataProcessor  {
    private final Node owner;
    private final LoggerHelper logger;
    private final int bufferSize;
    private final ByteBufferPool byteBufferPool;
    private final AtomicReference<SelectionKey> keyToProcess = new AtomicReference<SelectionKey>();
//    private final 
    private volatile String statusMessage;
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private volatile boolean processingData = false;

    public AbstractDataProcessor(Node owner, LoggerHelper logger, int bufferSize
            , ByteBufferPool byteBufferPool) 
    {
        this.owner = owner;
        this.logger = logger;
        this.bufferSize = bufferSize;
        this.byteBufferPool = byteBufferPool;
    }

    public boolean processData(SelectionKey key) {
        PacketProcessor pp = (PacketProcessor) key.attachment();
        if (pp.changeToProcessing()) {
            if (keyToProcess.compareAndSet(null, key)) {
//                key.interestOps(0);
                if (key.isWritable())
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                synchronized(this) {
                    notify();
                }
                return true;
            } else
                pp.changeToUnprocessing();
        }
        return false;
    }

    public void stop() {
        stopFlag.compareAndSet(false, true);
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void run() {
        if (logger.isInfoEnabled())
            logger.info("Successfully started");
//        final ByteBufferHolder bufferHolder = byteBufferPool.getBuffer(bufferSize);
//        final ByteBuffer buffer = bufferHolder.getBuffer();
        try {
//            processingData = false;
            while (!stopFlag.get()) {
                SelectionKey key = keyToProcess.get();
                if (key!=null) {
//                    processingData = true;
                    keyToProcess.set(null);
                    try {
                        try {
                            doProcessData(key);
                        } catch (Throwable e) {
                            if (logger.isErrorEnabled())
                                logger.error("Error processig packet", e);
                        }
                    } finally {
                        ((PacketProcessor)key.attachment()).changeToUnprocessing();
                    }
                } else {
//                    processingData = false;
                    synchronized(this) {
                        try {
                            wait(5);
                        } catch (InterruptedException ex) {
                            if (logger.isErrorEnabled())
                                logger.error("Interrupted");
                            return;
                        }
                    }
                }
            }
        } finally {
//            bufferHolder.release();
            if (logger.isInfoEnabled())
                logger.info("Stopped");
        }
    }
    
    protected abstract void doProcessData(SelectionKey key) throws Exception;
}
