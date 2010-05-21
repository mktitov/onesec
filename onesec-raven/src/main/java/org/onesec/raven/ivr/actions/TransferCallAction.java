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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class TransferCallAction extends AsyncAction
{
    public final static String ACTION_NAME = "Transfer call action";
    
    private final String address;
    private final boolean monitorTransfer;
    private final long callStartTimeout;
    private final long callEndTimeout;

    public TransferCallAction(
            String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
    {
        super(ACTION_NAME);
        this.address = address;
        this.monitorTransfer = monitorTransfer;
        this.callStartTimeout = callStartTimeout;
        this.callEndTimeout = callEndTimeout;
        setStatusMessage("Transfering call to the ("+address+") address");
    }

    public boolean isFlowControlAction() {
        return false;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(logMess("Transfering call to the ("+address+") address"));
        conversation.transfer(address, monitorTransfer, callStartTimeout, callEndTimeout);
    }
}
