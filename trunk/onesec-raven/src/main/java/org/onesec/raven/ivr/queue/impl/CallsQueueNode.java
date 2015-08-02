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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueuesContainerNode.class)
public class CallsQueueNode extends BaseNode implements CallsQueue, Viewable
{
    private static final int STOP_PROCESSOR_TIMEOUT = 1000;

    @NotNull @Parameter(defaultValue="10")
    private Integer maxQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @Message private static String queueTitleMessage;
    @Message private static String queueBusyMessage;
    @Message private static String numberInQueueMessage;
    @Message private static String priorityMessage;
    @Message private static String requestIdMessage;
    @Message private static String queueTimeMessage;
    @Message private static String targetQueueMessage;
    @Message private static String nextOnBusyBehaviourStepMessage;
    @Message private static String lastOperatorIndexMessage;
    @Message private static String requestMessage;
    @Message private static String operatorsTitleMessage;
    @Message private static String operatorPhoneMessage;
    @Message private static String operatorDescMessage;
    @Message private static String operatorActiveMessage;
    @Message private static String operatorBusyMessage;
    @Message private static String operatorBusyTimerMessage;
    @Message private static String operatorHandledCallsMessage;
    @Message private static String operatorChanceToReceiveCallMessage;
    @Message private static String operatorCurrentCallMessage;
    @Message private static String yesMessage;
    @Message private static String noMessage;
    
    private AtomicReference<DataProcessorFacade> processor;

    private AtomicLong sumCallDuration;
    private AtomicInteger callsCount;

    @Override
    protected void initFields() {
        super.initFields();
        processor = new AtomicReference<>();
        callsCount = new AtomicInteger();
        sumCallDuration = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        resetStat();
        processor.set(new DataProcessorFacadeConfig(
                "Queue", this, new CallsQueueDataProcessor(maxQueueSize), executor, new LoggerHelper(this, null)).build());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        DataProcessorFacade _processor = processor.getAndSet(null);
        if (_processor!=null)
            _processor.askStop(STOP_PROCESSOR_TIMEOUT, TimeUnit.MILLISECONDS).get();
    }    

    @Override
    public void queueCall(CallQueueRequestController request) {
        DataProcessorFacade _processor = processor.get();
        if (_processor!=null)
            _processor.send(request);
        else {
            request.addToLog(String.format("Queue (%s) not ready", getName()));
            request.fireRejectedQueueEvent();
        }
    }
    
    public Collection<CallQueueRequestController> getRequests() {
        DataProcessorFacade _processor = processor.get();
        if (_processor!=null)
            return (Collection<CallQueueRequestController>) 
                    _processor.ask(CallsQueueDataProcessor.GET_REQUESTS)
                            .getOrElse(Collections.EMPTY_LIST, 1000);
        return Collections.EMPTY_LIST;
    }
    
    @Parameter(readOnly=true)
    public int getAvgCallDuration() {
        int _callsCount = callsCount.get();
        return (int) (_callsCount==0? 0 : sumCallDuration.get()/_callsCount);
    }

    @Parameter(readOnly=true)
    public int getActiveOperatorsCount() {
        return getOpers(true).size();
    }

    public void resetStat() {
        sumCallDuration.set(0l);
        callsCount.set(0);
    }

    public void updateCallDuration(int callDuration) {
        sumCallDuration.addAndGet(callDuration);
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception 
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(1);
        //queue table
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, queueTitleMessage));
        TableImpl tab = new TableImpl(new String[]{numberInQueueMessage, requestIdMessage,
                priorityMessage, queueTimeMessage, targetQueueMessage, 
                nextOnBusyBehaviourStepMessage, lastOperatorIndexMessage, requestMessage});
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        int numInQueue = 1;
        for (CallQueueRequestController req: getRequests()) {
            tab.addRow(new Object[]{numInQueue++, req.getRequestId(), req.getPriority()
                    , fmt.format(new Date(req.getLastQueuedTime()))
                    , req.getTargetQueue().getName()
                    , req.getOnBusyBehaviourStep(), req.getOperatorIndex(), req.toString()
            });
        }
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, tab));
        //queue operators table
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, operatorsTitleMessage));
        TableImpl opersTab = new TableImpl(new String[]{operatorPhoneMessage, operatorDescMessage, 
            operatorActiveMessage, operatorBusyMessage, operatorBusyTimerMessage, operatorHandledCallsMessage,
            operatorCurrentCallMessage});
        List<Object[]> tableRows = new ArrayList<Object[]>();
        for (CallsQueueOperator oper: getOpers(false)) {
            CallsQueueOperatorNode operNode = (CallsQueueOperatorNode)(oper instanceof CallsQueueOperatorNode? oper : null);
            String operPhone = operNode==null? null : operNode.getPhoneNumbers();
            Boolean busy = operNode==null? null : operNode.getBusy();
            Long busyTimer = operNode==null? null : operNode.getBusyTimerValue();
            String curCall = operNode==null? null : operNode.getProcessingRequest();
            tableRows.add(new Object[]{operPhone, oper.getPersonDesc(), oper.isActive(), busy, busyTimer, 
                oper.getHandledRequests(), curCall});
        }
        sortOpersRows(tableRows);
        addMarkupToOpersRows(tableRows);
        for (Object[] row: tableRows)
            opersTab.addRow(row);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, opersTab));
        return vos;
    }
    
    private void addMarkupToOpersRows(List<Object[]> rows) {
        for (Object[] row: rows) {
            boolean active = (Boolean)row[2];
            for (int i=0; i<row.length; ++i) {
                if (row[i] instanceof Boolean)
                    row[i] = (Boolean)row[i]? yesMessage : noMessage;
                if (!active)
                    row[i] = "<span style='color:#7a7a7a'>"+(row[i]==null?"":row[i])+"</span>";
            }
        }
    }
    
    private void sortOpersRows(List<Object[]> rows) {
        Collections.sort(rows, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                return mkHash((Object[])o1).compareTo(mkHash((Object[])o2));
            }
            private String mkHash(Object[] r) {
                return ""+((Boolean)r[2]? 0:1)+r[1];
            }
        });
    }
    
    private Set<CallsQueueOperator> getOpers(boolean onlyActive) {
        Set<CallsQueueOperator> opers = new HashSet<CallsQueueOperator>();
        for (Node priority: getNodes())
            for (CallsQueueOperatorRefNode ref: NodeUtils.getChildsOfType(priority, CallsQueueOperatorRefNode.class)) {
                CallsQueueOperator oper = ref.getOperator();
                if (oper.isStarted() && (!onlyActive || oper.isActive()))
                    opers.add(oper);
            }
        return opers.isEmpty()? Collections.EMPTY_SET : opers;
    }

    public Integer getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
