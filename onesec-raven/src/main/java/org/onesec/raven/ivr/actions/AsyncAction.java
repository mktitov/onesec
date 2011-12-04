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

/**
 *
 * @author Mikhail Titov
 */
public abstract class AsyncAction extends AbstractAction implements Task
{
    protected IvrEndpointConversation conversation;
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

    public Node getTaskNode() {
        return conversation.getOwner();
    }

    public void run() {
        try{
            try {
                if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                    conversation.getOwner().getLogger().debug(logMess("Executing..."));
                doExecute(conversation);
                if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                    conversation.getOwner().getLogger().debug(logMess("Executed"));
            }
            catch (Exception ex) {
                if (conversation.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                    conversation.getOwner().getLogger().error(logMess("Execution error"), ex);
            }
        }
        finally {
            setStatus(IvrActionStatus.EXECUTED);
        }
    }

    protected abstract void doExecute(IvrEndpointConversation conversation) throws Exception;
}
