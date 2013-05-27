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
import org.onesec.raven.ivr.conference.ConferenceManager;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public class JoinToConferenceAction extends AsyncAction {
    public final static String NAME = "Join to conference action";
    public final static String CONFERENCE_STATE_BINDING = "conferenceState";
//    public final static String CONFERENCE_ID_BINDING = "conferenceId";
//    public final static String CONFERENCE_ACCESS_CODE_BINDING = "conferenceAccessCode";
    
    private final ConferenceManager conferenceManager;
    private final Boolean autoConnect;
    private final Boolean autoUnmute;
    private final String conferenceId;
    private final String accessCode;

    public JoinToConferenceAction(ConferenceManager conferenceManager, Boolean autoConnect, 
            Boolean autoUnmute, String conferenceId, String accessCode) {
        super(NAME);
        this.conferenceManager = conferenceManager;
        this.autoConnect = autoConnect;
        this.autoUnmute = autoUnmute;
        this.conferenceId = conferenceId;
        this.accessCode = accessCode;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        ConversationScenarioState conversationState = conversation.getConversationScenarioState();
        Bindings bindings = conversationState.getBindings();
//        String conferenceId = (String) bindings.get(CONFERENCE_ID_BINDING);
//        String conferenceAccessCode = (String) bindings.get(CONFERENCE_ACCESS_CODE_BINDING);
        ConferenceSessionState state = (ConferenceSessionState) conversationState.getBindings().get(
                CONFERENCE_STATE_BINDING);
        if (state==null) {
            if (logger.isDebugEnabled())
                logger.debug("Connectiong to conference...");
            state = new ConferenceSessionState(conversation);
            conversationState.setBinding(CONFERENCE_STATE_BINDING, state, BindingScope.POINT);
            conferenceManager.join(conversation, conferenceId, accessCode, state);
        } else {
            if (autoConnect && state.getStatus()==ConferenceSessionStatus.JOINED)
                state.connect();
            if (autoUnmute && state.getStatus()==ConferenceSessionStatus.CONNECTED)
                state.unmute();
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
