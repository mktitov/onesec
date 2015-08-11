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
import org.raven.sched.impl.AbstractTask;

/**
 *
 * @author Mikhail Titov
 */
public class ContinueConversationAction extends AsyncAction {

    public ContinueConversationAction() {
        super("Continue conversation action");
    }

    public ContinueConversationAction(String actionName) {
        super(actionName);
    }

    @Override
    public boolean isFlowControlAction() {
        return true;
    }

    @Override
    protected void doExecute(final IvrEndpointConversation conversation) throws Exception {
        conversation.getConversationScenarioState().switchToNextConversationPoint();
        if (logger.isDebugEnabled())
            logger.debug(
                    "Executing transition to the conversation point ({})"
                    , conversation.getConversationScenarioState().getConversationPoint().getPath());
//        setStatus(IvrActionStatus.EXECUTED);
        conversation.getExecutorService().execute(new AbstractTask(conversation.getOwner(), "Continue conversation") {
            @Override public void doRun() throws Exception {
                conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);                
            }
        });
    }
}
