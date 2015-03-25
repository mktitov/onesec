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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviour;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.onesec.raven.ivr.queue.CallsQueuePrioritySelector;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueDataProcessor extends AbstractDataProcessorLogic {
    private final static CallsQueueRequestComparator requestComparator = new CallsQueueRequestComparator();
    private final LinkedList<CallQueueRequestController> queue = new LinkedList<>();
    private final int maxQueueSize;
    private CallsQueueNode callsQueue;

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
        if (message instanceof CallQueueRequestController)
            addRequestToQueue((CallQueueRequestController)message);
        return VOID;
    }

    private void addRequestToQueue(CallQueueRequestController request) {
        CallsQueue oldQueue = request.getCallsQueue();
        request.setCallsQueue(callsQueue);
        if (getLogger().isDebugEnabled()) 
            getLogger().debug("Request {} to the queue", this==oldQueue?"returned":"added");       
        
        queue.offer(request);
        if (queue.size() > 1) {
            Collections.sort(queue, requestComparator);
        }
        if (queue.size() > maxQueueSize) {
            CallQueueRequestController rejReq = queue.removeLast();
            sendReject(rejReq, "queue size was exceeded");
        } else {
            request.fireCallQueuedEvent();
            fireQueueNumberChangedEvents();
        }       
    }
    
    private void processQueue() {
        CallQueueRequestController request = queue.peek();
        if (request!=null) {
            if (!request.isValid() || !processRequest(request)) {
                queue.poll();
                fireQueueNumberChangedEvents();
                //нужно обработать следующий запрос
            } else {
                //запрос остался в очереди пытаемся его обработать через некоторое время
            }
        } 
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
            //looking up for operator that will process the request
            List<CallsQueueOperatorRef> operatorRefs = selector.getOperatorsRefs();
            boolean processed = false;
            int startIndex = selector.getStartIndex(request, operatorRefs.size());
            if (startIndex<operatorRefs.size())
                for (int i=startIndex; i<operatorRefs.size(); ++i){
                    request.incOperatorHops();
                    if (operatorRefs.get(i).processRequest(callsQueue, request)) {
                        request.setOperatorIndex(i);
                        processed = true;
                        selector.rebuildIndex(operatorRefs, i);
                        break;
                    }
                }
            if (!processed) {
                //if no operators found then processing onBusyBehaviour
                if (getLogger().isDebugEnabled())
                    getLogger().debug(request.logMess("Not found free operator to process request"));
                CallsQueueOnBusyBehaviour onBusyBehaviour = request.getOnBusyBehaviour();
                if (onBusyBehaviour==null){
                    onBusyBehaviour = selector.getOnBusyBehaviour();
                    request.setOnBusyBehaviour(onBusyBehaviour);
                }
                leaveInQueue = onBusyBehaviour.handleBehaviour(callsQueue, request);
            }
        }        
        return leaveInQueue;
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
