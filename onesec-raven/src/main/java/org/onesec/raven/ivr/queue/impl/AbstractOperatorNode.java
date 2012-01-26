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

import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.RavenUtils;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractOperatorNode extends BaseNode implements CallsQueueOperator {
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";
    public final static String TOTAL_REQUESTS_ATTR = "totalRequests";
    public final static String HANDLED_REQUESTS_ATTR = "handledRequests";
    public final static String ON_BUSY_REQUESTS_ATTR = "onBusyRequests";
    public final static String ON_NO_FREE_ENDPOINTS_REQUESTS_ATTR = "onNoFreeEndpointsRequests";
    public final static String ON_NO_ANSWER_REQUESTS = "onNoAnswerRequests";
    public final static String ON_NOT_STARTED_REQUESTS = "onNotStartedRequests";
    public final static String PROCESSING_REQUEST_COUNT = "processingRequestCount";
    public final static String ACTIVE_ATTR = "active";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @NotNull @Parameter(defaultValue="30")
    private Integer inviteTimeout;

    @NotNull @Parameter(defaultValue="5000")
    private Long endpointWaitTimeout;

    @Parameter
    private Integer parallelCallAfter;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationsBridgeManager;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFile greeting;

    @NotNull @Parameter(defaultValue="true")
    private Boolean active;


    protected AtomicInteger totalRequests;
    protected AtomicInteger handledRequests;
    protected AtomicInteger onBusyRequests;
    protected AtomicInteger onNoFreeEndpointsRequests;
    protected AtomicInteger onNoAnswerRequests;
    protected AtomicInteger onNotStartedRequests;
    protected AtomicInteger processingRequestCount;

    @Override
    protected void initFields() {
        super.initFields();
        totalRequests = new AtomicInteger();
        handledRequests = new AtomicInteger();
        onBusyRequests = new AtomicInteger();
        onNoFreeEndpointsRequests = new AtomicInteger();
        onNoAnswerRequests = new AtomicInteger();
        onNotStartedRequests = new AtomicInteger();
        processingRequestCount = new AtomicInteger();
    }
    
    //CallQueueOpertor's method
    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers)
    {
        totalRequests.incrementAndGet();
        if (!Status.STARTED.equals(getStatus()) || !active) {
            onNotStartedRequests.incrementAndGet();
            return false;
        }
        boolean res =  doProcessRequest(queue, request, conversationScenario, greeting
                , operatorPhoneNumbers);
        if (res)
            processingRequestCount.incrementAndGet();
        return res;
    }

    void incOnNoFreeEndpointsRequests(){
        onNoFreeEndpointsRequests.incrementAndGet();
    }

    protected abstract boolean doProcessRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers);

    protected CallsCommutationManagerImpl commutate(CallsQueue queue, CallQueueRequestWrapper request
            , String phoneNumbers, IvrConversationScenario conversationScenario, AudioFile greeting)
    {
        request.fireOperatorQueueEvent(getName());
        request.fireOperatorGreetingQueueEvent(greeting!=null? greeting:this.greeting);
        String[] numbers = RavenUtils.split(phoneNumbers, ",");
        CallsCommutationManagerImpl manager = new CallsCommutationManagerImpl(executor, request, inviteTimeout
                , parallelCallAfter, queue, endpointWaitTimeout, numbers, conversationScenario
                , conversationsBridgeManager, endpointPool, this);
        request.addToLog(String.format("handling by operator (%s)", getName()));
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug(request.logMess("Operator (%s). Processing", getName()));
        manager.commutate();
        return manager;
    }

    void requestProcessed(CallsCommutationManagerImpl commutationManager, boolean callHandled) {
        if (callHandled)
            handledRequests.incrementAndGet();
        else
            onNoAnswerRequests.incrementAndGet();
        processingRequestCount.decrementAndGet();
        doRequestProcessed(commutationManager, callHandled);
    }

    protected abstract void doRequestProcessed(CallsCommutationManagerImpl commutationManager, boolean callHandled);

    @Parameter(readOnly=true)
    public int getHandledRequests() {
        return handledRequests.get();
    }

    @Parameter(readOnly=true)
    public int getOnBusyRequests() {
        return onBusyRequests.get();
    }

    @Parameter(readOnly=true)
    public int getOnNoAnswerRequests() {
        return onNoAnswerRequests.get();
    }

    @Parameter(readOnly=true)
    public int getOnNoFreeEndpointsRequests() {
        return onNoFreeEndpointsRequests.get();
    }

    @Parameter(readOnly=true)
    public int getTotalRequests() {
        return totalRequests.get();
    }

    @Parameter(readOnly=true)
    public int getOnNotStartedRequests() {
        return onNotStartedRequests.get();
    }

    @Parameter(readOnly=true)
    public int getProcessingRequestCount() {
        return processingRequestCount.get();
    }

    public IvrConversationsBridgeManager getConversationsBridgeManager() {
        return conversationsBridgeManager;
    }

    public void setConversationsBridgeManager(IvrConversationsBridgeManager conversationsBridgeManager) {
        this.conversationsBridgeManager = conversationsBridgeManager;
    }

    public IvrEndpointPool getEndpointPool() {
        return endpointPool;
    }

    public void setEndpointPool(IvrEndpointPool endpointPool) {
        this.endpointPool = endpointPool;
    }

    public Long getEndpointWaitTimeout() {
        return endpointWaitTimeout;
    }

    public void setEndpointWaitTimeout(Long endpointWaitTimeout) {
        this.endpointWaitTimeout = endpointWaitTimeout;
    }

    public Integer getInviteTimeout() {
        return inviteTimeout;
    }

    public void setInviteTimeout(Integer inviteTimeout) {
        this.inviteTimeout = inviteTimeout;
    }

    public AudioFile getGreeting() {
        return greeting;
    }

    public void setGreeting(AudioFile greeting) {
        this.greeting = greeting;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Integer getParallelCallAfter() {
        return parallelCallAfter;
    }

    public void setParallelCallAfter(Integer parallelCallAfter) {
        this.parallelCallAfter = parallelCallAfter;
    }
}
