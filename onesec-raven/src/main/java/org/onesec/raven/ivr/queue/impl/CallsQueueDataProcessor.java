/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.ivr.queue.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueDataProcessor extends AbstractDataProcessorLogic {
    public final static String GET_REQUESTS = "GET_REQUESTS";
    
//    private final static long MIN_REQUEST_REPROCESS_INTERVAL = 1000; //минимальный интервал повторной попытки обработки запроса очередью
    public final static long TICK_INTERVAL = 100;
    private final static String PROCESS_REQUEST = "PROCESS_REQUEST";
    private final static CallsQueueRequestComparator requestComparator = new CallsQueueRequestComparator();
    private final LinkedList<CallQueueRequestController> queue = new LinkedList<>();
    private final int maxQueueSize;
    private CallsQueueNode callsQueue;
    private boolean wating = false;

    public CallsQueueDataProcessor(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    @Override
    public void postInit() {
        if (getLogger().isDebugEnabled())
            getLogger().debug("STARTED");
        callsQueue = (CallsQueueNode) getContext().getOwner();
    }

    @Override
    public Object processData(Object message) throws Exception {
        if (getLogger().isDebugEnabled())
            getLogger().debug("Processing message: "+message);
        if (message instanceof CallQueueRequestController) {
            addRequestToQueue((CallQueueRequestController)message);
            if (!wating) 
                processQueue();
        } else if (PROCESS_REQUEST==message) {
            processQueue();
        } else if (GET_REQUESTS==message) {
            return queue.isEmpty()? Collections.EMPTY_LIST : new ArrayList<>(queue);
        } else 
            return UNHANDLED;
        return VOID;
    }

    private void addRequestToQueue(CallQueueRequestController request) throws Exception {
        CallsQueue oldQueue = request.getCallsQueue();
        request.setCallsQueue(callsQueue);
        if (getLogger().isDebugEnabled()) 
            getLogger().debug(request.logMess("Request %s to the queue", this==oldQueue?"returned":"added"));       
        
        queue.offer(request);
        if (queue.size() > 1) {
            Collections.sort(queue, requestComparator);
        }
        if (queue.size() > maxQueueSize) {
            CallQueueRequestController rejReq = queue.removeLast();
            if (getLogger().isDebugEnabled())
                getLogger().debug(rejReq.logMess("Rejectected. Queue size was exceeded"));
            sendReject(rejReq, "queue size was exceeded");
        } else {
            request.fireCallQueuedEvent();
            fireQueueNumberChangedEvents();
        }
    }
    
    private void processQueue() throws Exception {        
        wating = false;
        if (getLogger().isDebugEnabled())
            getLogger().debug("Processing requests from queue. Queue size: "+queue.size());
        CallQueueRequestController request;
        while ( (request=queue.peek())!=null ) {
            final boolean valid = request.isValid();
            if (!valid && getLogger().isDebugEnabled())
                getLogger().debug(request.logMess("Removed. Not valid request"));
            if (!valid || !processRequest(request)) {
                //если запрос не валидный или обработался успешно удаляем его из очереди
                queue.poll();
                fireQueueNumberChangedEvents();
            } else {
                //запрос остался в очереди пытаемся его обработать через некоторое время
                sendTickEvent();
                return;
            }
        } 
    }
    
    private void sendTickEvent() throws ExecutorServiceException {
        wating = true;
        getFacade().sendDelayed(TICK_INTERVAL, PROCESS_REQUEST);
    }
    
    private boolean processRequest(CallQueueRequestController request) {
        boolean leaveInQueue = false;
        if (getLogger().isDebugEnabled())
            getLogger().debug(request.logMess("Processing request"));
        CallsQueuePrioritySelector selector = searchForPrioritySelector(request);
        if (selector==null) {
            if (getLogger().isWarnEnabled())
                getLogger().warn(request.logMess("Rejecting request. Not found priority selector"));
            sendReject(request, "not found priority selector");
        } else {
            if (getLogger().isDebugEnabled())
                getLogger().debug(request.logMess(
                        "Request mapped to the (%s) priority selector"
                        , selector.getName()));
            if (!dispatchToOperator(request, selector)) {
                //if no operators found then processing onBusyBehaviour
                if (getLogger().isDebugEnabled())
                    getLogger().debug(request.logMess("Not found free operator to process request"));
                CallsQueueOnBusyBehaviour onBusyBehaviour = request.getOnBusyBehaviour();
                if (onBusyBehaviour==null){
                    onBusyBehaviour = selector.getOnBusyBehaviour();
                    request.setOnBusyBehaviour(onBusyBehaviour);
                }
                leaveInQueue = onBusyBehaviour.handleBehaviour(callsQueue, request);
                if (getLogger().isDebugEnabled()) {
                    if (leaveInQueue)
                        getLogger().debug(request.logMess("Leaved in queue by busy behaviour"));
                    else 
                        getLogger().debug(request.logMess("Rejected from the queue by busy behaviour"));
                }
            }
        }        
        return leaveInQueue;
    }
    
    private boolean dispatchToOperator(CallQueueRequestController request, CallsQueuePrioritySelector selector) {
        List<CallsQueueOperatorRef> operatorRefs = selector.getOperatorsRefs();
        int startIndex = selector.getStartIndex(request, operatorRefs.size());
        if (startIndex<operatorRefs.size())
            for (int i=startIndex; i<operatorRefs.size(); ++i){
                request.incOperatorHops();
                if (operatorRefs.get(i).processRequest(callsQueue, request)) {
                    request.setOperatorIndex(i);
                    selector.rebuildIndex(operatorRefs, i);
                    if (getLogger().isDebugEnabled())
                        getLogger().debug(request.logMess("Assigned to operator references: "+operatorRefs.get(i)));
                    return true;
                }
            }
        return false;
    } 
    
    private CallsQueuePrioritySelector searchForPrioritySelector(CallQueueRequestController request)
    {
        List<CallsQueuePrioritySelector> selectors = NodeUtils.getChildsOfType(
                callsQueue, CallsQueuePrioritySelector.class);
        
        if (selectors.isEmpty())
            return null;
        
        Collections.sort(selectors, new CallsQueuePrioritySelectorComparator());
        for (CallsQueuePrioritySelector selector: selectors)
            if (selector.getPriority()<=request.getPriority())
                return selector;
        return null;
    }
    
    private void sendReject(CallQueueRequestController req, String message) {        
        getContext().getExecutor().executeQuietly(new RejectTask(getContext().getOwner(), req, message));
    }
    
    private void fireQueueNumberChangedEvents() 
    {
        int lastElement = Math.min(maxQueueSize, queue.size());
        int pos = 1;
        for (CallQueueRequestController req: queue) {
            if (pos>lastElement)
                break;
            req.setPositionInQueue(pos);
            pos++;
        }
    }
    
    private static class RejectTask extends AbstractTask {
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
