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
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
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
    protected final ExecutorService executor;
    protected final int workersCount;
    protected final Node owner;
    protected final LoggerHelper logger;
    protected final String name;
    protected final LinkedList<PacketProcessor> pendingProcessors = new LinkedList<PacketProcessor>();
    private final ReentrantLock pendingProcessorsLock = new ReentrantLock();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    protected volatile String statusMessage;

    public AbstractPacketDispatcher(ExecutorService executor, int workersCount, String name, Node owner
            , LoggerHelper logger) 
    {
        this.executor = executor;
        this.workersCount = workersCount;
        this.logger = logger;
        this.owner = owner;
        this.name = name;
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
        //createWorkers
        try {
            while (!stopFlag.get()) {

            }
        } finally {
            //closeWorkers
            closeSelector(selector);
        }
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
