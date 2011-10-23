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
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class CommutationManagerCallImpl implements CommutationManagerCall, IvrConversationsBridgeListener
        , EndpointRequest, ConversationCompletionCallback
{
    public final static String SEARCHING_FOR_ENDPOINT_MSG = "Looking up for free endpoint in the pool (%s)";

    private final CallsCommutationManager manager;
    private final String number;
    
    private final AtomicReference<String> statusMessage;
    private final List<CallsCommutationManagerListener> listeners =
            new LinkedList<CallsCommutationManagerListener>();
    private AtomicBoolean operatorConversationFlag = new AtomicBoolean(false);
    private boolean operatorReadyToCommutate = false;
    private boolean readyToCommutateSended = false;
    private boolean abonentReadyToCommutate = false;
    private boolean commutated = false;
    private AtomicBoolean commutationValid = new AtomicBoolean(true);
    private IvrEndpointConversation operatorConversation = null;
    private ReentrantLock lock = new ReentrantLock();
    private Condition eventCondition = lock.newCondition();

    public CommutationManagerCallImpl(CallsCommutationManager manager, String number) {
        this.manager = manager;
        this.number = number;
        this.statusMessage = new AtomicReference<String>(
                String.format(SEARCHING_FOR_ENDPOINT_MSG, manager.getRequest().toString()));
    }

    private boolean checkState() {
        commutationValid.compareAndSet(true, manager.getRequest().isValid()
                && (!manager.getRequest().isHandlingByOperator() || readyToCommutateSended)
                && operatorConversationFlag.get());
        return commutationValid.get();
    }

    public boolean isCommutationValid() {
        return commutationValid.get();
    }

    private String getNumber(){
        return number;
    }

    public CallQueueRequestWrapper getRequest() {
        return manager.getRequest();
    }

    public void addListener(CallsCommutationManagerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CallsCommutationManagerListener listener) {
        listeners.remove(listener);
    }

    public void commutate() {
        try {
            if (manager.getRequest().isValid() && !manager.getRequest().isHandlingByOperator())
                manager.getEndpointPool().requestEndpoint(this);
            else
                manager.callFinished(this, false);
        } catch (ExecutorServiceException ex) {
            if (manager.getOperator().isLogLevelEnabled(LogLevel.ERROR))
                manager.getOperator().getLogger().error(logMess(
                        "Get endpoint from pool error"), ex);
            manager.getRequest().addToLog("get endpoint from pool error");
            manager.callFinished(this, false);
        }
    }

    public void processRequest(IvrEndpoint endpoint) {
        boolean callHandled = false;
        try{
            if (endpoint==null) {
                if (manager.getOperator().isLogLevelEnabled(LogLevel.WARN))
                    manager.getOperator().getLogger().warn(logMess(
                            "Can't process call queue request because of no free endpoints in the pool (%s)"
                            , manager.getEndpointPool().getName()));
                manager.getRequest().addToLog("no free endpoints in the pool");
                manager.callFinished(this, false);
                return;
            }

            Map<String, Object> bindings = new HashMap<String, Object>();
            bindings.put(CALLS_COMMUTATION_MANAGER_BINDING, this);
            bindings.put(CALL_QUEUE_REQUEST_BINDING, manager.getRequest());
            try{
                if (callToOperator(endpoint, bindings))
                    callHandled = true;
                else {
                    if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
                        manager.getOperator().getLogger().debug(logMess("Call not handled"));
                    manager.getRequest().addToLog(String.format(
                            "operator (%s) didn't handle a call", manager.getOperator().getName()));
                }
            } catch(Exception e){
                if (manager.getOperator().isLogLevelEnabled(LogLevel.ERROR))
                    manager.getOperator().getLogger().error(logMess("Error handling by operator"), e);
                manager.getRequest().addToLog("error handling by operator");
            }
        } finally {
            manager.callFinished(this, callHandled);
        }
    }

    public boolean callToOperator(IvrEndpoint endpoint, Map<String, Object> bindings) throws Exception
    {
        operatorConversationFlag.set(true);
        if (!checkState())
            return false;
        manager.getRequest().fireOperatorNumberQueueEvent(getNumber());
        endpoint.invite(getNumber(), manager.getConversationScenario(), this, bindings);
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
                    if (manager.getRequest().fireReadyToCommutateQueueEvent(this)) 
                        readyToCommutateSended=true;
                }
                if (abonentReadyToCommutate && !commutated)
                    commutateCalls();
            }

            if (!checkState() && endpoint.getEndpointState().getId()==IvrEndpointState.INVITING)
                restartEndpoint(endpoint);

            if (commutated) {
                manager.getRequest().fireDisconnectedQueueEvent();
                return true;
            } else
                return false;
        } finally {
            lock.unlock();
        }
        
    }

    private boolean checkInviteTimeout(long callStartTime, IvrEndpoint endpoint) throws Exception
    {
        if (endpoint.getEndpointState().getId()==IvrEndpointState.INVITING
            && callStartTime+manager.getInviteTimeout()*1000<=System.currentTimeMillis())
        {
            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
                manager.getOperator().getLogger().debug(logMess("Operator's number (%s) not answered", getNumber()));
            manager.getRequest().addToLog(String.format("number (%s) not answer", getNumber()));
            //restarting endpoint
            restartEndpoint(endpoint);
            return false;
        }
        return true;
    }

    public void commutateCalls() throws IvrConversationBridgeExeption
    {
        IvrConversationsBridge bridge = manager.getConversationsBridgeManager().createBridge(
                manager.getRequest().getConversation(), operatorConversation, logMess(""));
        bridge.addBridgeListener(this);
        bridge.activateBridge();
        commutated = true;
    }

    public void operatorReadyToCommutate(IvrEndpointConversation operatorConversation)
    {
        lock.lock();
        try {
            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
                manager.getOperator().getLogger().debug(logMess("Number (%s) ready to commutate", getNumber()));
            operatorReadyToCommutate=true;
            manager.getRequest().addToLog(String.format("op. number (%s) ready to commutate", getNumber()));
            this.operatorConversation = operatorConversation;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        lock.lock();
        try {
            if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
                manager.getOperator().getLogger().debug(logMess("Abonent ready to commutate"));
            manager.getRequest().addToLog("abonent ready to commutate");
            fireAbonentReadyEvent();
            abonentReadyToCommutate = true;
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void bridgeActivated(IvrConversationsBridge bridge) {
        manager.getRequest().fireCommutatedEvent();
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
    }

    String logMess(String message, Object... args) {
        return manager.getRequest().logMess("Operator ("+manager.getOperator().getName()+"). "+message, args);
    }

    private void freeResources(){        
    }

    public long getWaitTimeout() {
        return manager.getWaitTimeout();
    }

    public Node getOwner() {
        return manager.getOperator();
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public int getPriority() {
        return manager.getRequest().getPriority();
    }

    public void conversationCompleted(ConversationResult res) {
        if (manager.getOperator().isLogLevelEnabled(LogLevel.DEBUG))
            manager.getOperator().getLogger().debug(logMess("Operator's conversation completed"));
        lock.lock();
        try {
            operatorConversationFlag.set(false);
            checkState();
            manager.getRequest().addToLog(String.format(
                    "conv. for op. number (%s) completed (%s)", getNumber(), res.getCompletionCode()));
            eventCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    private void fireAbonentReadyEvent() {
        for (CallsCommutationManagerListener listener: listeners)
            listener.abonentReady();
    }

    private void restartEndpoint(IvrEndpoint endpoint) throws Exception {
        endpoint.stop();
        TimeUnit.SECONDS.sleep(1);
        endpoint.start();
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 10000);
        if (res.isWaitInterrupted())
            throw new Exception("Wait for IN_SERVICE timeout");
    }
}
