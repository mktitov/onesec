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

import java.util.Collection;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class IvrActionsExecutor implements Task
{
    public static final int CANCEL_TIMEOUT = 5000;
    private final IvrEndpointNode endpoint;
    private final ExecutorService executorService;
    private Collection<IvrAction> actions;
    private String statusMessage;
    private boolean running;
    private boolean mustCancel;

    public IvrActionsExecutor(IvrEndpointNode endpoint, ExecutorService executorService)
    {
        this.endpoint = endpoint;
        this.executorService = executorService;
        running = false;
    }


    public synchronized void executeActions(Collection<IvrAction> actions)
            throws ExecutorServiceException, InterruptedException
    {
        cancelActionsExecution();
        this.actions = actions;
        mustCancel = false;
        running = true;
        executorService.execute(this);
    }

    public synchronized void cancelActionsExecution() throws InterruptedException
    {
        if (running)
            cancel();
    }

    private synchronized void cancel() throws InterruptedException
    {
        setMustCancel(true);
        wait(CANCEL_TIMEOUT);
    }

    public void run()
    {
        try
        {
            for (IvrAction action: actions)
            {
                try {
                    if (endpoint.isLogLevelEnabled(LogLevel.DEBUG))
                        endpoint.debug(String.format(
                                "ActionsExecutor. Executing action (%s)", action.getName()));
                    action.execute(endpoint);
                } catch (IvrActionException ex) {
                    if (endpoint.isLogLevelEnabled(LogLevel.ERROR))
                        endpoint.error(String.format(
                                "ActionsExecutor. Action (%s) execution error"
                                , action.getName()), ex);
                    return;
                }
                boolean canceling = false;
                while (action.getStatus()!=IvrActionStatus.EXECUTED)
                {
                    if (!canceling && isMustCancel())
                    {
                        if (endpoint.isLogLevelEnabled(LogLevel.DEBUG))
                            endpoint.debug(String.format(
                                    "ActionsExecutor. Canceling (%s) action execution"
                                    , action.getName()));
                        try {
                            action.cancel();
                        } catch (IvrActionException ex) {
                            if (endpoint.isLogLevelEnabled(LogLevel.ERROR))
                                endpoint.error(String.format(
                                    "ActionsExecutor. Error canceling execution of the action (%s)"
                                    , action.getName()), ex);
                        }
                        canceling = true;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        if (endpoint.isLogLevelEnabled(LogLevel.ERROR))
                            endpoint.error(
                                    "ActionsExecutor. Action executor thread was interrupted");
                        return;
                    }
                }
                if (isMustCancel())
                    return;
            }
        }
        finally
        {
            running = false;
            actions = null;
            synchronized(this)
            {
                notifyAll();
            }
        }
    }

    public synchronized boolean isMustCancel() {
        return mustCancel;
    }

    public synchronized void setMustCancel(boolean mustCancel) {
        this.mustCancel = mustCancel;
    }

    public Node getTaskNode() 
    {
        return endpoint;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

}
