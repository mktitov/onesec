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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueOperatorNode extends BaseNode 
        implements CallsQueueOperator
{
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;
    
    @NotNull @Parameter
    private String phoneNumbers;

    @NotNull @Parameter(defaultValue="30")
    private Integer inviteTimeout;

    @NotNull @Parameter(defaultValue="5000")
    private Long endpointWaitTimeout;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationsBridgeManager;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFile greeting;
    
    private AtomicReference<CallsCommutationManagerImpl> commutationManager;
    private AtomicBoolean busy;

    private AtomicInteger totalRequests;
    private AtomicInteger handledRequests;
    private AtomicInteger onBusyRequests;
    private AtomicInteger onNoFreeEndpointRequests;
    private AtomicInteger onNoAnswerRequests;
    private AtomicInteger onNotStartedRequests;

    @Override
    protected void initFields()
    {
        super.initFields();
        commutationManager = new AtomicReference<CallsCommutationManagerImpl>();
        busy = new AtomicBoolean(false);
        totalRequests = new AtomicInteger();
        handledRequests = new AtomicInteger();
        onBusyRequests = new AtomicInteger();
        onNoFreeEndpointRequests = new AtomicInteger();
        onNoAnswerRequests = new AtomicInteger();
        onNotStartedRequests = new AtomicInteger();
    }
    
    /**
     * for test purposes
     */
    CallsCommutationManagerImpl getCommutationManager(){
        return commutationManager.get();
    }

    //CallQueueOpertor's method
    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request
            , IvrConversationScenario conversationScenario, AudioFile greeting)
    {
        totalRequests.incrementAndGet();
        if (!Status.STARTED.equals(getStatus())) {
            onNotStartedRequests.incrementAndGet();
            return false;
        }
        if (!busy.compareAndSet(false, true)){
            onBusyRequests.incrementAndGet();
            return false;
        }
        request.fireOperatorQueueEvent(getName());
        request.fireOperatorGreetingQueueEvent(greeting!=null?greeting:this.greeting);        
        String[] numbers = RavenUtils.split(phoneNumbers, ",");
        this.commutationManager.set(new CallsCommutationManagerImpl(
                request, queue, endpointWaitTimeout, inviteTimeout, conversationScenario, numbers
                , conversationsBridgeManager, this));
        request.addToLog(String.format("handling by operator (%s)", getName()));
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug(commutationManager.get().logMess("Processing..."));
        try {
            endpointPool.requestEndpoint(commutationManager.get());
            return true;
        } catch (ExecutorServiceException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(commutationManager.get().logMess("Get endpoint from pool error"), ex);
            request.addToLog("get endpoint from pool error");
            onNoFreeEndpointRequests.incrementAndGet();
            busy.set(false);
            return false;
        }
    }

    void requestProcessed(CallsCommutationManagerImpl commutationManager, boolean callHandled)
    {
        if (this.commutationManager.compareAndSet(commutationManager, null)) {
            if (callHandled)
                handledRequests.incrementAndGet();
            else
                onNoAnswerRequests.incrementAndGet();
            busy.set(false);
        }
    }

    public int getHandledRequests() {
        return handledRequests.get();
    }

    public int getOnBusyRequests() {
        return onBusyRequests.get();
    }

    public int getOnNoAnswerRequests() {
        return onNoAnswerRequests.get();
    }

    public int getOnNoFreeEndpointsRequests() {
        return onNoFreeEndpointRequests.get();
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getOnNotStartedRequests() {
        return onNotStartedRequests.get();
    }

    public CallQueueRequestWrapper getProcessingRequest()
    {
        CallsCommutationManagerImpl manager = commutationManager.get();
        return manager==null? null : manager.getRequest();
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

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public AudioFile getGreeting() {
        return greeting;
    }

    public void setGreeting(AudioFile greeting) {
        this.greeting = greeting;
    }
}
