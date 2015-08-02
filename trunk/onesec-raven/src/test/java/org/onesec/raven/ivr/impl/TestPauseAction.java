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

import org.onesec.raven.ivr.ActionStopListener;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class TestPauseAction implements IvrAction
{
    private IvrActionStatus status = IvrActionStatus.WAITING;
    private boolean mustCancel = false;
    private boolean canceled = false;

    public String getName() {
        return "pause action";
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isFlowControlAction() {
        return false;
    }

    public void execute(IvrEndpointConversation conversation, final ActionStopListener stopListener, LoggerHelper logger) throws IvrActionException
    {
        setStatus(IvrActionStatus.EXECUTING);
        new Thread(){
            @Override
            public void run()
            {
                try
                {
                    for (int i=0; i<50; ++i)
                    {
                        if (mustCancel)
                        {
                            canceled = true;
                            return;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                finally
                {
                    setStatus(IvrActionStatus.EXECUTED);
                    if (stopListener!=null)
                        stopListener.actionExecuted(TestPauseAction.this);
                }
            }
        }.start();
    }

    public synchronized IvrActionStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(IvrActionStatus status) {
        this.status = status;
    }

    public synchronized boolean isMustCancel() {
        return mustCancel;
    }

    public synchronized void cancel() throws IvrActionException {
        mustCancel = true;
    }

    public String getStatusMessage() {
        return "pause action";
    }

    public void setLogPrefix(String prefix) {
    }

}
