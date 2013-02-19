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
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AsyncAction extends AbstractAction implements Task
{
    protected IvrEndpointConversation conversation;
    protected Logger logger;
    private final AtomicBoolean cancelRequest;

    public AsyncAction(String actionName)
    {
        super(actionName);
        cancelRequest = new AtomicBoolean(false);
    }

    public void cancel() throws IvrActionException
    {
        cancelRequest.set(true);
    }

    public boolean hasCancelRequest()
    {
        return cancelRequest.get();
    }

    public void execute(IvrEndpointConversation endpoint) throws IvrActionException
    {
        this.conversation = endpoint;
        logger = new LoggerHelper(conversation.getOwner(), logMess(""));
        try
        {
            setStatus(IvrActionStatus.EXECUTING);
            endpoint.getExecutorService().execute(this);
        }
        catch (ExecutorServiceException ex)
        {
            setStatus(IvrActionStatus.EXECUTED);
            throw new IvrActionException("Error executing async action", ex);
        }
    }
    
    //for tests purposes
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Node getTaskNode() {
        return conversation.getOwner();
    }

    public void run() {
        try {
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Executing...");
                doExecute(conversation);
                if (logger.isDebugEnabled())
                    logger.debug("Executed");
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Execution error", ex);
            }
        }
        finally {
            setStatus(IvrActionStatus.EXECUTED);
        }
    }

    protected abstract void doExecute(IvrEndpointConversation conversation) throws Exception;
}
