/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.conference.actions;

import org.onesec.raven.ivr.actions.AbstractAction;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectToConferenceAction extends AbstractAction {
    public final static String NAME = "Connect to conference";

    public ConnectToConferenceAction() {
        super(NAME);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        ConferenceSessionState state = (ConferenceSessionState)message.getConversation()
                .getConversationScenarioState()
                .getBindings()
                .get(JoinToConferenceAction.CONFERENCE_STATE_BINDING);
        if (state!=null)
            state.connect();
        return ACTION_EXECUTED_then_EXECUTE_NEXT;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
}
