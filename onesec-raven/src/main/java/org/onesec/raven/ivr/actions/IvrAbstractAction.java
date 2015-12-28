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
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.raven.dp.FutureCallback;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.RavenPromise;

/**
 *
 * @author Mikhail Titov
 */
@Deprecated
public abstract class IvrAbstractAction implements IvrAction
{
    private final String actionName;
    private IvrActionStatus status;
    private String statusMessage;
//    private String logPrefix;
//    protected volatile LoggerHelper logger;

    public IvrAbstractAction(String actionName)
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
    
    protected void complecteActionByFuture(final RavenPromise<IvrAction, IvrActionException> actionPromise, RavenFuture someFuture) {
        someFuture.onComplete(new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                actionPromise.completeWithValue(IvrAbstractAction.this);
            }
            @Override
            public void onError(Throwable error) {
                actionPromise.completeWithError(new IvrActionException(IvrAbstractAction.this, error));
            }
            @Override
            public void onCanceled() {
                actionPromise.completeWithValue(IvrAbstractAction.this);                
            }
        });
    }

//    public void setLogPrefix(String prefix) {
//        this.logPrefix = prefix;
//    }

//    protected String logMess(String mess, Object... args) {
//        return (logPrefix==null? "" : logPrefix)+"Actions. "+actionName+". "+String.format(mess, args);
//    }

//    @Override
//    public void setLogger(LoggerHelper logger) {
//        this.logger = logger;
//    }
}
