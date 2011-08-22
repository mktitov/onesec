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

import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.queue.CallsCommutationManager;

/**
 * Цель: проинформировать {@link CallsCommutationManager} о том, что оператор готов к коммутации и ждать
 *       до тех пор пока оператор не положит трубку или пока
 * @author Mikhail Titov
 */
public class WaitForCallCommutationAction extends AsyncAction
{
    private final static String NAME = "Wait for commutation action";

    public WaitForCallCommutationAction() {
        super(NAME);
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        CallsCommutationManager commutationManager = (CallsCommutationManager) conversation
                .getConversationScenarioState()
                .getBindings()
                .get(CallsCommutationManager.CALLS_COMMUTATION_MANAGER_BINDING);

        if (commutationManager==null)
            throw new Exception("CallsCommutationManager not found in the conversation scenario state");

        commutationManager.operatorReadyToCommutate(conversation);

        while (!hasCancelRequest() && commutationManager.isCommutationValid())
            TimeUnit.MILLISECONDS.sleep(100);
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
