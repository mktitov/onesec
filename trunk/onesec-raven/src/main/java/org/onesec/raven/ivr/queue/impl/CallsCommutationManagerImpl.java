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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrConversationBridgeExeption;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class CallsCommutationManagerImpl implements CallsCommutationManager, IvrConversationsBridgeListener
        , EndpointRequest, ConversationCompletionCallback
{
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";

    private final CallQueueRequestWrapper request;
    private final CallsQueue queue;
    private final long waitTimeout;
    private final int inviteTimeout;
    private final IvrConversationScenario conversationScenario;
    private final IvrConversationsBridgeManager bridgeManager;
    private final String[] numbers;
    private final CallsQueueOperatorNode owner;
    private final AtomicReference<String> statusMessage;
    private final List<CallsCommutationManagerListener> listeners =
            new LinkedList<CallsCommutationManagerListener>();
    private int numberIndex=0;
    private AtomicBoolean operatorConversationFlag = new AtomicBoolean(false);
    private boolean operatorReadyToCommutate = false;
    private boolean readyToCommutateSended = false;
    private boolean abonentReadyToCommutate = false;
    private boolean commutated = false;
    private AtomicBoolean commutationValid = new AtomicBoolean(true);
    private IvrEndpointConversation operatorConversation = null;
    private ReentrantLock lock = new ReentrantLock();
    private Condition eventCondition = lock.newCondition();

    public CallsCommutationManagerImpl(CallQueueRequestWrapper request, CallsQueue queue, long waitTimeout
            , int inviteTimeout, IvrConversationScenario conversationScenario, String[] numbers
            , IvrConversationsBridgeManager bridgeManager
            , CallsQueueOperatorNode owner)
    {
        this.request = request;
        this.queue = queue;
        this.waitTimeout = waitTimeout;
        this.inviteTimeout = inviteTimeout;
        this.conversationScenario = conversationScenario;
        this.numbers = numbers;
        this.bridgeManager = bridgeManager;
        this.owner = owner;
        this.statusMessage = new AtomicReference<String>(
                String.format(SEARCHING_FOR_ENDPOINT_MSG, request.toString()));
    }

    private boolean checkState()
    {
        commutationValid.compareAndSet(true, request.isValid() && operatorConversationFlag.get());
        return commutationValid.get();
    }

    public boolean isCommutationValid() {
        return commutationValid.get();
    }

    private String getNumber(){
        return numbers[numberIndex];
    }

    public CallQueueRequestWrapper getRequest() {
        return request;
    }

    public void addListener(CallsCommutationManagerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CallsCommutationManagerListener listener) {
        listeners.remove(listener);
    }

    public void processRequest(IvrEndpoint endpoint)
    {
        boolean callHandled = false;
        try{
            if (endpoint==null) {
                if (owner.isLogLevelEnabled(LogLevel.WARN))
                    owner.getLogger().warn(logMess(
                            "Can't process call queue request because of no free endpoints in the pool (%s)"
                            , owner.getEndpointPool().getName()));
                request.addToLog("no free endpoints in the pool");
                queue.queueCall(request);
                return;
            }

            Map<String, Object> bindings = new HashMap<String, Object>();
            bindings.put(CALLS_COMMUTATION_MANAGER_BINDING, this);
            bindings.put(CALL_QUEUE_REQUEST_BINDING, request);
            try {
                for (int i=0; i<numbers.length; ++i) {
                    numberIndex=i;
                    if (callToOperator(endpoint, bindings)) {
                        callHandled = true;
                        break;
                    }
                }
                if (!callHandled) {
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(logMess("Call not handled"));
                    request.addToLog(String.format("operator (%s) didn't handle a call", owner.getName()));
                    queue.queueCall(request);
                }
            } catch(Exception e){
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error handling by operator"), e);
                request.addToLog("error handling by operator");
                queue.queueCall(request);
            }
            freeResources();
        } finally {
            owner.requestProcessed(this, callHandled);
        }
    }

    public boolean callToOperator(IvrEndpoint endpoint, Map<String, Object> bindings) throws Exception
    {
        operatorConversationFlag.set(true);
        request.fireOperatorNumberQueueEvent(getNumber());
        endpoint.invite(getNumber(), conversationScenario, this, bindings);
        lock.lock();
        try {
            long callStartTime = System.currentTimeMillis();
            while (checkState()){
                eventCondition.await(500, TimeUnit.MILLISECONDS);
                if (!checkState())
                    break;
                if (!checkInviteTimeout(callStartTime, endpoint))
                    return false;
                if (operatorReadyToCommutate && !readyToCommutateSended){
                    request.fireReadyToCommutateQueueEvent(this);
                    readyToCommutateSended=true;
                }
                if (abonentReadyToCommutate && !commutated)
                    commutateCalls();
            }

            if (commutated) {
                request.fireDisconnectedQueueEvent();
                return true;
            } else
                return !request.isValid();
        } finally {
            lock.unlock();
        }
        
    }

    private boolean checkInviteTimeout(long callStartTime, IvrEndpoint endpoint) throws Exception
    {
        if (endpoint.getEndpointState().getId()==IvrEndpointState.INVITING
            && callStartTime+inviteTimeout*1000<=System.currentTimeMillis())
        {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Operator's number (%s) not answered", getNumber()));
            request.addToLog(String.format("number (%s) not answer", getNumber()));
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

    public void commutateCalls() throws IvrConversationBridgeExeption
    {
        IvrConversationsBridge bridge = bridgeManager.createBridge(
                request.getConversation(), operatorConversation, logMess(""));
        bridge.addBridgeListener(this);
        bridge.activateBridge();
        commutated = true;
    }

    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation)
    {
        lock.lock();
        try {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Number (%s) ready to commutate", getNumber()));
            operatorReadyToCommutate=true;
            request.addToLog(String.format("op. number (%s) ready to commutate", getNumber()));
            this.operatorConversation = operatorConversation;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation)
    {
        lock.lock();
        try {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Abonent ready to commutate"));
            request.addToLog("abonent ready to commutate");
            fireAbonentReadyEvent();
            abonentReadyToCommutate = true;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void bridgeActivated(IvrConversationsBridge bridge) {
        request.fireCommutatedEvent();
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
    }

    String logMess(String message, Object... args)
    {
        return request.logMess("Operator ("+owner.getName()+"). "+message, args);
    }

    private void freeResources(){        
    }

    public long getWaitTimeout() {
        return waitTimeout;
    }

    public Node getOwner() {
        return owner;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public int getPriority() {
        return request.getPriority();
    }

    public void conversationCompleted(ConversationResult res)
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Operator's conversation completed"));
        lock.lock();
        try {
            operatorConversationFlag.set(false);
            checkState();
            request.addToLog(String.format(
                    "conv. for op. number (%s) completed (%s)", getNumber(), res.getCompletionCode()));
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    private void fireAbonentReadyEvent()
    {
        for (CallsCommutationManagerListener listener: listeners)
            listener.abonentReady();
    }
}
