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

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.raven.conv.ConversationScenarioState;

/**
 *
 * @author Mikhail Titov
 */
public class MuteConferenceParticipantAction extends AsyncAction {
    public final static String NAME = "Mute conference participant";

    public MuteConferenceParticipantAction() {
        super(NAME);
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        ConferenceSessionState state = (ConferenceSessionState)conversation
                .getConversationScenarioState()
                .getBindings()
                .get(JoinToConferenceAction.CONFERENCE_STATE_BINDING);
        if (state!=null)
            state.mute();
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
