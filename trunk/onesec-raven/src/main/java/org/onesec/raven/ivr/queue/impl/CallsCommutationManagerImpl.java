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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.queue.*;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;

/**
 *
 * @author Mikhail Titov
 */
public class CallsCommutationManagerImpl implements CallsCommutationManager {
    private final ExecutorService executor;
    private final CallQueueRequestController req;
    private final long inviteTimeout;
    private long inviteEndTime;
    private final Integer parallelCallAfter;
    private final CallsQueue queue;
    private final long waitTimeout;
    private final String[] numbers;
    private final IvrConversationScenario conversationScenario;
    private final IvrConversationsBridgeManager conversationBridgeManager;
    private CallsQueueOperator operator;
    private final IvrEndpointPool endpointPool;

    private final Set<CommutationManagerCall> calls = new HashSet<CommutationManagerCall>();
    private final AtomicBoolean callHandled = new AtomicBoolean(false);
    private int numberPos = 0;

    public CallsCommutationManagerImpl(ExecutorService executor, CallQueueRequestController req
            , int inviteTimeout, Integer parallelCallAfter, CallsQueue queue, long waitTimeout
            , String[] numbers, IvrConversationScenario conversationScenario
            , IvrConversationsBridgeManager conversationsBridgeManager
            , IvrEndpointPool endpointPool
            , CallsQueueOperator operator)
    {
        this.executor = executor;
        this.req = req;
        this.inviteTimeout = inviteTimeout*1000;
        this.parallelCallAfter = parallelCallAfter;
        this.queue = queue;
        this.waitTimeout = waitTimeout;
        this.numbers = numbers;
        this.operator = operator;
        this.endpointPool = endpointPool;
        this.conversationScenario = conversationScenario;
        this.conversationBridgeManager = conversationsBridgeManager;
        this.inviteEndTime = System.currentTimeMillis()+this.inviteTimeout;
    }

    public void commutate(){
        tryCommutateWithNumber(numbers[numberPos]);
        if (parallelCallAfter!=null)
            executor.executeQuietly(parallelCallAfter*1000, new AbstractTask(operator, "Waiting for parallel call") {
                @Override public void doRun() throws Exception {
                    commutateWithOtherNumbers();
                }
            });
    }

    public void callFinished(CommutationManagerCall call, boolean successfull) {
        callHandled.compareAndSet(false, successfull);
        synchronized(calls){
            calls.remove(call);
            if (parallelCallAfter==null && ++numberPos<numbers.length) {
                inviteEndTime = System.currentTimeMillis()+inviteTimeout;
                tryCommutateWithNumber(numbers[numberPos]);
            }
            if (calls.isEmpty()) {
                if (!callHandled.get())
                    queue.queueCall(req);
                operator.requestProcessed(this, callHandled.get());
                if (!callHandled.get())
                    req.addToLog(String.format("NOT handled by op.(%s)", operator.getName()));
            }
        }
    }

    public synchronized void callTransfered(String phoneNumber) {
        if (operator.isLogLevelEnabled(LogLevel.DEBUG))
            operator.getLogger().debug(req.logMess(
                    "Transfering call from (%s) to other operator", operator.getName()));
        operator = operator.callTransferedFromOperator(phoneNumber, this);
        req.fireCallTransfered(operator.getName(), phoneNumber, operator.getPersonId()
                , operator.getPersonDesc());
        if (operator.isLogLevelEnabled(LogLevel.DEBUG))
            operator.getLogger().debug(req.logMess(
                    "Call successfully transfered to the (%s) operator", operator.getName()));
    }

    private void tryCommutateWithNumber(String number) {
        final CommutationManagerCall call = new CommutationManagerCallImpl(this, number);
        synchronized(calls){
            try {
                executor.execute(new AbstractTask(operator, "Initiating call to operator number " + number) {
                    @Override public void doRun() throws Exception {
                        call.commutate();
                    }
                });
                calls.add(call);
            } catch (ExecutorServiceException ex) {
                callFinished(null, false);
            }
        }
    }

    private void commutateWithOtherNumbers(){
        synchronized(calls) {
            if (calls.isEmpty())
                return;
            for (int i=1; i<numbers.length; ++i)
                tryCommutateWithNumber(numbers[i]);
        }
    }

    public void incOnNoFreeEndpointsRequests() {
        operator.incOnNoFreeEndpointsRequests();
    }

    public IvrConversationScenario getConversationScenario() {
        return conversationScenario;
    }

    public IvrConversationsBridgeManager getConversationsBridgeManager() {
        return conversationBridgeManager;
    }

    public IvrEndpointPool getEndpointPool() {
        return endpointPool;
    }

    /**
     * Returns invite timeout in milliseconds. If method returns the result that equals or less than 0 so the
     * time wait for invite was ended
     */
    public long getInviteTimeout() {
        return inviteEndTime-System.currentTimeMillis();
    }

    public CallsQueueOperator getOperator() {
        return operator;
    }

    public CallsQueue getQueue() {
        return queue;
    }

    public CallQueueRequestController getRequest() {
        return req;
    }

    public long getWaitTimeout() {
        return waitTimeout;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
