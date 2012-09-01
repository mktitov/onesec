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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ManagedTask;
import org.raven.sched.TaskRestartPolicy;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueuesContainerNode.class)
public class CallsQueueNode extends BaseNode implements CallsQueue, ManagedTask, Viewable
{
    @NotNull @Parameter(defaultValue="10")
    private Integer maxQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @Message private static String queueBusyMessage;
    @Message private static String numberInQueueMessage;
    @Message private static String priorityMessage;
    @Message private static String requestIdMessage;
    @Message private static String queueTimeMessage;
    @Message private static String targetQueueMessage;
    @Message private static String nextOnBusyBehaviourStepMessage;
    @Message private static String lastOperatorIndexMessage;
    @Message private static String requestMessage;

    private AtomicReference<String> statusMessage;
    private AtomicBoolean stopProcessing;
    AtomicBoolean processingThreadRunning;
    LinkedList<CallQueueRequestController> queue;
    private ReadWriteLock lock;
    private Condition requestAddedCondition;
    private CallsQueueRequestComparator requestComparator;

    @Override
    protected void initFields() {
        super.initFields();
        statusMessage = new AtomicReference<String>("Waiting for request...");
        stopProcessing = new AtomicBoolean(true);
        processingThreadRunning = new AtomicBoolean(false);
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
        executor.execute(this);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        stopProcessing.set(true);
        while(processingThreadRunning.get())
            TimeUnit.MILLISECONDS.sleep(100);
    }

    public TaskRestartPolicy getTaskRestartPolicy() {
        return TaskRestartPolicy.RESTART_NODE;
    }
    
    public void queueCall(CallQueueRequestController request) 
    {
        if (!Status.STARTED.equals(getStatus()) || stopProcessing.get()) {
            request.addToLog(String.format("Queue (%s) not ready", getName()));
            request.fireRejectedQueueEvent();
        }
        
        CallsQueue oldQueue = request.getCallsQueue();
        request.setCallsQueue(this);
        if (isLogLevelEnabled(LogLevel.DEBUG)) 
            getLogger().debug(logMess(request, "Request %s to the queue", this==oldQueue?"returned":"added"));
        
//        if (request.getRequestId()==0)
//            request.setRequestId(requestIdSeq.getAndIncrement());
        addRequestToQueue(request);
    }
    
    private void addRequestToQueue(CallQueueRequestController request)
    {
        lock.writeLock().lock();
        try {
            queue.offer(request);
            if (queue.size()>1)
                Collections.sort(queue, requestComparator);
            if (queue.size()>maxQueueSize){
                CallQueueRequestController rejReq = queue.removeLast();
                executor.executeQuietly(new RejectTask(this, rejReq, "queue size was exceeded"));
            }else {
                request.fireCallQueuedEvent();
                fireQueueNumberChangedEvents();
            }
            requestAddedCondition.signal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Collection<CallQueueRequestController> getRequests() {
        try {
            if (lock.readLock().tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    return new ArrayList<CallQueueRequestController>(queue);
                } finally {
                    lock.readLock().unlock();
                }
            }
        } catch (InterruptedException e) { }
        return Collections.EMPTY_LIST;
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(1);
        if (lock.readLock().tryLock(500, TimeUnit.MILLISECONDS)) {
            try {
                TableImpl tab = new TableImpl(new String[]{numberInQueueMessage, requestIdMessage,
                        priorityMessage, queueTimeMessage, targetQueueMessage, 
                        nextOnBusyBehaviourStepMessage, lastOperatorIndexMessage, requestMessage});
                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                int numInQueue = 1;
                for (CallQueueRequestController req: queue) {
                    tab.addRow(new Object[]{numInQueue++, req.getRequestId(), req.getPriority()
                            , fmt.format(new Date(req.getLastQueuedTime()))
                            , req.getTargetQueue().getName()
                            , req.getOnBusyBehaviourStep(), req.getOperatorIndex(), req.toString()
                    });
                }
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, tab));
            } finally {
                lock.readLock().unlock();
            }
        } else 
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, queueBusyMessage));
        return vos;
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
                while (!stopProcessing.get()) {
                    statusMessage.set("Waiting for request...");
                    processRequest();
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException e) {
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
    
    void processRequest() throws InterruptedException {
        if (lock.writeLock().tryLock(100, TimeUnit.MILLISECONDS)) 
            try {
                requestAddedCondition.await(1, TimeUnit.SECONDS);
                boolean leaveInQueue = false;
                try {
                    CallQueueRequestController request = queue.peek();
                    if (request!=null && request.isValid()){
                        statusMessage.set(request.logMess("Processing request..."));
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            getLogger().debug(logMess(request, "Processing request"));
                        CallsQueuePrioritySelector selector = searchForPrioritySelector(request);
                        if (selector==null) {
                            if (isLogLevelEnabled(LogLevel.WARN))
                                getLogger().warn(logMess(
                                        request
                                        , "Rejecting request. Not found priority selector"));
                            executor.executeQuietly(new RejectTask(this, request, "not found priority selector"));
                        } else {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                getLogger().debug(logMess(
                                        request, "Request mapped to the (%s) priority selector"
                                        , selector.getName()));
                            //looking up for operator that will process the request
                            List<CallsQueueOperatorRef> operatorRefs = selector.getOperatorsRefs();
                            boolean processed = false;
                            int startIndex = selector.getStartIndex(request, operatorRefs.size());
                            if (startIndex<operatorRefs.size())
                                for (int i=startIndex; i<operatorRefs.size(); ++i){
                                    request.incOperatorHops();
                                    if (operatorRefs.get(i).processRequest(this, request)) {
                                        request.setOperatorIndex(i);
                                        processed = true;
                                        selector.rebuildIndex(operatorRefs, i);
                                        break;
                                    }
                                }
                            if (!processed) {
                                //if no operators found then processing onBusyBehaviour
                                if (isLogLevelEnabled(LogLevel.DEBUG))
                                    getLogger().debug(logMess(
                                            request, "Not found free operator to process request"));
                                CallsQueueOnBusyBehaviour onBusyBehaviour = request.getOnBusyBehaviour();
                                if (onBusyBehaviour==null){
                                    onBusyBehaviour = selector.getOnBusyBehaviour();
                                    request.setOnBusyBehaviour(onBusyBehaviour);
                                }
                                leaveInQueue = onBusyBehaviour.handleBehaviour(this, request);
                            }
                        }
                    }
                } finally {
                    if (!leaveInQueue && queue.peek()!=null) {
                        queue.pop();
                        fireQueueNumberChangedEvents();
                    } 
                }
            } finally {
                lock.writeLock().unlock();
            }
    }
    
    private CallsQueuePrioritySelector searchForPrioritySelector(CallQueueRequestController request)
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
        for (CallQueueRequestController req: queue) {
            req.addToLog("queue stopped");
            req.fireRejectedQueueEvent();
        }
        queue=null;
    }

    private void fireQueueNumberChangedEvents() 
    {
        int lastElement = Math.min(maxQueueSize, queue.size());
        int pos = 1;
        for (CallQueueRequestController req: queue){
            if (pos>lastElement)
                break;
            req.setPositionInQueue(pos);
            pos++;
        }
    }

    private String logMess(CallQueueRequestController req, String mess, Object... args)
    {
        return req.logMess("CallsQueue. "+mess, args);
    }
    
    private class RejectTask extends AbstractTask {
        private final String log;
        private final CallQueueRequestController req;

        public RejectTask(Node taskNode, CallQueueRequestController req, String log) {
            super(taskNode, "Rejecting request");
            this.req = req;
            this.log = log;
        }

        @Override
        public void doRun() throws Exception {
            req.addToLog(log);
            req.fireRejectedQueueEvent();
        }
    }
}
