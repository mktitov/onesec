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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.IvrEndpointState;
import org.raven.annotations.NodeClass;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=IvrEndpointNode.class)
public class IvrEndpointPoolNode extends BaseNode implements IvrEndpointPool, Viewable
{
    private ReadWriteLock lock;
    private Condition endpointReleased;
    private Map<Integer, IvrEndpoint> busyEndpoints;
    private Map<Integer, Long> usageCounters;

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
        endpointReleased = lock.writeLock().newCondition();
        busyEndpoints = new HashMap<Integer, IvrEndpoint>();
        usageCounters = new HashMap<Integer, Long>();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        busyEndpoints.clear();
        usageCounters.clear();
    }

    public IvrEndpoint getEndpoint(long timeout)
    {
        lock.writeLock().lock();
        try
        {
            IvrEndpoint endpoint = getAndLockFreeEndpoint();
            if (endpoint==null)
            {
                try
                {
                    if (endpointReleased.await(timeout, TimeUnit.MILLISECONDS))
                        endpoint = getAndLockFreeEndpoint();
                }
                catch (InterruptedException ex)
                {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error("Error waiting for endpoint releasing", ex);
                }
            }
            return endpoint;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void releaseEndpoint(IvrEndpoint endpoint)
    {
        lock.writeLock().lock();
        try
        {
            busyEndpoints.remove(endpoint.getId());
            endpointReleased.signal();
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
}
