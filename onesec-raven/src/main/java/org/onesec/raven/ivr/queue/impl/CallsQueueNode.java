/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.onesec.raven.ivr.queue.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueuesNode.class)
public class CallsQueueNode extends BaseNode implements CallsQueue, Task
{
    @NotNull @Parameter(defaultValue="10")
    private Integer maxQueueSize;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    private AtomicReference<String> statusMessage;
    private AtomicBoolean stopProcessing;
    AtomicBoolean processingThreadRunning;
    private AtomicLong requestIdSeq;
    LinkedList<CallQueueRequestWrapper> queue;
    private ReadWriteLock lock;
    private Condition requestAddedCondition;
    private CallsQueueRequestComparator requestComparator;

    @Override
    protected void initFields() {
        super.initFields();
        statusMessage = new AtomicReference<String>("Waiting for request...");
        stopProcessing = new AtomicBoolean(true);
        processingThreadRunning = new AtomicBoolean(false);
        requestIdSeq = new AtomicLong(1);
        lock = new ReentrantReadWriteLock();
        requestAddedCondition = lock.writeLock().newCondition();
        requestComparator = new CallsQueueRequestComparator();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (processingThreadRunning.get())
            throw new Exception(
                    "Can't start calls queue because of processing thread is still running");
        queue = new LinkedList();
        stopProcessing.set(false);
        requestIdSeq.set(1);
        executor.execute(this);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        stopProcessing.set(true);
    }
    
    public void queueCall(CallQueueRequestWrapper request) 
    {
        if (!Status.STARTED.equals(getStatus()) || stopProcessing.get()) {
            request.addToLog(String.format("Queue (%s) not ready", getName()));
            request.fireRejectedQueueEvent();
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("New request added to the queue ({})", request.toString());
        
        request.setCallsQueue(this);
        if (request.getRequestId()==0)
            request.setRequestId(requestIdSeq.getAndIncrement());
        addRequestToQueue(request);
    }
    
    private void addRequestToQueue(CallQueueRequestWrapper request)
    {
        lock.writeLock().lock();
        try {
            queue.offer(request);
            if (queue.size()>1)
                Collections.sort(queue, requestComparator);
            if (queue.size()>maxQueueSize){
                CallQueueRequestWrapper rejReq = queue.removeLast();
                rejReq.addToLog("queue size was exceeded");
                rejReq.fireRejectedQueueEvent();
            }else {
                request.fireCallQueuedEvent();
                fireQueueNumberChangedEvents();
            }
            requestAddedCondition.signal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Node getTaskNode() {
        return this;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public Integer getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void run() {
        processingThreadRunning.set(true);
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task started");
            try {
                while (!stopProcessing.get())
                    processRequest();
            } catch (InterruptedException e)
            {
                if (isLogLevelEnabled(LogLevel.WARN))
                    warn("Manager task was interrupted");
                Thread.currentThread().interrupt();
            }
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task stoped");
        } finally {
            clearQueue();
            processingThreadRunning.set(false);
        }
    }
    
    void processRequest() throws InterruptedException
    {
        if (lock.writeLock().tryLock(100, TimeUnit.MILLISECONDS)) 
            try {
                requestAddedCondition.await(1, TimeUnit.SECONDS);
                boolean leaveInQueue = false;
                try {
                    CallQueueRequestWrapper request = queue.peek();
                    if (request!=null){
                        CallsQueuePrioritySelector selector = searchForPrioritySelector(request);
                        if (selector==null) {
                            if (isLogLevelEnabled(LogLevel.WARN))
                                getLogger().warn(
                                        "Rejecting request ({}). Not found priority selector"
                                        , request);
                            request.addToLog("not found priority selector");
                            request.fireRejectedQueueEvent();
                        } else {
                            //looking up for operator that will process the request
                            List<CallsQueueOperatorRef> operatorRefs = selector.getOperatorsRefs();
                            boolean processed = false;
                            for (CallsQueueOperatorRef operatorRef: operatorRefs)
                                if (operatorRef.getOperator().processRequest(this, request)) {
                                    processed = true;
                                    break;
                                }
                            if (!processed) {
                                //if no operators found then processing onBusyBehaviour
                                CallsQueueOnBusyBehaviour onBusyBehaviour = 
                                        request.getOnBusyBehaviour();
                                if (onBusyBehaviour==null){
                                    onBusyBehaviour = selector.getOnBusyBehaviour();
                                    request.setOnBusyBehaviour(onBusyBehaviour);
                                }
                                leaveInQueue = onBusyBehaviour.handleBehaviour(this, request);
                            }
                        }
                    }
                }finally{
                    if (!leaveInQueue && queue.peek()!=null) {
                        queue.pop();
                        fireQueueNumberChangedEvents();
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
    }
    
    private CallsQueuePrioritySelector searchForPrioritySelector(CallQueueRequestWrapper request)
    {
        List<CallsQueuePrioritySelector> selectors = NodeUtils.getChildsOfType(
                this, CallsQueuePrioritySelector.class);
        
        if (selectors.isEmpty())
            return null;
        
        Collections.sort(selectors, new CallsQueuePrioritySelectorComparator());
        for (CallsQueuePrioritySelector selector: selectors)
            if (selector.getPriority()<=request.getPriority())
                return selector;
        return null;
    }
    
    private void clearQueue()
    {
        statusMessage.set("Stoping processing requests. Clearing queue...");
        for (CallQueueRequestWrapper req: queue) {
            req.addToLog("queue stopped");
            req.fireRejectedQueueEvent();
        }
        queue=null;
    }

    private void fireQueueNumberChangedEvents() 
    {
        int lastElement = Math.min(maxQueueSize, queue.size());
        int pos = 1;
        for (CallQueueRequestWrapper req: queue){
            if (pos>lastElement)
                break;
            req.setPositionInQueue(pos);
            pos++;
        }
    }
}
