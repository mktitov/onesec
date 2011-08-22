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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
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
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationsBridgeManager conversationsBridgeManager;
    
    private AtomicReference<CallsCommutationManagerImpl> commutationManager;
    private AtomicBoolean busy;
    private AtomicLong processedRequestCount;

    @Override
    protected void initFields()
    {
        super.initFields();
        commutationManager = new AtomicReference<CallsCommutationManagerImpl>();
        busy = new AtomicBoolean(false);
        processedRequestCount = new AtomicLong();
    }
    
    public long getProcessedRequestCount() {
        return processedRequestCount.get();
    }

    /**
     * for test purposes
     */
    CallsCommutationManagerImpl getCommutationManager(){
        return commutationManager.get();
    }

    //CallQueueOpertor's method
    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request) 
    {
        if (!Status.STARTED.equals(getStatus()) || !busy.compareAndSet(false, true))
            return false;
        String[] numbers = RavenUtils.split(phoneNumbers, ",");
        this.commutationManager.set(new CallsCommutationManagerImpl(
                request, queue, endpointWaitTimeout, inviteTimeout, conversationScenario, numbers
                , conversationsBridgeManager, this));
        request.addToLog(String.format("handling by operator (%s)", getName()));
        try {
            endpointPool.requestEndpoint(commutationManager.get());
            return true;
        } catch (ExecutorServiceException ex) {
            request.addToLog("get endpoint from pool error");
            busy.set(false);
            return false;
        }
    }

    void requestProcessed(CallsCommutationManagerImpl commutationManager)
    {
        if (this.commutationManager.compareAndSet(commutationManager, null)) {
            processedRequestCount.incrementAndGet();
            busy.set(false);
        }
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

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
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
}
