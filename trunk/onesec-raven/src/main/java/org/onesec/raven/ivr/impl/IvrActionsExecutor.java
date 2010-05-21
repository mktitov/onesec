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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
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
    private final IvrEndpointConversation endpoint;
    private final ExecutorService executorService;
    private Collection<IvrAction> actions;
    private String statusMessage;
    private boolean running;
    private boolean mustCancel;
    private String logPrefix;
    private Set<Character> defferedDtmfs;
    private List<Character> collectedDtmfs;

    public IvrActionsExecutor(IvrEndpointConversation endpoint, ExecutorService executorService)
    {
        this.endpoint = endpoint;
        this.executorService = executorService;
        defferedDtmfs = new HashSet<Character>();
        collectedDtmfs = new ArrayList<Character>();
        running = false;
    }

    public synchronized void executeActions(Collection<IvrAction> actions)
            throws ExecutorServiceException, InterruptedException
    {
        cancelActionsExecution();
        for (IvrAction action: actions)
            if (action instanceof DtmfProcessPointAction)
                for (char c: ((DtmfProcessPointAction)action).getDtmfs().toCharArray())
                    defferedDtmfs.add(c);
        this.actions = actions;
        mustCancel = false;
        running = true;
        executorService.execute(this);
    }

    public synchronized boolean hasDtmfProcessPoint(char dtmf)
    {
        if (defferedDtmfs.contains(dtmf)){
            collectedDtmfs.add(dtmf);
            return true;
        }else
            return false;
    }

    public List<Character> getCollectedDtmfs() {
        return collectedDtmfs;
    }

    public synchronized void cancelActionsExecution() throws InterruptedException
    {
        defferedDtmfs.clear();
        collectedDtmfs.clear();
        if (running){
            cancel();
        }
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
                action.setLogPrefix(logPrefix);
                boolean executeAction = true;
                try {
                    statusMessage = String.format("Executing action (%s)", action.getName());
                    if (endpoint.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                        endpoint.getOwner().getLogger().debug(getStatusMessage());
                    if (action instanceof DtmfProcessPointAction)
                        synchronized(this){
                            List dtmfs = new ArrayList(collectedDtmfs.size());
                            String validDtmfs = ((DtmfProcessPointAction)action).getDtmfs();
                            for (char c: collectedDtmfs)
                                if (validDtmfs.indexOf(c)>=0)
                                    dtmfs.add(c);
                            for (char c: validDtmfs.toCharArray())
                                defferedDtmfs.remove(c);
                            executeAction = dtmfs.size()>0;
                            if (executeAction)
                                endpoint.getConversationScenarioState().getBindings().put(
                                        IvrEndpointConversation.DTMFS_BINDING, dtmfs);
                        }
                    if (executeAction)
                        action.execute(endpoint);
                    else {
                        statusMessage = String.format("Skipping execution of action (%s)", action.getName());
                        if (endpoint.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                            endpoint.getOwner().getLogger().debug(getStatusMessage());
                    }
                } catch (Throwable ex) {
                    statusMessage = String.format("Action (%s) execution error", action.getName());
                    if (endpoint.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                        endpoint.getOwner().getLogger().error(getStatusMessage(), ex);
                    return;
                }
                boolean canceling = false;
                while (executeAction && action.getStatus()!=IvrActionStatus.EXECUTED)
                {
                    if (!canceling && isMustCancel())
                    {
                        statusMessage = String.format(
                                "Canceling (%s) action execution", action.getName());
                        if (endpoint.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                            endpoint.getOwner().getLogger().debug(getStatusMessage());
                        try {
                            action.cancel();
                        } catch (IvrActionException ex) {
                            statusMessage = String.format(
                                    "Error canceling execution of the action (%s)"
                                    , action.getName());
                            if (endpoint.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                                endpoint.getOwner().getLogger().error(getStatusMessage(), ex);
                        }
                        canceling = true;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        statusMessage = "Executor thread was interrupted";
                        if (endpoint.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                            endpoint.getOwner().getLogger().error(getStatusMessage());
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (isMustCancel() || (executeAction && action.isFlowControlAction()))
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

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public synchronized boolean isMustCancel() {
        return mustCancel;
    }

    public synchronized void setMustCancel(boolean mustCancel) {
        this.mustCancel = mustCancel;
    }

    public Node getTaskNode() 
    {
        return endpoint.getOwner();
    }

    public String getStatusMessage()
    {
        return logMess(statusMessage);
    }

    private String logMess(String mess, Object... args)
    {
        return (logPrefix==null? "" : logPrefix)+"Actions. <<Executor>>. "+String.format(mess, args);
    }
}
