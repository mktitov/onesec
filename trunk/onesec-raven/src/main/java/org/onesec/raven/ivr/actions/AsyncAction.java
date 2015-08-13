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

package org.onesec.raven.ivr.actions;

import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.ivr.ActionStopListener;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AsyncAction extends AbstractAction implements Task
{
    protected IvrEndpointConversation conversation;
    protected LoggerHelper logger;
    private final AtomicBoolean cancelRequest;
    private volatile ActionStopListener stopListener;
    private volatile boolean waitForExecuted;

    public AsyncAction(String actionName)
    {
        super(actionName);
        cancelRequest = new AtomicBoolean(false);
    }

    @Override
    public void cancel() throws IvrActionException
    {
        if (logger.isDebugEnabled())
            logger.debug("Canceling execution");
        cancelRequest.set(true);
    }

    public boolean hasCancelRequest()
    {
        return cancelRequest.get();
    }
    
    protected void waitForExecutedEvent() {
        waitForExecuted = true;
    }
    
    protected void executed() {
        setStatus(IvrActionStatus.EXECUTED);
        if (stopListener!=null)
            stopListener.actionExecuted(this);        
    }

    @Override
    public void execute(IvrEndpointConversation endpoint, ActionStopListener stopListener, LoggerHelper logger) 
            throws IvrActionException
    {
        this.stopListener = stopListener;
        this.waitForExecuted = false;
        this.conversation = endpoint;
        this.logger = new LoggerHelper(logger, getName()+". ");
        try {
            setStatus(IvrActionStatus.EXECUTING);
            endpoint.getExecutorService().execute(this);
        } catch (ExecutorServiceException ex) {            
            setStatus(IvrActionStatus.EXECUTED);
            throw new IvrActionException("Error executing async action", ex);
        }
    }
    
    //for tests purposes
    public void setLogger(LoggerHelper logger) {
        this.logger = logger;
    }

    @Override
    public Node getTaskNode() {
        return conversation.getOwner();
    }

    @Override
    public void run() {
        try {
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Executing...");
                doExecute(conversation);
                if (logger.isDebugEnabled()) {
                    if (waitForExecuted)
                        logger.debug("Wating for executed event...");
                    else
                        logger.debug(hasCancelRequest()? "Canceled" : "Executed");
                }
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Execution error", ex);
            }
        } finally {
            if (!waitForExecuted) {
                setStatus(IvrActionStatus.EXECUTED);
                if (stopListener!=null)
                    stopListener.actionExecuted(this);
            }
        }
    }

    protected abstract void doExecute(IvrEndpointConversation conversation) throws Exception;
}
