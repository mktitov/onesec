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

package org.onesec.raven.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.core.provider.ProviderConfiguration;
import org.onesec.raven.RavenProviderConfigurator;
import org.raven.annotations.NodeClass;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ProvidersNode extends BaseNode
{
    @Service
    private static RavenProviderConfigurator providerService;

    private Lock lock;

    public final static String NAME = "Providers";

    public ProvidersNode()
    {
        super(NAME);
        setStartAfterChildrens(true);
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantLock();
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        super.nodeStatusChanged(node, oldStatus, newStatus);
        
        if (!getStatus().equals(Status.STARTED))
            return;
        if (node instanceof ProviderConfiguration)
        {
            if (lock.tryLock())
            {
                try
                {
                    ProviderConfiguration configuration = (ProviderConfiguration) node;
                    switch (newStatus)
                    {
                        case STARTED :
                            providerService.add(configuration);
                                if (isLogLevelEnabled(LogLevel.INFO))
                                    info(String.format(
                                            "Provider configuration (%s) ADDED", node.getName()));
                            break;
                        case INITIALIZED :
                            if (oldStatus.equals(Status.STARTED))
                            {
                                providerService.remove(configuration);
                                if (isLogLevelEnabled(LogLevel.INFO))
                                    info(String.format(
                                            "Provider configuration (%s) REMOVED", node.getName()));
                            }
                            break;
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }
            else
                error("Error aquiring lock on child nodes");
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        providerService.setProvidersNode(this);
        processProviderOperation(true);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        processProviderOperation(false);
        providerService.setProvidersNode(null);
    }

    public Collection<ProviderConfiguration> getProviders()
    {
        if (!getStatus().equals(Status.STARTED))
            return null;

        Collection<Node> childs = getEffectiveChildrens();
        Collection<ProviderConfiguration> providers = null;
        if (childs!=null && !childs.isEmpty())
        {
            providers = new ArrayList<ProviderConfiguration>(childs.size());
            for (Node child: childs)
                if (child instanceof ProviderConfiguration)
                    providers.add((ProviderConfiguration) child);
            
        }

        return providers==null || providers.isEmpty()? null : providers;
    }

    private void processProviderOperation(boolean addOperation) throws Exception
    {
        if (lock.tryLock())
        {
            try
            {
                Collection<Node> childs = getSortedChildrens();
                if (childs!=null && !childs.isEmpty())
                    for (Node child: childs)
                        if (   child.getStatus().equals(Status.STARTED)
                            && child instanceof ProviderConfiguration)
                        {
                            if (addOperation)
                                providerService.add((ProviderConfiguration) child);
                            else
                                providerService.remove((ProviderConfiguration) child);
                            if (isLogLevelEnabled(LogLevel.INFO))
                                info(String.format(
                                        "Provider configuration (%s) %s"
                                        , child.getName()
                                        , addOperation? "ADDED" : "REMOVED"));
                        }
            }
            finally
            {
                lock.unlock();
            }
        }
        else
            throw new Exception("Error aquire lock on child nodes (provider configuration)");
    }
}
