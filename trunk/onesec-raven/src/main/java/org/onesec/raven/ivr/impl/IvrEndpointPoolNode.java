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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=IvrEndpointNode.class)
public class IvrEndpointPoolNode extends BaseNode implements IvrEndpointPool, Viewable, Task, Schedulable
{
    public static final int LOADAVERAGE_INTERVAL = 300000;
    @NotNull @Parameter(defaultValue="100")
    private Integer maxRequestQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler watchdogScheduler;

    @Parameter(readOnly=true)
    private Double loadAverage;

    private ReadWriteLock lock;
    private Condition endpointReleased;
    private Map<Integer, RequestInfo> busyEndpoints;
    private Map<Integer, Long> usageCounters;
//    private LinkedBlockingQueue<RequestInfo> queue;
    private PriorityBlockingQueue<RequestInfo> queue;
    private AtomicBoolean stopManagerTask;
    private AtomicBoolean managerThreadStoped;
    private AtomicReference<String> statusMessage;
    private long timePeriod;
    private long duration;

    @Message
    private static String totalUsageCountMessage;
    @Message
    private static String terminalsTableTitleMessage;
    @Message
    private static String terminalColumnMessage;
    @Message
    private static String terminalStatusColumnMessage;
    @Message
    private static String terminalPoolStatusColumnMessage;
    @Message
    private static String usageCountColumnMessage;
    @Message
    private static String currentUsageTimeMessage;
    @Message
    private static String requesterNodeMessage;
    @Message
    private static String requesterStatusMessage;
    @Message
    private static String queueTableTitleMessage;
    @Message
    private static String queueTimeMessage;
    @Message
    private static String waitingTimeMessage;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantReadWriteLock();
        endpointReleased = lock.writeLock().newCondition();
        busyEndpoints = new HashMap<Integer, RequestInfo>();
        usageCounters = new HashMap<Integer, Long>();
        stopManagerTask = new AtomicBoolean(false);
        statusMessage = new AtomicReference<String>("");
        managerThreadStoped = new AtomicBoolean(true);
        timePeriod = 0;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        if (!managerThreadStoped.get())
            throw new Exception("Can't start pool because of manager task is still running");
        lock = new ReentrantReadWriteLock();
        endpointReleased = lock.writeLock().newCondition();
//        queue = new LinkedBlockingQueue<RequestInfo>(maxRequestQueueSize);
        queue = new PriorityBlockingQueue<RequestInfo>(maxRequestQueueSize, new RequestComparator());
        busyEndpoints.clear();
        usageCounters.clear();
        stopManagerTask.set(false);
        executor.execute(this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        stopManagerTask.set(true);
    }

    public void requestEndpoint(EndpointRequest request) 
    {
        if (!Status.STARTED.equals(getStatus()) || stopManagerTask.get())
            request.processRequest(null);
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("New request added to the queue from ({})", request.getOwner().getPath());
        if (!queue.offer(new RequestInfo(request)))
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "The queue size was exceeded. The request from the (%s) was ignored."
                        , request.getOwner().getPath()));
            request.processRequest(null);
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
        statusMessage.set("Stoping processing requests. Clearing queue...");
        for (RequestInfo ri : queue) 
            ri.request.processRequest(null);
    }

    private void processRequest() throws InterruptedException
    {
        try {
            try{
                statusMessage.set("Waiting for request...");
                RequestInfo ri = queue.poll(10, TimeUnit.SECONDS);
                if (ri != null) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug("Processing request from (" + ri.getTaskNode().getPath() + ")");
                    lookupForEndpoint(ri);
                    if (ri.endpoint == null)
                        ri.request.processRequest(null);
                    else {
                        if (!sendResponse(ri)) {
                            try {
                                ri.request.processRequest(null);
                            } finally {
                                releaseEndpoint(null, 0);
                            }
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

    private boolean sendResponse(RequestInfo requestInfo)
    {
        try
        {
            statusMessage.set("Executing response for request from ("+requestInfo.getTaskNode().getPath()+")");
            executor.execute(requestInfo);
            return true;
        }
        catch(ExecutorServiceException e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error executing task for ("+requestInfo.getTaskNode().getPath()+")", e);
        }

        return false;
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

    public Double getLoadAverage() {
        return loadAverage;
    }

    private synchronized void addDuration(long dur)
    {
        long currTimePeriod = System.currentTimeMillis()/LOADAVERAGE_INTERVAL;
        if (currTimePeriod>timePeriod){
            timePeriod = currTimePeriod;
            if (timePeriod>0){
                loadAverage = duration*100./getChildrenCount()*LOADAVERAGE_INTERVAL;
                loadAverage = new BigDecimal(loadAverage).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
            duration = dur;
        }else
            duration+=dur;
    }

    private void releaseEndpoint(IvrEndpoint endpoint, long duration)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Realesing endpoint (%s) to the pool", endpoint.getName()));
        lock.writeLock().lock();
        try
        {
            addDuration(duration);
            busyEndpoints.remove(endpoint.getId());
            endpointReleased.signal();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Endpoint (%s) successfully realesed to the pool"
                        , endpoint.getName()));
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private void getAndLockFreeEndpoint(RequestInfo requestInfo) throws InterruptedException
    {
        Collection<Node> childs = getChildrens();
        if (childs != null && !childs.isEmpty())
        {
            for (Node child : childs) {
                if (   child instanceof IvrEndpoint
                    && Status.STARTED.equals(child.getStatus())
                    && !busyEndpoints.containsKey(child.getId())
                    && ((IvrEndpoint) child).getEndpointState().getId() == IvrEndpointState.IN_SERVICE)
                {
                    busyEndpoints.put(child.getId(), requestInfo);
                    requestInfo.terminalUsageTime = System.currentTimeMillis();
                    requestInfo.endpoint = (IvrEndpoint) child;
                    Long counter = usageCounters.get(child.getId());
                    usageCounters.put(child.getId(), counter == null ? 1 : counter + 1);
                    return;
                }
            }
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        TableImpl table = new TableImpl(
                new String[]{
                    terminalColumnMessage, terminalStatusColumnMessage
                    , terminalPoolStatusColumnMessage, usageCountColumnMessage
                    , currentUsageTimeMessage, requesterNodeMessage, requesterStatusMessage});

        Collection<Node> childs = getSortedChildrens();
        long totalUsageCount = 0;
        if (childs!=null && !childs.isEmpty())
        {
            lock.readLock().lock();
            try
            {
                String statusFormat = "<span style=\"color: %s\"><b>%s</b></span>";
                for (Node child: childs)
                    if (child instanceof IvrEndpoint)
                    {
                        IvrEndpoint endpoint = (IvrEndpoint) child;
                        String name = endpoint.getName();
                        if (!Status.STARTED.equals(endpoint.getStatus()))
                            name = "<span style=\"color: yellow\">"+name+"</span>";
                        String status = endpoint.getEndpointState().getIdName();
                        status = String.format(
                                statusFormat, status.equals("IN_SERVICE")? "green" : "blue", status);
                        String poolStatus = busyEndpoints.containsKey(endpoint.getId())? "BUSY" : "FREE";
                        poolStatus = String.format(
                                statusFormat, poolStatus.equals("BUSY")? "blue" : "green", poolStatus);
                        Long counter = usageCounters.get(endpoint.getId());
                        String usageCount = counter==null? "0" : counter.toString();
                        if (counter!=null)
                            totalUsageCount+=counter;
                        RequestInfo ri = busyEndpoints.get(endpoint.getId());
                        String currentUsageTime = null; String requester = null; String requesterStatus = null;
                        if (ri!=null)
                        {
                            currentUsageTime = ""+((System.currentTimeMillis()-ri.terminalUsageTime)/1000);
                            requester = ri.getTaskNode().getPath();
                            requesterStatus = ri.getStatusMessage();
                        }

                        table.addRow(new Object[]{
                            name, status, poolStatus, usageCount, currentUsageTime, requester, requesterStatus});
                    }
            }
            finally
            {
                lock.readLock().unlock();
            }
        }
        List<ViewableObject> vos = new ArrayList<ViewableObject>(5);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+terminalsTableTitleMessage+"</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+totalUsageCountMessage+": </b>"+totalUsageCount));

        TableImpl queueTable = new TableImpl(new String[]{requesterNodeMessage, waitingTimeMessage});
        for (RequestInfo ri: queue)
            queueTable.addRow(new Object[]{
                ri.getTaskNode().getPath(), (System.currentTimeMillis()-ri.startTime)/1000});

        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+queueTableTitleMessage+"</b>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, queueTable));

        return vos;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public Node getTaskNode()
    {
        return this;
    }

    public String getStatusMessage()
    {
        return statusMessage.get();
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        try {
            if (lock.writeLock().tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    Collection<Node> childs = getSortedChildrens();
                    if (childs!=null) {
                        int restartedEndpoints = 0;
                        for (Node child: childs)
                            if (   child instanceof IvrEndpoint
                                && !busyEndpoints.containsKey(child.getId())
                                && Status.INITIALIZED==child.getStatus())
                            {
                                if (isLogLevelEnabled(LogLevel.DEBUG))
                                    debug("Watchdog task. Restarting endpoint ({})", child.getName());
                                if (child.start())
                                    ++restartedEndpoints;
                            }
                        if (restartedEndpoints>0)
                        {
                            //giving some time for terminals to be IN_SERVICE
                            TimeUnit.SECONDS.sleep(5);
                            endpointReleased.signal();
                            if (isLogLevelEnabled(LogLevel.INFO))
                                info("Watchdog task. Successfully restarted ({}) endpoints", restartedEndpoints);
                        }
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }else if (isLogLevelEnabled(LogLevel.WARN))
                warn("Error executing watchdog task. Timeout acquiring read lock");

        } catch (InterruptedException ex) {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Wait for read lock was interrupted", ex);
        }
    }

    public void run()
    {
        managerThreadStoped.set(false);
        try
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task started");
            try {
                while (!stopManagerTask.get())
                {
                    processRequest();
                }
            } catch (InterruptedException interruptedException)
            {
                if (isLogLevelEnabled(LogLevel.WARN))
                    warn("Manager task was interrupted");
                Thread.currentThread().interrupt();
            }
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Manager task stoped");
        }
        finally
        {
            clearQueue();
            managerThreadStoped.set(true);
        }
    }

    private void lookupForEndpoint(RequestInfo ri) throws InterruptedException
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Searching for free endpoint for request from ("+ri.getTaskNode().getPath()+")");
        statusMessage.set("Looking up for endpoint for request from ("+ri.getTaskNode().getPath()+")");

        if (lock.writeLock().tryLock(1, TimeUnit.SECONDS))
        {
            try
            {
                getAndLockFreeEndpoint(ri);
                if (ri.endpoint==null)
                {
                    long timeout = ri.request.getWaitTimeout()-(System.currentTimeMillis()-ri.startTime);
                    if (timeout>0)
                    {
                        if (endpointReleased.await(timeout, TimeUnit.MILLISECONDS))
                            getAndLockFreeEndpoint(ri);
                    }
                }
                if (isLogLevelEnabled(LogLevel.DEBUG))
                {
                    if (ri.endpoint==null)
                        debug("No free endpoint found in the pool");
                    else
                        debug("Found free endpoint ("+ri.endpoint.getName()+")");
                }
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }
    }

    private class RequestInfo implements Task
    {
        private final EndpointRequest request;
        private final long startTime;
        private long terminalUsageTime;
        private IvrEndpoint endpoint;

        public RequestInfo(EndpointRequest request)
        {
            this.request = request;
            startTime = System.currentTimeMillis();
        }

        public void setEndpoint(IvrEndpoint endpoint)
        {
            this.endpoint = endpoint;
        }

        public Node getTaskNode()
        {
            return request.getOwner();
        }

        public String getStatusMessage()
        {
            return request.getStatusMessage();
        }

        public void run()
        {
            long durStartTime = System.currentTimeMillis();
            try
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Executing request from ("+getTaskNode().getPath()+")");
                request.processRequest(endpoint);
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Request from ("+getTaskNode().getPath()+") was successfully executed");
            }
            finally
            {
                releaseEndpoint(endpoint, System.currentTimeMillis()-durStartTime);
            }
        }
    }

    private class RequestComparator implements Comparator<RequestInfo>
    {
        public int compare(RequestInfo o1, RequestInfo o2) {
            return new Integer(o1.request.getPriority()).compareTo(o2.request.getPriority());
        }
    }
}
