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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointException;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueOperatorNode extends BaseNode 
        implements CallsQueueOperator, EndpointRequest, ConversationCompletionCallback
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
    
    private AtomicReference<RequestInfo> request;
    private AtomicReference<String> statusMessage;
    private AtomicBoolean busy;
    private boolean commutationValid;
    private ReentrantLock lock;
    private Condition eventCondition;

    @Override
    protected void initFields() {
        super.initFields();
        request = new AtomicReference<RequestInfo>();
        busy = new AtomicBoolean(false);
        statusMessage.set(null);
        lock = new ReentrantLock();
        eventCondition = lock.newCondition();
    }
    
    public long getProcessedRequestCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //CallQueueOpertor's method
    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request) 
    {
        if (!Status.STARTED.equals(getStatus()) || !busy.compareAndSet(false, true))
            return false;
        String[] numbers = RavenUtils.split(phoneNumbers, ",");
        this.request.set(new RequestInfo(
                request, queue, endpointWaitTimeout, inviteTimeout, conversationScenario, numbers));
        statusMessage.set(String.format(SEARCHING_FOR_ENDPOINT_MSG, request.toString()));
        request.addToLog(String.format("handling by operator (%s)", getName()));
        commutationValid = true;
        try {
            endpointPool.requestEndpoint(this);
            return true;
        } catch (ExecutorServiceException ex) {
            request.addToLog("get endpoint from pool error");
            busy.set(false);
            return false;
        }
    }


    //EndpointPool request methods
    public void processRequest(IvrEndpoint endpoint)
    {
        RequestInfo info = request.get();

        if (endpoint==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Can't process call queue request because of no free endpoints "
                        + "in the pool", endpointPool.getName());
            info.request.addToLog("no free endpoints in the pool");
            info.queue.queueCall(info.request);
            return;
        }

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(CALL_QUEUE_OPERATOR_BINDING, this);
        bindings.put(CALL_QUEUE_REQUEST_BINDING, info.request);
        try {
            try {
                boolean callHandled = false;
                for (int i=0; i<info.numbers.length; ++i) {
                    info.numberIndex=i;
                    if (callToOperator(endpoint, bindings)) {
                        callHandled = true;
                        break;
                    }
                }
                if (!callHandled) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        getLogger().debug("Operator ({}) didn't handle the call", getName());
                    info.request.addToLog(String.format("operator (%s) didn't handle a call", getName()));
                    info.queue.queueCall(info.request);
                }
            } catch(Exception e){
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error handling by operator", e);
                info.request.addToLog("error handling by operator");
                info.queue.queueCall(info.request);
            }
            freeResources();
        } finally {
            busy.set(false);
        }
    }

    /**
     * Returns <b>true</b> if there were success commutation or if abonent hung up
     * @throws Exception
     */
    private boolean callToOperator(IvrEndpoint endpoint, Map<String, Object> bindings)
        throws Exception
    {
        RequestInfo info = request.get();
        info.operatorConversationFlag = true;
        endpoint.invite(phoneNumbers, info.conversationScenario, this, bindings);
        lock.lock();
        try {
            long callStartTime = System.currentTimeMillis();
            while (checkState()){
                eventCondition.await(500, TimeUnit.MILLISECONDS);
                if (!checkState())
                    break;
                if (!checkInviteTimeout(callStartTime, info, endpoint, info.getNumber()))
                    return false;
                if (info.operatorReadyToCommutate && !info.readyToCommutateSended){
                    info.request.fireReadyToCommutateQueueEvent(this);
                    info.readyToCommutateSended=true;
                }
                if (info.abonentReadyToCommutate && !info.commutated)
                    commutateCalls(info);
            }

            if (info.commutated) {
                info.request.fireDisconnectedQueueEvent();
                return false;
            } else
                return !info.request.isValid();
        } finally {
            lock.unlock();
        }
    }

    private void commutateCalls(RequestInfo info)
    {
        info.commutated = true;
        
    }

    private void freeResources()
    {
        
    }

    private boolean checkInviteTimeout(long callStartTime, RequestInfo info, IvrEndpoint endpoint
            , String operatorNumber) throws Exception
    {
        if (endpoint.getEndpointState().getId()==IvrEndpointState.INVITING
            && callStartTime+info.inviteTimeout*1000>System.currentTimeMillis())
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Operator number ({}) not answered", operatorNumber);
            info.request.addToLog(String.format("number (%s) not answer", operatorNumber));
            //restarting endpoint
            endpoint.stop();
            TimeUnit.SECONDS.sleep(1);
            endpoint.start();
            StateWaitResult res = endpoint.getEndpointState().waitForState(
                    new int[]{IvrEndpointState.IN_SERVICE}, 10000);
            if (res.isWaitInterrupted())
                throw new Exception("Wait for IN_SERVICE timeout");
            return false;
        }
        return true;
    }

    private boolean checkState()
    {
        if (commutationValid){
            RequestInfo info = request.get();
            commutationValid = info.request.isValid() && info.operatorConversationFlag;
        }
        return commutationValid;
    }

    public long getWaitTimeout() {
        return request.get().waitTimeout;
    }

    public Node getOwner() {
        return this;
    }

    public int getPriority() {
        return request.get().request.getPriority();
    }

    //Endpoint ConverstionCompleteCallback
    public void conversationCompleted(ConversationResult res)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Operator's conversation completed");
        lock.lock();
        try {
            RequestInfo info = request.get();
            info.operatorConversationFlag = false;
            checkState();
            info.request.addToLog(String.format(
                    "conv. for op. number (%s) completed (%s)", info.getNumber(), res.getCompletionCode()));
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation)
    {
        lock.lock();
        try {
            request.get().request.addToLog("abonent ready to commutate");
            request.get().abonentReadyToCommutate = true;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation) {
        lock.lock();
        try {
            String number = request.get().getNumber();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Operator ({}, {}) ready to commutate", getName(), number);
            request.get().operatorReadyToCommutate=true;
            request.get().request.addToLog(String.format("op. number (%s) ready to commutate", number));
            request.get().operatorConversation = operatorConversation;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }
    public CallQueueRequestWrapper getProcessingRequest()
    {
        RequestInfo info = request.get();
        return info==null? null : info.request;
    }

    public String getStatusMessage() {
        return statusMessage.get();
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
    
    private class RequestInfo {
        final CallQueueRequestWrapper request;
        final CallsQueue queue;
        final long waitTimeout;
        final int inviteTimeout;
        final IvrConversationScenario conversationScenario;
        final String[] numbers;
        int numberIndex=0;
        boolean operatorConversationFlag = false;
        boolean operatorReadyToCommutate = false;
        boolean readyToCommutateSended = false;
        boolean abonentReadyToCommutate = false;
        boolean commutated = false;
        IvrEndpointConversation operatorConversation = null;

        public RequestInfo(CallQueueRequestWrapper request, CallsQueue queue, long waitTimeout
                , int inviteTimeout, IvrConversationScenario conversationScenario, String[] numbers)
        {
            this.request = request;
            this.queue = queue;
            this.waitTimeout = waitTimeout;
            this.inviteTimeout = inviteTimeout;
            this.conversationScenario = conversationScenario;
            this.numbers = numbers;
        }

        public String getNumber(){
            return numbers[numberIndex];
        }
    }
}
