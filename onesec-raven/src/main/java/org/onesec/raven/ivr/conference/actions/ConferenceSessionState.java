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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.conference.ConferenceSession;
import org.onesec.raven.ivr.conference.ConferenceSessionListener;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceSessionState implements ConferenceSessionListener {
    
    public enum Status {JOINING, JOINED, CONNECTED, REJECTED_DUE_ERROR, REJECTED_DUE_INVALID_ID, STOPPED};
    
    private final static Map<Status, EnumSet<Status>> allowedTransitions;
    
    private final AtomicReference<Status> status = 
            new AtomicReference<ConferenceSessionState.Status>(Status.JOINING);
    private final IvrEndpointConversation conversation;
    private final LoggerHelper logger;
    private volatile ConferenceSession session;
    
    static {
        allowedTransitions = new EnumMap<Status, EnumSet<Status>>(Status.class);
        allowedTransitions.put(Status.JOINING, EnumSet.of(
                Status.JOINED, Status.REJECTED_DUE_ERROR, Status.REJECTED_DUE_INVALID_ID, Status.STOPPED));
        allowedTransitions.put(Status.JOINED, EnumSet.of(
                Status.CONNECTED, Status.STOPPED));
        allowedTransitions.put(Status.CONNECTED, EnumSet.of(
                Status.STOPPED));
    }

    public ConferenceSessionState(IvrEndpointConversation conversation, LoggerHelper logger) {
        this.conversation = conversation;
        this.logger = logger;
    }
    
    
    private synchronized void makeTransition(Status newStatus) {
        EnumSet<Status> allowedStatuses = allowedTransitions.get(status.get());
        if (allowedStatuses==null || !allowedStatuses.contains(newStatus)) {
            if (logger.isWarnEnabled())
                logger.warn(String.format("Not allowed transition from %s to %s. Allowed transitions are: ", 
                        status.get(), newStatus, allowedStatuses));
        } else {
            switch(newStatus) {
                
            }
        }
            
    }

    public void sessionCreated(ConferenceSession session) {
        status.set(Status.JOINED);
        this.session = session;
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    public void sessionCreationError() {
        status.set(Status.REJECTED_DUE_ERROR);
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    public void conferenceStopped() {
        status.set(Status.STOPPED);
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    public void conferenceNotActive() {
        status.set(Status.REJECTED_DUE_ERROR);
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    public void invalidConferenceId(String conferenceId) {
        status.set(Status.REJECTED_DUE_INVALID_ID);
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }

    public void invalidAccessCode() {
        status.set(Status.REJECTED_DUE_INVALID_ID);
        conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
    }
}
