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

import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpoint;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PauseAction implements IvrAction
{
    public static final String ACTION_NAME = "Pause action";
    private boolean mustCancel = false;
    private final long interval;
    private IvrActionStatus status;

    public PauseAction(long interval)
    {
        this.interval = interval;
        status = IvrActionStatus.WAITING;
    }
    
    public String getName()
    {
        return ACTION_NAME;
    }

    public void execute(IvrEndpoint endpoint) throws IvrActionException
    {
        setStatus(IvrActionStatus.EXECUTING);
        try
        {
            endpoint.getExecutorService().execute(new PauseActionTask(endpoint));
        }
        catch (ExecutorServiceException ex)
        {
            setStatus(IvrActionStatus.EXECUTED);
            throw new IvrActionException("Error while executing pause action", ex);
        }
    }

    public synchronized boolean isMustCancel()
    {
        return mustCancel;
    }

    public synchronized IvrActionStatus getStatus()
    {
        return status;
    }

    public synchronized void setStatus(IvrActionStatus status)
    {
        this.status = status;
    }

    public synchronized void cancel() throws IvrActionException
    {
        mustCancel = true;
    }

    public String getStatusMessage()
    {
        return "Pausing";
    }

    private class PauseActionTask implements Task
    {
        private final Node initiator;

        public PauseActionTask(Node initiator) {
            this.initiator = initiator;
        }

        public Node getTaskNode() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getStatusMessage() {
            return PauseAction.this.getStatusMessage();
        }

        public void run()
        {
            try
            {
                long start = System.currentTimeMillis();
                do {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException ex) {
                        if (initiator.isLogLevelEnabled(LogLevel.ERROR))
                            initiator.getLogger().error("Pause action was itnterrupted");
                    }

                } while (System.currentTimeMillis()-start<interval && !isMustCancel());
            }
            finally
            {
                setStatus(IvrActionStatus.EXECUTED);
            }
        }
    }
}
