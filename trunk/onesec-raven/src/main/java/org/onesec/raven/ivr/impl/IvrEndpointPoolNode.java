/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.*;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.ConversationScenario;
import org.raven.dp.DataProcessor;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.log.LogLevel;
import org.raven.sched.*;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.LoadAverageStatistic;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=IvrEndpointNode.class)
public class IvrEndpointPoolNode extends BaseNode implements IvrEndpointPool, Viewable, ManagedTask, Schedulable
{
    public static final int LOADAVERAGE_INTERVAL = 60000;
    
    private final static String STATUS_FORMAT = "<span style=\"color: %s\"><b>%s</b></span>";
    
    public enum UseCase {INCOMING_CALLS, OUTGOING_CALLS};
    public enum PrioritizationType {STRICT, WEIGTH}
    
    @NotNull @Parameter(defaultValue="OUTGOING_CALLS")
    private UseCase useCase;
    
    @NotNull @Parameter(defaultValue="STRICT")
    private PrioritizationType prioritizationType;
    
    @NotNull @Parameter(defaultValue="100")
    private Integer maxRequestQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler watchdogScheduler;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool auxiliaryPool;

    @Parameter(readOnly=true)
    private LoadAverageStatistic loadAverage;

    @Parameter(readOnly=true)
    private AtomicInteger auxiliaryPoolUsageCount;

    @Parameter
    private String addressRanges;

    @Parameter
    private IvrConversationScenarioNode conversationScenario;

    @NotNull @Parameter(defaultValue="false")
    private Boolean enableIncomingRtp;

    @NotNull @Parameter(defaultValue="AUTO")
    private Codec codec;

    @Parameter
    private Integer rtpPacketSize;

    @NotNull @Parameter(defaultValue="5")
    private Integer rtpInitialBuffer;

    @NotNull @Parameter(defaultValue="0")
    private Integer rtpMaxSendAheadPacketsCount;

    @NotNull @Parameter(defaultValue="false")
    private Boolean enableIncomingCalls;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean shareInboundOutboundPort;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean startRtpImmediatelly;    
    
    @Parameter
    private Integer maxRequestsPerSecond;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsRouter callsRouter;

    private ReadWriteLock lock;
    private Condition endpointReleased;
    private Map<Integer, RequestInfo> busyEndpoints;
    private Map<Integer, Long> usageCounters;
    private BlockingQueue<RequestInfo> queue;
    private AtomicBoolean stopManagerTask;
    private AtomicBoolean managerThreadStoped;
    private AtomicReference<String> statusMessage;
    private AtomicBoolean watchdogRunning;
    private final static AtomicLong requestSeq = new AtomicLong();
    private int sessionMaxRequestsPerSecond;
    private int requestsCountInSecond;
    private long lastMaxRequestsPerSecondCheckTime;
    private ConcurrentMap<IvrEndpoint, ReservedEndpointInfo> reservedEndpoints;

    @Message private static String totalUsageCountMessage;
    @Message private static String terminalsTableTitleMessage;
    @Message private static String terminalColumnMessage;
    @Message private static String terminalPortMessage;
    @Message private static String terminalStatusColumnMessage;
    @Message private static String terminalPoolStatusColumnMessage;
    @Message private static String usageCountColumnMessage;
    @Message private static String currentUsageTimeMessage;
    @Message private static String requesterNodeMessage;
    @Message private static String requesterStatusMessage;
    @Message private static String queueTableTitleMessage;
    @Message private static String queueTimeMessage;
    @Message private static String waitingTimeMessage;
    @Message private static String terminalReservedStatus;
    @Message private static String callIdMessage;
    @Message private static String callCreatedMessage;
    @Message private static String callDurationMessage;
    @Message private static String callDescriptionMessage;
    @Message private static String yesMessage;
    @Message private static String noMessage;
    
    @Message private static String prioritiesRatesTableTitleMessage;
    @Message private static String priorityColumnMessage;
    @Message private static String rateColumnMessage;

    @Override
    protected void initFields() {
        super.initFields();
        lock = new ReentrantReadWriteLock();
        endpointReleased = lock.writeLock().newCondition();
        busyEndpoints = new ConcurrentHashMap<Integer, RequestInfo>();
        usageCounters = new ConcurrentHashMap<Integer, Long>();
        stopManagerTask = new AtomicBoolean(false);
        statusMessage = new AtomicReference<String>("");
        managerThreadStoped = new AtomicBoolean(true);
        auxiliaryPoolUsageCount = new AtomicInteger(0);
        reservedEndpoints = new ConcurrentHashMap<IvrEndpoint, ReservedEndpointInfo>();
        watchdogRunning = new AtomicBoolean();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (!managerThreadStoped.get())
            throw new Exception("Can't start pool because of manager task is still running");
        lock = new ReentrantReadWriteLock();
        endpointReleased = lock.writeLock().newCondition();
//        queue = new LinkedBlockingQueue<RequestInfo>(maxRequestQueueSize);
        switch (prioritizationType) {
            case STRICT : 
                queue = new PriorityBlockingQueue<RequestInfo>(maxRequestQueueSize, new RequestComparator());
                break;
            case WEIGTH :
                queue = new ProbabilisticPriorityQueue<RequestInfo>(maxRequestQueueSize);
                break;
            default: throw new Exception("Invalid prioritization type: "+prioritizationType);
        }
//        queue = new PriorityBlockingQueue<RequestInfo>(maxRequestQueueSize, new RequestComparator());
        busyEndpoints.clear();
        usageCounters.clear();
        stopManagerTask.set(false);
        synchEndpointsWithAddressRanges();
        executor.execute(this);
        loadAverage = new LoadAverageStatistic(LOADAVERAGE_INTERVAL, getChildrenCount());
        sessionMaxRequestsPerSecond = maxRequestsPerSecond==null? Integer.MAX_VALUE : maxRequestsPerSecond;
        lastMaxRequestsPerSecondCheckTime = 0;
        requestsCountInSecond = 0;
        reservedEndpoints.clear();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        stopManagerTask.set(true);
        while (!managerThreadStoped.get())
            TimeUnit.MILLISECONDS.sleep(100);
    }

    @Override
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pool operations not supported by this data source");
    }

    @Override
    public Boolean getStopProcessingOnError() {
        return true;
    }

    @Override
    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public TaskRestartPolicy getTaskRestartPolicy() {
        return TaskRestartPolicy.RESTART_NODE;
    }

    public ConversationScenario getConversationScenario(IvrEndpoint endpoint) {
        ReservedEndpointInfo info = reservedEndpoints.get(endpoint);
        return info==null? getConversationScenario() : info.scenario;
    }

    @Override
    public void handleCallEvent(CallEventType callEvent, IvrEndpointConversation conversation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrEndpoint reserveEndpoint(ReserveEndpointRequest request) {
        if (!isStarted())
            return null;
        if (useCase!=UseCase.INCOMING_CALLS) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Can't reserve endpoints in OUTGOING_CALLS mode");
            return null;
        }
        ConversationScenario scenario = request.getConversationScenario();
        if (scenario==null)
            scenario = getConversationScenario();
        if (scenario==null) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Can't reserve endpoint because of conversation scenario not defined "
                        + "neither in reserve request nor in the conversationScenarion attribute");
            return null;
        }
        ReservedEndpointInfo res = new ReservedEndpointInfo(scenario);
        for (IvrEndpoint endpoint: NodeUtils.getChildsOfType(this, IvrEndpoint.class))
            if (   endpoint.getEndpointState().getId()==IvrEndpointState.IN_SERVICE
                && endpoint.getActiveCallsCount()==0
                && reservedEndpoints.putIfAbsent(endpoint, res)==null) 
            {
                executor.executeQuietly(request.getTimeout(), new UnreserveEndpointTask(endpoint));
                incEndpointUsageCounter(endpoint);
                return endpoint;
            }
        if (isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("No free endpoints in the pool");
        return null;
    }

    public void requestEndpoint(EndpointRequest request) 
    {
        if (!isStarted() || stopManagerTask.get()) {
            request.processRequest(null);
            return;
        }
        if (useCase!=UseCase.OUTGOING_CALLS) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Can't requests endpoints in INCOMING_CALLS mode");
            request.processRequest(null);
            return;
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("New request added to the queue from ({})", request.getOwner().getPath());
        if (!queue.offer(new RequestInfo(request))) {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "The queue size was exceeded. The request from the (%s) was ignored."
                        , request.getOwner().getPath()));
            request.processRequest(null);
        }
    }

    public void releaseEndpoint(IvrEndpoint endpoint) {
        if (endpoint==null)
            throw new NullPointerException("Endpoint parameter can not be null");
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Realesing endpoint (%s) to the pool", endpoint.getName()));
        lock.writeLock().lock();
        try {
            RequestInfo req = busyEndpoints.remove(endpoint.getId());
            if (req!=null){
                loadAverage.addDuration(System.currentTimeMillis()-req.execStartTime);
                endpointReleased.signal();
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format("Endpoint (%s) successfully realesed to the pool"
                            , endpoint.getName()));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void cleanupQueue()
    {
        statusMessage.set("Cleaning up queue from timeouted requests");
        RequestInfo ri;
        Iterator<RequestInfo> it = queue.iterator();
        while (it.hasNext()) {
            ri = it.next();
            if (System.currentTimeMillis() - ri.startTime > ri.request.getWaitTimeout()) {
                it.remove();
                ri.request.processRequest(null);
            }
        }
    }

    private void clearQueue() 
    {
        statusMessage.set("Stopping processing requests. Clearing queue...");
        for (RequestInfo ri : queue) 
            ri.request.processRequest(null);
    }

    private void processRequest() throws InterruptedException {
        try {
            try{
                statusMessage.set("Waiting for request...");
                RequestInfo ri = queue.poll(10, TimeUnit.SECONDS);
                if (ri != null) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug("Processing request from (" + ri.getTaskNode().getPath() + ")");
                    lookupForEndpoint(ri);
                    if (ri.endpoint == null) {
                        IvrEndpointPool _auxiliaryPool = auxiliaryPool;
                        if (_auxiliaryPool==null)
                            ri.request.processRequest(null);
                        else{
                            auxiliaryPoolUsageCount.incrementAndGet();
                            _auxiliaryPool.requestEndpoint(ri.request);
                        }
                    } else {
                        if (!sendResponse(ri)) 
                            try {
                                ri.request.processRequest(null);
                            } finally {
                                releaseEndpoint(ri.endpoint, 0);
                            }
                    }
                }
            }finally{
                cleanupQueue();
            }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                throw (InterruptedException) e;
            } else if (isLogLevelEnabled(LogLevel.ERROR)) {
                getLogger().error("Error processing request", e);
            }
        }
    }
    
    private synchronized void applyMaxRequestsPerSecondPolicy() {
        long ts = System.currentTimeMillis();
        if (ts/1000 > lastMaxRequestsPerSecondCheckTime/1000) {
            lastMaxRequestsPerSecondCheckTime = ts;
            requestsCountInSecond=1;
        } else if (requestsCountInSecond<=sessionMaxRequestsPerSecond) {
            ++requestsCountInSecond;
        } else {
            try {
                Thread.sleep(1000-ts%1000);
                requestsCountInSecond = 1;
                lastMaxRequestsPerSecondCheckTime = System.currentTimeMillis();
            } catch (InterruptedException ex) { }
        }
    }

    private boolean sendResponse(RequestInfo requestInfo) {
        try {
            applyMaxRequestsPerSecondPolicy();
            statusMessage.set("Executing response for request from ("+requestInfo.getTaskNode().getPath()+")");
            executor.execute(requestInfo);
            return true;
        } catch(ExecutorServiceException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error executing task for ("+requestInfo.getTaskNode().getPath()+")", e);
        }
        return false;
    }

    public UseCase getUseCase() {
        return useCase;
    }

    public void setUseCase(UseCase useCase) {
        this.useCase = useCase;
    }

    public PrioritizationType getPrioritizationType() {
        return prioritizationType;
    }

    public void setPrioritizationType(PrioritizationType prioritizationType) {
        this.prioritizationType = prioritizationType;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Scheduler getWatchdogScheduler() {
        return watchdogScheduler;
    }

    public void setWatchdogScheduler(Scheduler watchdogScheduler) {
        this.watchdogScheduler = watchdogScheduler;
    }

    public Integer getMaxRequestQueueSize() {
        return maxRequestQueueSize;
    }

    public void setMaxRequestQueueSize(Integer maxRequestQueueSize) {
        this.maxRequestQueueSize = maxRequestQueueSize;
    }

    public IvrEndpointPool getAuxiliaryPool() {
        return auxiliaryPool;
    }

    public void setAuxiliaryPool(IvrEndpointPool auxiliaryPool) {
        this.auxiliaryPool = auxiliaryPool;
    }

    public LoadAverageStatistic getLoadAverage() {
        return loadAverage;
    }

    public AtomicInteger getAuxiliaryPoolUsageCount() {
        return auxiliaryPoolUsageCount;
    }

    public String getAddressRanges() {
        return addressRanges;
    }

    public void setAddressRanges(String addressRanges) {
        this.addressRanges = addressRanges;
    }

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    public IvrConversationScenarioNode getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenarioNode conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public Boolean getEnableIncomingCalls() {
        return enableIncomingCalls;
    }

    public void setEnableIncomingCalls(Boolean enableIncomingCalls) {
        this.enableIncomingCalls = enableIncomingCalls;
    }

    public Boolean getShareInboundOutboundPort() {
        return shareInboundOutboundPort;
    }

    public void setShareInboundOutboundPort(Boolean shareInboundOutboundPort) {
        this.shareInboundOutboundPort = shareInboundOutboundPort;
    }

    public Boolean getStartRtpImmediatelly() {
        return startRtpImmediatelly;
    }

    public void setStartRtpImmediatelly(Boolean startRtpImmediatelly) {
        this.startRtpImmediatelly = startRtpImmediatelly;
    }

    public Boolean getEnableIncomingRtp() {
        return enableIncomingRtp;
    }

    public void setEnableIncomingRtp(Boolean enableIncomingRtp) {
        this.enableIncomingRtp = enableIncomingRtp;
    }

    public Integer getRtpInitialBuffer() {
        return rtpInitialBuffer;
    }

    public void setRtpInitialBuffer(Integer rtpInitialBuffer) {
        this.rtpInitialBuffer = rtpInitialBuffer;
    }

    public Integer getRtpMaxSendAheadPacketsCount() {
        return rtpMaxSendAheadPacketsCount;
    }

    public void setRtpMaxSendAheadPacketsCount(Integer rtpMaxSendAheadPacketsCount) {
        this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;
    }

    public Integer getRtpPacketSize() {
        return rtpPacketSize;
    }

    public void setRtpPacketSize(Integer rtpPacketSize) {
        this.rtpPacketSize = rtpPacketSize;
    }

    public Integer getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    public void setMaxRequestsPerSecond(Integer maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public CallsRouter getCallsRouter() {
        return callsRouter;
    }

    public void setCallsRouter(CallsRouter callsRouter) {
        this.callsRouter = callsRouter;
    }

    private void releaseEndpoint(IvrEndpoint endpoint, long duration)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Realesing endpoint (%s) to the pool", endpoint.getName()));
        lock.writeLock().lock();
        try {
            loadAverage.addDuration(duration);
            busyEndpoints.remove(endpoint.getId());
            endpointReleased.signal();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Endpoint (%s) successfully realesed to the pool"
                        , endpoint.getName()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void getAndLockFreeEndpoint(RequestInfo requestInfo) throws InterruptedException {
        for (IvrEndpoint endpoint : NodeUtils.getChildsOfType(this, IvrEndpoint.class)) {
            if (   endpoint.getActiveCallsCount()==0
                && !busyEndpoints.containsKey(endpoint.getId())
                && ((IvrEndpoint)endpoint).getEndpointState().getId() == IvrEndpointState.IN_SERVICE)
            {
                busyEndpoints.put(endpoint.getId(), requestInfo);
                requestInfo.terminalUsageTime = System.currentTimeMillis();                
                requestInfo.endpoint = (IvrEndpoint) endpoint;
                incEndpointUsageCounter(endpoint);
//                Long counter = usageCounters.get(endpoint.getId());
//                usageCounters.put(endpoint.getId(), counter == null ? 1 : counter + 1);
                return;
            }
        }
    }
    
    private void incEndpointUsageCounter(IvrEndpoint endpoint) {
        Long counter = usageCounters.get(endpoint.getId());
        usageCounters.put(endpoint.getId(), counter == null ? 1 : counter + 1);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    //TODO: Remove RTP address from the table
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        return useCase==UseCase.OUTGOING_CALLS? 
                getViewableObjectsForOutgoingCallsUseCase() : getViewableObjectsForIncomingCallsUseCase();
    }
    
    private List<ViewableObject> getViewableObjectsForOutgoingCallsUseCase() {
        TableImpl table = new TableImpl(
                new String[]{
                    terminalColumnMessage, terminalStatusColumnMessage
                    , terminalPoolStatusColumnMessage, usageCountColumnMessage
                    , currentUsageTimeMessage, requesterNodeMessage, requesterStatusMessage});
        long totalUsageCount = 0;
        for (IvrEndpoint endpoint: NodeUtils.getChildsOfType(this, IvrEndpoint.class, false)) {
            String name = getEndpointNodeStatus(endpoint);
            String status = getEndpointState(endpoint);
            RequestInfo ri = busyEndpoints.get(endpoint.getId());
            String poolStatus = ri!=null? "BUSY" : "FREE";
            poolStatus = String.format(STATUS_FORMAT, ri!=null? "blue" : "green", poolStatus);
            long counter = getEndpointUsageCount(endpoint);
            totalUsageCount+=counter;
            String currentUsageTime = null; String requester = null;
            String requesterStatus = null;
            if (ri!=null) {
                currentUsageTime = ""+((System.currentTimeMillis()-ri.terminalUsageTime)/1000);
                requester = ri.getTaskNode().getPath();
                requesterStatus = ri.getStatusMessage();
            }
            table.addRow(new Object[]{
                name, status, poolStatus, counter, currentUsageTime, requester, requesterStatus});
        }
        List<ViewableObject> vos = new ArrayList<ViewableObject>(5);
        addPriorityRatesTable(vos);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+totalUsageCountMessage+": </b>"+totalUsageCount));        
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+terminalsTableTitleMessage+"</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        TableImpl queueTable = new TableImpl(new String[]{requesterNodeMessage, waitingTimeMessage});
        for (RequestInfo ri: queue)
            queueTable.addRow(new Object[]{
                ri.getTaskNode().getPath(), (System.currentTimeMillis()-ri.startTime)/1000});

        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+queueTableTitleMessage+"</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, queueTable));

        return vos;        
    }

    private void addPriorityRatesTable(List<ViewableObject> vos) {
        if (prioritizationType==PrioritizationType.WEIGTH) {
            List<ProbabilisticPriorityQueue.PriorityRate> rates = ((ProbabilisticPriorityQueue)queue).getPriorityRatesInPercent();
            TableImpl ratesTable = new TableImpl(new String[]{priorityColumnMessage, rateColumnMessage});
            for (ProbabilisticPriorityQueue.PriorityRate rate: rates)
                ratesTable.addRow(new Object[]{rate.getPriority(), rate.getRateInPercent()});
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+prioritiesRatesTableTitleMessage+"</b>"));
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, ratesTable));
        }
    }
    
    private List<ViewableObject> getViewableObjectsForIncomingCallsUseCase() {
        List<ViewableObject> vos = new LinkedList<ViewableObject>();
        TableImpl table = new TableImpl(new String[]{terminalColumnMessage, terminalStatusColumnMessage,
            terminalReservedStatus, usageCountColumnMessage, callIdMessage, callCreatedMessage, 
            callDurationMessage, callDescriptionMessage});
        long totalUsages = 0;
        for (IvrEndpointNode endpoint: NodeUtils.getChildsOfType(this, IvrEndpointNode.class, false)) {
            List<CiscoJtapiTerminal.CallInfo> callsInfo = endpoint.getCallsInfo();
            if (callsInfo==null || callsInfo.isEmpty()) {
                callsInfo = new ArrayList<CiscoJtapiTerminal.CallInfo>(1);
                callsInfo.add(null);
            }
            int i=0;
            for (CiscoJtapiTerminal.CallInfo info: callsInfo) {
                Object[] row = new Object[table.getColumnNames().length];
                if (i++ == 0) {
                    row[0]= getEndpointNodeStatus(endpoint);
                    row[1]= getEndpointState(endpoint);
                    row[2]= getReservedStatus(endpoint);
                    long endpointUsages = getEndpointUsageCount(endpoint);
                    totalUsages += endpointUsages;
                    row[3] = endpointUsages;
                }
                if (info!=null) {
                    row[4] = info.getCallId();
                    row[5] = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(info.getCreated());
                    row[6] = info.getDuration();
                    row[7] = info.getDescription();
                }
                table.addRow(row);
            }
        }
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+terminalsTableTitleMessage+"</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+totalUsageCountMessage+": </b>"+totalUsages));
        return vos;
    }
    
    private static String getEndpointNodeStatus(IvrEndpoint endpoint) {
        return endpoint.isStarted()? 
                endpoint.getName() : "<span style=\"color: yellow\">"+endpoint.getName()+"</span>";
    }
    
    private static String getEndpointState(IvrEndpoint endpoint) {
        String status = endpoint.getEndpointState().getIdName();
        return String.format(STATUS_FORMAT, status.equals("IN_SERVICE")? "green" : "blue", status);
    }
    
    private String getReservedStatus(IvrEndpoint endpoint) {
        boolean reserved = reservedEndpoints.containsKey(endpoint);
        return String.format(STATUS_FORMAT, reserved?"blue": "green", reserved? yesMessage: noMessage);
    }
    
    private long getEndpointUsageCount(IvrEndpoint endpoint) {
        Long usages = usageCounters.get(endpoint.getId());
        return usages==null? 0l : usages;
    }
    
    public Boolean getAutoRefresh() {
        return true;
    }

    public Node getTaskNode() {
        return this;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void executeScheduledJob(Scheduler scheduler) {
        if (watchdogRunning.compareAndSet(false, true)) {
            try {
                if (useCase==UseCase.INCOMING_CALLS) runWatchdogForIncomingCallsUseCase();
                else  runWatchdogForOutgoingCallsUseCase();
            } finally {
                watchdogRunning.set(false);
            }
        }
        else if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Can't execute watchdog task. Already executing...");
    }
    
    private void runWatchdogForOutgoingCallsUseCase() {
        try {
            if (lock.writeLock().tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    loadAverage.addDuration(0);
                    Collection<IvrEndpoint> endpoints = NodeUtils.getChildsOfType(this, IvrEndpoint.class, false);
                    int restartedEndpoints = 0;
                    boolean hasFree = false;
                    for (IvrEndpoint endpoint: endpoints) 
                        if (!busyEndpoints.containsKey(endpoint.getId())) {
                            int state = endpoint.getEndpointState().getId();
                            if (endpoint.isStarted() && state==IvrEndpointState.IN_SERVICE)
                                hasFree = true;
                            if (   endpoint.isInitialized()
                                || (   endpoint.isStarted()
                                    && (state!=IvrEndpointState.IN_SERVICE || endpoint.getActiveCallsCount()>0)))
                            {
                                if (isLogLevelEnabled(LogLevel.DEBUG))
                                    debug("Watchdog task. Restarting endpoint ({})", endpoint.getName());
                                if (endpoint.isStarted())
                                    endpoint.stop();
                                if (endpoint.start())
                                    ++restartedEndpoints;
                            }
                        }
                    if (restartedEndpoints>0) {
                        //giving some time for terminals to be IN_SERVICE
                        TimeUnit.SECONDS.sleep(5);
                        if (isLogLevelEnabled(LogLevel.INFO))
                            info("Watchdog task. Successfully restarted ({}) endpoints", restartedEndpoints);
                    }
                    if (hasFree || restartedEndpoints>0)
                        endpointReleased.signal();
                } finally {
                    lock.writeLock().unlock();
                }
            } else if (isLogLevelEnabled(LogLevel.WARN))
                warn("Error executing watchdog task. Timeout acquiring read lock");
        } catch (InterruptedException ex) {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Wait for read lock was interrupted", ex);
        }
    }
    
    private void runWatchdogForIncomingCallsUseCase() {
        for (IvrEndpoint endpoint: NodeUtils.getChildsOfType(this, IvrEndpoint.class, false)) {
            int stateId = endpoint.getEndpointState().getId();
            List<CiscoJtapiTerminal.CallInfo> callsInfo = ((IvrEndpointNode)endpoint).getCallsInfo();
            if (   !reservedEndpoints.containsKey(endpoint)
                && (   (endpoint.isInitialized() && endpoint.isAutoStart())
                    || (endpoint.isStarted() && stateId ==IvrEndpointState.OUT_OF_SERVICE))
                    || (endpoint.isStarted() && stateId==IvrEndpointState.IN_SERVICE && callsInfo!=null && !callsInfo.isEmpty()))
            {
                if (endpoint.isStarted())
                    endpoint.stop();
                endpoint.start();
            }
        }
    }
    
    public void run() {
        managerThreadStoped.set(false);
        try {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task started");
            try {
                while (!stopManagerTask.get())
                    processRequest();
            } catch (InterruptedException e) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    warn("Manager task was interrupted");
                Thread.currentThread().interrupt();
            }
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task stoped");
        } finally {
            clearQueue();
            managerThreadStoped.set(true);
        }
    }

    private void lookupForEndpoint(RequestInfo ri) throws InterruptedException
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Searching for free endpoint for request from ("+ri.getTaskNode().getPath()+")");
        statusMessage.set("Looking up for endpoint for request from ("+ri.getTaskNode().getPath()+")");

        if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) try {
            getAndLockFreeEndpoint(ri);
            if (ri.endpoint==null && auxiliaryPool==null) {
                long timeout = ri.request.getWaitTimeout()-(System.currentTimeMillis()-ri.startTime);
                if (timeout>0) {
                    if (endpointReleased.await(timeout, TimeUnit.MILLISECONDS))
                        getAndLockFreeEndpoint(ri);
                }
            }
            if (isLogLevelEnabled(LogLevel.DEBUG)) {
                if (ri.endpoint==null)
                    debug("No free endpoint found in the pool");
                else
                    debug("Found free endpoint ("+ri.endpoint.getName()+")");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void synchEndpointsWithAddressRanges() throws Exception
    {
        if (addressRanges==null)
            return;
        try{
            String[] ranges = addressRanges.split("\\s*,\\s*");
            HashSet<String> legalAddresses = new HashSet<String>();
            for (String rangeStr: ranges) {
                String[] range = rangeStr.split("\\s*-\\s*");
                for (int addr = Integer.parseInt(range[0]); addr<=Integer.parseInt(range[1]); ++addr){
                    String addrStr = ""+addr;
                    legalAddresses.add(addrStr);
                    IvrEndpointNode term = (IvrEndpointNode) getNode(addrStr);
                    if (term==null){
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            getLogger().debug("Creating endpoint with address ({})", addrStr);
                        term = new IvrEndpointNode();
                        term.setName(addrStr);
                        addAndSaveChildren(term);
                    } else if (Status.STARTED.equals(term.getStatus()))
                        term.stop();
                    term.setAddress(addrStr);
                    term.setEnableIncomingCalls(null);
                    term.setEnableIncomingRtp(null);
                    term.setCodec(null);
                    term.setRtpMaxSendAheadPacketsCount(null);
                    term.setRtpPacketSize(null);
                    term.setCallsRouter(null);
                    term.setShareInboundOutboundPort(null);
                    term.setStartRtpImmediatelly(null);
                    term.setLogLevel(getLogLevel());
                    term.start();
                }
            }
            for (Node term: getNodes())
                if (!legalAddresses.contains(term.getName())) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        getLogger().debug("Removing endpoint ({}) from the pool", term.getName());
                    if (term.isStarted())
                        term.stop();
                    tree.remove(term);
                }
        } catch(Throwable e){
            throw new Exception(String.format("Invalid addressRanges format: (%s)", addressRanges));
        }
    }

    private class RequestInfo implements Task, ProbabilisticPriorityQueue.Entity
    {
        private final EndpointRequest request;
        private final long startTime;
        private long terminalUsageTime;
        private long execStartTime;
        private IvrEndpoint endpoint;
        private long id;

        public RequestInfo(EndpointRequest request) {
            this.request = request;
            startTime = System.currentTimeMillis();
            id = requestSeq.incrementAndGet();
        }

        public void setEndpoint(IvrEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        public Node getTaskNode() {
            return request.getOwner();
        }

        public String getStatusMessage() {
            return request.getStatusMessage();
        }

        public int getPriority() {
            return request.getPriority();
        }

        public void run() {
            execStartTime = System.currentTimeMillis();
            try {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Executing request from ("+getTaskNode().getPath()+")");
                request.processRequest(endpoint);
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Request from ("+getTaskNode().getPath()+") was successfully executed");
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format(
                            "Unexpected error on request processing by requester (%s)"
                            , request.getOwner().getPath())
                        , e);
                releaseEndpoint(endpoint);
            }
        }
    }

    private class RequestComparator implements Comparator<RequestInfo>
    {
        public int compare(RequestInfo o1, RequestInfo o2)
        {
            int res = new Integer(o1.request.getPriority()).compareTo(o2.request.getPriority());
            if (res==0 && o1!=o2)
                res = new Long(o1.id).compareTo(o2.id);
            return res;
        }
    }
    
    private class ReservedEndpointInfo {
        private final ConversationScenario scenario;

        public ReservedEndpointInfo(ConversationScenario scenario) {
            this.scenario = scenario;
        }
    }
    
    private class UnreserveEndpointTask extends AbstractTask {
        private final IvrEndpoint endpoint;

        public UnreserveEndpointTask(IvrEndpoint endpoint) {
            super(IvrEndpointPoolNode.this, String.format("Unreserving endpoint (%s)", endpoint.getName()));
            this.endpoint = endpoint;
        }

        @Override
        public void doRun() throws Exception {
            reservedEndpoints.remove(endpoint);
        }
    }
    
    public static class CallEvent {
        private final CallEventType eventType;
        private final IvrEndpointConversation conversation;

        public CallEvent(CallEventType eventType, IvrEndpointConversation conversation) {
            this.eventType = eventType;
            this.conversation = conversation;
        }        
    }
    
    import static org.onesec.raven.ivr.impl.CallCdrRecordSchemaNode.*;
    
    private static class SendCdrDP implements DataProcessor<CallEvent> {
        private final IvrEndpointPool endpointPool;
        private final RecordSchema cdrSchema;
        private final EnumSet<CallEventType> enabledEvent;
        
        private final Map<IvrEndpointConversation, Record> cdrs = new HashMap<>();

        public SendCdrDP(IvrEndpointPool endpointPool, RecordSchema cdrSchema, EnumSet<CallEventType> enabledEvent) {
            this.endpointPool = endpointPool;
            this.cdrSchema = cdrSchema;
            this.enabledEvent = enabledEvent;
        }

        @Override
        public Object processData(CallEvent event) throws Exception {
            Record cdr;
            switch (event.eventType) {
                case CONVERSATION_STARTED:
                    cdr = createCdrRecord(event.conversation);
                    cdrs.put(event.conversation, cdr);                    
                    trySendCdrToConcumers(cdr, CallEventType.CONVERSATION_STARTED);
                    break;
                case CONVERSATION_FINISHED:
                    cdr = cdrs.remove(event.conversation);
                    
                    break;                    
            }
            if (event.eventType==CallEventType.CONVERSATION_FINISHED) {
                
            }
            return VOID;
        }
        
        private void trySendCdrToConcumers(final Record cdr, final CallEventType eventType) throws Exception {
            if (enabledEvent.contains(eventType)) {
                cdr.setTag("eventType", eventType.name());
                DataSourceHelper.sendDataToConsumers(endpointPool, cdr, new DataContextImpl());                
            }
        }

        private Record createCdrRecord(IvrEndpointConversation conversation) throws RecordException {
            Record cdr = cdrSchema.createRecord();
            
        }

    }
}
