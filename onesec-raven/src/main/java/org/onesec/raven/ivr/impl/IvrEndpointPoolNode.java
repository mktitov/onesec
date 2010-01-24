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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
public class IvrEndpointPoolNode extends BaseNode implements IvrEndpointPool, Viewable, Task
{
    private ReadWriteLock lock;
    private Condition newRequestCondition;
    private Condition endpointReleased;
    private Map<Integer, IvrEndpoint> busyEndpoints;
    private Map<Integer, Long> usageCounters;
    private LinkedList<RequestInfo> queue;
    private AtomicBoolean stopManagerTask;
    private AtomicBoolean managerThreadStoped;
    private AtomicReference<String> statusMessage;

    @NotNull @Parameter(defaultValue="100")
    private Integer maxRequestQueueSize;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler cleanupScheduler;
    
    @Message
    private static String terminalColumnMessage;
    @Message
    private static String terminalStatusColumnMessage;
    @Message
    private static String terminalPoolStatusColumnMessage;
    @Message
    private static String usageCountColumnMessage;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantReadWriteLock();
        newRequestCondition = lock.writeLock().newCondition();
        endpointReleased = lock.writeLock().newCondition();
        busyEndpoints = new HashMap<Integer, IvrEndpoint>();
        usageCounters = new HashMap<Integer, Long>();
        queue = new LinkedList<RequestInfo>();
        stopManagerTask = new AtomicBoolean(Boolean.FALSE);
        statusMessage = new AtomicReference<String>("");
        managerThreadStoped = new AtomicBoolean(true);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        if (!managerThreadStoped.get())
            throw new Exception("Can't start pool because of manager task is still running");
        queue.clear();
        busyEndpoints.clear();
        usageCounters.clear();
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
        lock.writeLock().lock();
        try
        {
            if (queue.size()<maxRequestQueueSize)
            {
                queue.offer(new RequestInfo(request));
                newRequestCondition.signal();
            }
            else
                request.processRequest(null);
        }
        finally
        {
            lock.writeLock().unlock();
        }
        
    }

    private void cleanupQueue()
    {
        statusMessage.set("Cleaning up queue from timeouted requests");
        RequestInfo ri;
        ListIterator<RequestInfo> it = queue.listIterator();
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
        if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
            try {
                for (RequestInfo ri : queue) {
                    ri.request.processRequest(null);
                }
            } finally {
                lock.writeLock().unlock();
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
                error("Error executing task for ("+requestInfo.getTaskNode().getPath()+")");
        }

        return false;
    }

    public Scheduler getCleanupScheduler() {
        return cleanupScheduler;
    }

    public void setCleanupScheduler(Scheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
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
        lock.writeLock().lock();
        try
        {
            busyEndpoints.remove(endpoint.getId());
            newRequestCondition.signal();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private IvrEndpoint getAndLockFreeEndpoint()
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (   child instanceof IvrEndpoint
                    && Status.STARTED.equals(child.getStatus())
                    && !busyEndpoints.containsKey(child.getId())
                    && ((IvrEndpoint)child).getEndpointState().getId()==IvrEndpointState.IN_SERVICE)
                {
                    busyEndpoints.put(child.getId(), (IvrEndpoint)child);
                    Long counter = usageCounters.get(child.getId());
                    usageCounters.put(child.getId(), counter==null? 1 : counter+1);
                    return (IvrEndpoint)child;
                }
        return null;
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
                    , terminalPoolStatusColumnMessage, usageCountColumnMessage});

        Collection<Node> childs = getSortedChildrens();
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

                        table.addRow(new Object[]{name, status, poolStatus, usageCount});
                    }
            }
            finally
            {
                lock.readLock().unlock();
            }
        }

        ViewableObject vo = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        
        return Arrays.asList(vo);
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
            try {
                while (!stopManagerTask.get())
                {
                    statusMessage.set("Waiting for request...");
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS))
                    {
                        try
                        {
                            newRequestCondition.await(10, TimeUnit.SECONDS);
                            RequestInfo ri = queue.poll();
                            if (ri!=null)
                            {
                                lookupForEndpoint(ri);
                                if (ri.endpoint==null)
                                    ri.request.processRequest(null);
                                else if (!sendResponse(ri))
                                    queue.offer(ri);
                            }
                            cleanupQueue();
                        }
                        finally
                        {
                            lock.writeLock().unlock();
                        }
                    }
                }
                clearQueue();
            } catch (InterruptedException interruptedException)
            {
                if (isLogLevelEnabled(LogLevel.WARN))
                    warn("Manager task was interrupted");
                Thread.currentThread().interrupt();
            }
        }
        finally
        {
            managerThreadStoped.set(true);
        }
    }

    private void lookupForEndpoint(RequestInfo ri) throws InterruptedException
    {
        statusMessage.set("Looking up for endpoint for request from ("+ri.getTaskNode().getPath()+")");
        IvrEndpoint endpoint = getAndLockFreeEndpoint();
        if (endpoint==null)
        {
            long timeout = ri.request.getWaitTimeout()-(System.currentTimeMillis()-ri.startTime);
            if (timeout>0)
            {
                if (endpointReleased.await(timeout, TimeUnit.DAYS))
                    endpoint = getAndLockFreeEndpoint();
            }
        }
        ri.endpoint = endpoint;
    }
        

    private class RequestInfo implements Task
    {
        private final EndpointRequest request;
        private final long startTime;
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
                request.processRequest(endpoint);
            }
            finally
            {
                releaseEndpoint(endpoint);
            }
        }
    }
}
