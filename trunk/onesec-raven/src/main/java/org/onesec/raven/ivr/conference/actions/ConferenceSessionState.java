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
import static org.onesec.raven.ivr.conference.actions.ConferenceSessionStatus.*;
import org.raven.conv.BindingScope;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
/**
 *
 * @author Mikhail Titov
 */
public class ConferenceSessionState implements ConferenceSessionListener {
    
    private final static Map<ConferenceSessionStatus, EnumSet<ConferenceSessionStatus>> allowedTransitions;
    
    private final AtomicReference<ConferenceSessionStatus> status = 
            new AtomicReference<ConferenceSessionStatus>(ConferenceSessionStatus.JOINING);
    private final IvrEndpointConversation conversation;
    private final LoggerHelper logger;
    private final ExecutorService executor;
    private final Node owner;
    private volatile ConferenceSession session;
    
    static {
        allowedTransitions = new EnumMap<ConferenceSessionStatus, EnumSet<ConferenceSessionStatus>>(ConferenceSessionStatus.class);
        allowedTransitions.put(JOINING, EnumSet.of(JOINED, REJECTED_DUE_ERROR, REJECTED_DUE_INVALID_ID, 
                REJECTED_DUE_TOO_MANY_PARTICIPANTS, STOPPED));
        allowedTransitions.put(JOINED, EnumSet.of(CONNECTED, STOPPED));
        allowedTransitions.put(CONNECTED, EnumSet.of(MUTED, UNMUTED, STOPPED, REJECTED_DUE_ERROR));
        allowedTransitions.put(MUTED, EnumSet.of(UNMUTED, STOPPED, REJECTED_DUE_ERROR));
        allowedTransitions.put(UNMUTED, EnumSet.of(MUTED, STOPPED, REJECTED_DUE_ERROR));
    }

    public ConferenceSessionState(IvrEndpointConversation conversation) 
    {
        this.conversation = conversation;
        this.owner = conversation.getOwner();
        this.executor = conversation.getExecutorService();
        this.logger = new LoggerHelper(owner.getLogLevel(), owner.getName(), "Conference state. ", 
                conversation.getLogger());
    }
    
    private boolean makeTransition(ConferenceSessionStatus newStatus) {
        EnumSet<ConferenceSessionStatus> allowedStatuses = allowedTransitions.get(status.get());
        if (allowedStatuses==null || !allowedStatuses.contains(newStatus)) {
            if (logger.isWarnEnabled())
                logger.warn(String.format("Not allowed transition from %s to %s. Allowed transitions are: ", 
                        status.get(), newStatus, allowedStatuses));
            return false;
        } else {
            if (logger.isDebugEnabled())
                logger.debug(String.format("Status changed from (%s) to (%s)", status.get(), newStatus));
            status.set(newStatus);
            return true;
        }
    }

    public synchronized void sessionCreated(ConferenceSession session) {
        if (makeTransition(JOINED)) {
            this.session = session;
            continueConversation();
        }
    }

    public synchronized void sessionCreationError() {
        if (makeTransition(REJECTED_DUE_ERROR))
            continueConversation();
    }

    public synchronized void conferenceStopped() {
        if (makeTransition(STOPPED))
            continueConversation();
    }

    public synchronized void conferenceNotActive() {
        if (makeTransition(REJECTED_DUE_ERROR))
            continueConversation();
    }

    public synchronized void invalidConferenceId(String conferenceId) {
        if (makeTransition(REJECTED_DUE_INVALID_ID))
            continueConversation();
    }

    public synchronized void invalidAccessCode() {
        if (makeTransition(REJECTED_DUE_INVALID_ID))
            continueConversation();
    }

    public void tooManyParticipants() {
        if (makeTransition(ConferenceSessionStatus.REJECTED_DUE_TOO_MANY_PARTICIPANTS))
            continueConversation();
    }
    
    private void continueConversation() {
        executor.executeQuietly(new AbstractTask(owner, "Conference state. Sending continue conversation event") {
            @Override public void doRun() throws Exception {
                conversation.continueConversation(IvrEndpointConversation.EMPTY_DTMF);
            }
        });
    }

    public ConferenceSession getSession() {
        return session;
    }
    
    public ConferenceSessionStatus getStatus() {
        return status.get();
    }
    
    public synchronized void connect() {
        if (makeTransition(CONNECTED)) {
            conversation.getConversationScenarioState().setBinding(
                    IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true, BindingScope.POINT);
            session.start();
            continueConversation();
        }
    }
    
    public void mute() {
        if (makeTransition(MUTED)) 
            session.mute();
    }
    
    public void unmute() {
        if (makeTransition(UNMUTED)) 
            try {
                session.unmute();
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Unmuting error", ex);
            }
    }
}
