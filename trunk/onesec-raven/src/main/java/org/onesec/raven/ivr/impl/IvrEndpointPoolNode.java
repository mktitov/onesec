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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
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
public class IvrEndpointPoolNode extends BaseNode implements IvrEndpointPool, Viewable, Task
{
    private ReadWriteLock lock;
    private Condition newRequestCondition;
    private Condition endpointReleased;
    private Map<Integer, RequestInfo> busyEndpoints;
    private Map<Integer, Long> usageCounters;
    private LinkedBlockingQueue<RequestInfo> queue;
    private AtomicBoolean stopManagerTask;
    private AtomicBoolean managerThreadStoped;
    private AtomicReference<String> statusMessage;

    @NotNull @Parameter(defaultValue="100")
    private Integer maxRequestQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

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
        newRequestCondition = lock.writeLock().newCondition();
        endpointReleased = lock.writeLock().newCondition();
        busyEndpoints = new HashMap<Integer, RequestInfo>();
        usageCounters = new HashMap<Integer, Long>();
        stopManagerTask = new AtomicBoolean(false);
        statusMessage = new AtomicReference<String>("");
        managerThreadStoped = new AtomicBoolean(true);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        if (!managerThreadStoped.get())
            throw new Exception("Can't start pool because of manager task is still running");
        queue = new LinkedBlockingQueue<RequestInfo>(maxRequestQueueSize);
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
            debug("Recieved new request from ("+request.getOwner().getPath()+") to queue");
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

    private void clearQueue() throws InterruptedException
    {
        statusMessage.set("Stoping processing requests. Clearing queue...");
        for (RequestInfo ri : queue) 
            ri.request.processRequest(null);
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
                error("Error executing task for ("+requestInfo.getTaskNode().getPath()+")");
        }

        return false;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Integer getMaxRequestQueueSize() {
        return maxRequestQueueSize;
    }

    public void setMaxRequestQueueSize(Integer maxRequestQueueSize) {
        this.maxRequestQueueSize = maxRequestQueueSize;
    }

    private void releaseEndpoint(IvrEndpoint endpoint)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Realesing endpoint (%s) to the pool", endpoint.getName()));
        lock.writeLock().lock();
        try
        {
            busyEndpoints.remove(endpoint.getId());
            endpointReleased.signal();
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Endpoint (%s) successfully realesed to the pool", endpoint.getName()));
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
                    statusMessage.set("Waiting for request...");
                    RequestInfo ri = queue.poll(10, TimeUnit.SECONDS);
                    if (ri!=null)
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug("Processing request from ("+ri.getTaskNode().getPath()+")");
                        lookupForEndpoint(ri);
                        if (ri.endpoint==null || !sendResponse(ri))
                            ri.request.processRequest(null);
                    }
                    cleanupQueue();
                }
                clearQueue();
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
                releaseEndpoint(endpoint);
            }
        }
    }
}
