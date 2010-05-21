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

import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionStatus;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAction implements IvrAction
{
    private final String actionName;
    private IvrActionStatus status;
    private String statusMessage;
    private String logPrefix;

    public AbstractAction(String actionName)
    {
        this.actionName = actionName;
        status = IvrActionStatus.WAITING;
        statusMessage = "Waiting for execution";
    }

    public String getName()
    {
        return actionName;
    }

    public IvrActionStatus getStatus()
    {
        return status;
    }

    public void setStatus(IvrActionStatus status)
    {
        this.status = status;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    public void setLogPrefix(String prefix) {
        this.logPrefix = prefix;
    }

    protected String logMess(String mess, Object... args)
    {
        return (logPrefix==null? "" : logPrefix)+"Actions. "+actionName+". "+String.format(mess, args);
    }

}
