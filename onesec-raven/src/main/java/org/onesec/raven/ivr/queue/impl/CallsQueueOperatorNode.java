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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueOperatorNode extends BaseNode implements CallsQueueOperator, Task
{
    public final static String PROCESSING_FOR_REQUEST_MSG = "Processing call queue request (%s)";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter
    private String phoneNumbers;
    
    private AtomicReference<RequestInfo> request;
    private AtomicReference<String> statusMessage;
    private AtomicBoolean busy;

    @Override
    protected void initFields() {
        super.initFields();
        request = new AtomicReference<RequestInfo>();
        busy = new AtomicBoolean(false);
        statusMessage.set(null);
    }
    
    public long getProcessedRequestCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request) 
    {
        if (!Status.STARTED.equals(getStatus()) || !busy.compareAndSet(false, true))
            return false;
        this.request.set(new RequestInfo(request, queue));
        statusMessage.set(String.format(PROCESSING_FOR_REQUEST_MSG, request.toString()));
        try {
            executor.execute(this);
        } catch (ExecutorServiceException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(
                        String.format("Error on start processing call queue request (%s)", request)
                        , ex);
            busy.set(false);
            return false;
        }
        return true;
    }

    public CallQueueRequestWrapper getProcessingRequest() 
    {
        RequestInfo info = request.get();
        return info==null? null : info.request;
    }

    public Node getTaskNode() {
        return this;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void run() {
        String[] numbers = RavenUtils.split(phoneNumbers);
        
    }
    
    private class RequestInfo {
        final CallQueueRequestWrapper request;
        final CallsQueue queue;

        public RequestInfo(CallQueueRequestWrapper request, CallsQueue queue) {
            this.request = request;
            this.queue = queue;
        }
    }

    private class RequestHandler implements EndpointRequest {
        private final long waitTimeout;
        private final int priority;

        public RequestHandler(long waitTimeout, int priority) {
            this.waitTimeout = waitTimeout;
            this.priority = priority;
        }

        public void processRequest(IvrEndpoint endpoint) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getWaitTimeout() {
            return waitTimeout;
        }

        public Node getOwner() {
            return CallsQueueOperatorNode.this;
        }

        public String getStatusMessage() {
            return statusMessage.get();
        }

        public int getPriority() {
            return priority;
        }
    }
}
