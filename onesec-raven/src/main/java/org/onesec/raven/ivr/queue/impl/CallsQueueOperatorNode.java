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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.event.impl.OperatorBusyTimerStartedImpl;
import org.onesec.raven.ivr.queue.event.impl.OperatorBusyTimerStoppedImpl;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.impl.AbstractTask;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOperatorsNode.class)
public class CallsQueueOperatorNode extends AbstractOperatorNode {
    //время (сек.) на которое оператор заблокируется (перейдет в состояние busy) в случае не успешного звонка
    public final static Integer ON_UNSUCCESS_CALL_BUSY_TIMEOUT = 1; 
    
    @NotNull @Parameter
    private String phoneNumbers;
    @Parameter
    private String personId;
    @Parameter
    private String personDesc;
    @Parameter
    private Integer busyTimer;

    private AtomicBoolean busy;
    private AtomicReference<CallsCommutationManager> commutationManager;
    private AtomicReference<String> request;
    private AtomicLong busyTimerEndTime;
    private AtomicBoolean busyByBusyTimer;
    
    @Override
    protected void initFields() {
        super.initFields();
        busy = new AtomicBoolean(false);
        request = new AtomicReference<String>();
        commutationManager = new AtomicReference<CallsCommutationManager>();
        busyTimerEndTime = new AtomicLong();
        busyByBusyTimer = new AtomicBoolean(false);
    }

    @Override
    protected void doStart() throws Exception {
        busy.set(false);
        super.doStart();
    }

    public Integer getBusyTimer() {
        return busyTimer;
    }

    public void setBusyTimer(Integer busyTimer) {
        this.busyTimer = busyTimer;
    }
    
    @Parameter(readOnly=true)
    public Boolean getBusy() {
        return busy.get();
    }
    
    @Parameter(readOnly=true)
    public String getProcessingRequest(){
        return request.get();
    }

    public boolean isActive() {
        return getActive();
    }

    public String getOperatorDesc() {
        return personDesc;
    }

    public void setPersonDesc(String personDesc) {
        this.personDesc = personDesc;
    }

    public String getOperatorId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public String getPersonDesc() {
        return getOperatorDesc();
    }

    @Override
    public String getPersonId() {
        return getOperatorId();
    }
    
    @Parameter(readOnly=true)
    public Long getBusyTimerValue() {
        long val = (busyTimerEndTime.get()-System.currentTimeMillis())/1000;
        return val<0? 0 : val;
    }

    @Override
    protected boolean doProcessRequest(CallsQueue queue, CallQueueRequestController request
            , IvrConversationScenario conversationScenario, AudioFile greeting
            , String operatorPhoneNumbers, Integer inviteTimeout)
    {
        if (getCallsQueues().getUseOnlyRegisteredOperators()==true && getOperatorId()==null)
            return false;
//        if (timeoutEndTime.get()>=System.currentTimeMillis() || !busy.compareAndSet(false, true) ) {
        if (busyByBusyTimer.get() || !busy.compareAndSet(false, true) ) {
            onBusyRequests.incrementAndGet();
            return false;
        }
        try {
//            timeoutEndTime.set(0);
            final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            this.request.set(fmt.format(new Date())+": "+request.toString());
            final String _nums = operatorPhoneNumbers==null||operatorPhoneNumbers.trim().length()==0?
                    phoneNumbers : operatorPhoneNumbers;
            commutationManager.set(commutate(queue, request, _nums, inviteTimeout, conversationScenario, greeting));
            return true;
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(request.logMess("Error handling request by operator"), e);
            busy.set(false);
            this.request.set(null);
            return false;
        }
    }

    @Override
    protected void doRequestProcessed(CallsCommutationManager manager, boolean callHandled) {
        if (commutationManager.compareAndSet(manager, null)) {
            startBusyTimer(callHandled? busyTimer : ON_UNSUCCESS_CALL_BUSY_TIMEOUT);
                
            busy.set(false);
            request.set(null);
        }
    }
    
    private void startBusyTimer(Integer timeout) {
//        Integer timeout = busyTimer;
        if (timeout!=null && busyByBusyTimer.compareAndSet(false, true)) {
            if (getExecutor().executeQuietly(timeout*1000, new BusyTimerTask())) { 
                busyTimerEndTime.set(System.currentTimeMillis()+timeout*1000);
                getCallsQueues().fireEvent(new OperatorBusyTimerStartedImpl(
                    timeout, getName(), personId, personDesc));
            } else
                busyByBusyTimer.set(false);
        }
    }

    public boolean resetBusyTimer() {
        if (!busyByBusyTimer.compareAndSet(true, false)) 
            return false;
        //fireBusyTimer stopped
        getCallsQueues().fireEvent(new OperatorBusyTimerStoppedImpl(getName(), getPersonId(), getPersonDesc()));
        return true;
    }
    
    public CallsQueueOperator callTransferedFromOperator(String phoneNumber
            , CallsCommutationManager manager) 
    {
        doRequestProcessed(commutationManager.get(), true);
        return getCallsQueues().processCallTransferedEvent(phoneNumber, manager);
    }

    public boolean callTransferedToOperator(CallsCommutationManager manager) {
        if (busy.compareAndSet(false, true)) {
            this.commutationManager.set(manager);
            return true;
        } else
            return false;
    }
    
    /**
     * for test purposes
     */
    CallsCommutationManager getCommutationManager(){
        return commutationManager.get();
    }
    
    private class BusyTimerTask extends AbstractTask {

        public BusyTimerTask() {
            super(CallsQueueOperatorNode.this, "BusyTimer");
        }

        @Override
        public void doRun() throws Exception {
            resetBusyTimer();
        }
    }
}