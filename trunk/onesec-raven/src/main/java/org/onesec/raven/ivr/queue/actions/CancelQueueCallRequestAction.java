/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.queue.actions;

import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.queue.QueuedCallStatus;

/**
 *
 * @author Mikhail Titov
 */
public class CancelQueueCallRequestAction extends AsyncAction
{
    public static final String ACTION_NAME = "Cancel queue call request";

    public CancelQueueCallRequestAction() {
        super(ACTION_NAME);
    }

    public boolean isFlowControlAction() {
        return false;
    }

    public void doExecute(IvrEndpointConversation conv) throws Exception {
        QueuedCallStatus state = (QueuedCallStatus) conv.getConversationScenarioState().getBindings().get(
                QueueCallAction.QUEUED_CALL_STATUS_BINDING);
        if (state!=null) {
            state.cancel();
            if (logger.isDebugEnabled())
                logger.debug("Queue call request was canceled");
        }
    }

    public void cancel() throws IvrActionException {
    }
}
