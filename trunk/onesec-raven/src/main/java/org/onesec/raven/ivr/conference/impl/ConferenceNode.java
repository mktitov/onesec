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
package org.onesec.raven.ivr.conference.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrDtmfReceivedConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceMixerSession;
import org.onesec.raven.ivr.conference.ConferenceSession;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=PlannedConferencesNode.class)
public class ConferenceNode extends BaseNode implements Conference {
    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    @NotNull @Parameter
    private String conferenceName;
    
    @NotNull @Parameter
    private String accessCode;
    
    @NotNull @Parameter
    private String startTimeStr;
    
    @NotNull @Parameter
    private String endTimeStr;
    
    @NotNull @Parameter
    private Integer channelsCount;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean callbackAllowed;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean manualJoinAllowed;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean joinUnregisteredParticipantAllowed;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean recordConference;
    
    private ConferenceManagerNode getManager() {
        return (ConferenceManagerNode) getParent().getParent();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getManager().checkConferenceNode(this);
    }

    public String getConferenceName() {
        return conferenceName;
    }

    public void setConferenceName(String conferenceName) {
        this.conferenceName = conferenceName;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getStartTimeStr() {
        return startTimeStr;
    }

    public void setStartTimeStr(String startTimeStr) {
        this.startTimeStr = startTimeStr;
    }

    public String getEndTimeStr() {
        return endTimeStr;
    }

    public void setEndTimeStr(String endTimeStr) {
        this.endTimeStr = endTimeStr;
    }

    public Date getStartTime() {
        return parseDate(startTimeStr);
    }

    public void setStartTime(Date startTime) {
        this.startTimeStr = formatDate(startTime);
    }

    public Date getEndTime() {
        return parseDate(endTimeStr);
    }

    public void setEndTime(Date endTime) {
        this.endTimeStr = formatDate(endTime);
    }

    public Integer getChannelsCount() {
        return channelsCount;
    }

    public void setChannelsCount(Integer channelsCount) {
        this.channelsCount = channelsCount;
    }

    public Boolean getCallbackAllowed() {
        return callbackAllowed;
    }

    public void setCallbackAllowed(Boolean callbackAllowed) {
        this.callbackAllowed = callbackAllowed;
    }

    public Boolean getManualJoinAllowed() {
        return manualJoinAllowed;
    }

    public void setManualJoinAllowed(Boolean manualJoinAllowed) {
        this.manualJoinAllowed = manualJoinAllowed;
    }

    public Boolean getJoinUnregisteredParticipantAllowed() {
        return joinUnregisteredParticipantAllowed;
    }

    public void setJoinUnregisteredParticipantAllowed(Boolean joinUnregisteredParticipantAllowed) {
        this.joinUnregisteredParticipantAllowed = joinUnregisteredParticipantAllowed;
    }

    public Boolean getRecordConference() {
        return recordConference;
    }

    public void setRecordConference(Boolean recordConference) {
        this.recordConference = recordConference;
    }

    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(date);
        } catch (ParseException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error parsing date from string: "+date);
            return null;
        }
    }
    
    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
    
    private class ConferenceController {
        private final ConferenceNode conferenceNode;
        private final RealTimeConferenceMixer mixer;
        private final Set<ConferenceSessionImpl> sessions = new ConcurrentSkipListSet<ConferenceSessionImpl>();
        private final LoggerHelper logger;

        public ConferenceController(ConferenceNode conferenceNode, CodecManager codecManager, 
                ExecutorService executor, int noiseLevel, double maxGainCoef) throws IOException 
        {
            this.conferenceNode = conferenceNode;
            this.logger = new LoggerHelper(conferenceNode, "Conference. ");
            mixer = new RealTimeConferenceMixer(codecManager, conferenceNode, logger, executor, noiseLevel, 
                    maxGainCoef);
            mixer.connect();
            mixer.start();
        }
        
        public ConferenceSession join(IvrEndpointConversation conv) throws Exception {
            ConferenceSessionImpl session = new ConferenceSessionImpl(this, conv, mixer, logger);
            sessions.add(session);
            return session;
        }
        
        public void removeSession(ConferenceSessionImpl session) {
            sessions.remove(session);
        }
    }
    
    private enum SessionStatus {INITIALIZED, STARTED, STOPPED};
    
    private class ConferenceSessionImpl implements ConferenceSession, IvrEndpointConversationListener {
        private final IvrEndpointConversation conversation;
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicReference<SessionStatus> status = 
                new AtomicReference<SessionStatus>(SessionStatus.INITIALIZED);
        private final AtomicBoolean muted = new AtomicBoolean();
        private final ConferenceMixerSession mixerSession;
        private final AtomicReference<RtpListener> rtpListener = new AtomicReference<RtpListener>();
        private final LoggerHelper logger;
        private final ConferenceController controller;

        public ConferenceSessionImpl(ConferenceController controller, IvrEndpointConversation conversation, 
                RealTimeConferenceMixer mixer, LoggerHelper logger) throws Exception 
        {
            this.controller = controller;
            this.conversation = conversation;
            this.logger = new LoggerHelper(logger, "["+conversation.getCallingNumber()+"]. ");
            this.mixerSession = mixer.addParticipant(conversation.getCallingNumber(), null);
        }
        
        public void start() throws Exception {
            if (status.compareAndSet(SessionStatus.INITIALIZED, SessionStatus.STARTED)) {
                conversation.getAudioStream().addSource(mixerSession.getConferenceAudioSource());
                if (logger.isDebugEnabled()) {
                    logger.debug("Conference audio routed to the participant");
                    logger.debug("Connected to the conference");
                }
            }
        }
        public void stop() {
            if (status.compareAndSet(SessionStatus.STARTED, SessionStatus.STOPPED)) {
                stopMixerSession();
            } else if (status.compareAndSet(SessionStatus.INITIALIZED, SessionStatus.STOPPED)) {
                stopMixerSession();
            }
        }
        
        private void stopMixerSession() {
            mixerSession.stopSession();
            controller.removeSession(this);
            if (logger.isDebugEnabled())
                logger.debug("Disconnected");
        }
        
        public void mute() {
            if (status.get()==SessionStatus.STARTED) {
                rtpListener.set(null);
                replaceDataSource(null, null);
                if (logger.isDebugEnabled())
                    logger.debug("Muted");
            }
        }
        public void unmute() throws Exception {
            if (status.get()==SessionStatus.STARTED) {
                try {
                    if (logger.isDebugEnabled())
                        logger.debug("Unmuting...");
                    RtpListener listener = new RtpListener();
                    rtpListener.set(listener);
                    conversation.getIncomingRtpStream().addDataSourceListener(listener, null);
                } catch (RtpStreamException e) {
                    if (logger.isErrorEnabled())
                        logger.error("Error unmuting participant", e);
                    throw e;
                }
            }
        }
        
        private void replaceDataSource(RtpListener listener, PushBufferDataSource ds) {
            if (status.get()==SessionStatus.STARTED && rtpListener.get()==listener) {
                try {
                    mixerSession.replaceParticipantAudio(ds);
                    if (ds!=null && logger.isDebugEnabled())
                        logger.debug("Unmuted");
                } catch (Exception ex) {
                    if (logger.isErrorEnabled())
                        logger.error("Error replacing participant incoming rtp stream in mixer", ex);
                }
            }
        }

        public void listenerAdded(IvrEndpointConversationEvent event) { 
            if (event.getConversation().getState().getId()!=IvrEndpointConversationState.TALKING)
                stop();
        }

        public void conversationStopped(IvrEndpointConversationStoppedEvent event) { 
            stop();
        }
        public void conversationStarted(IvrEndpointConversationEvent event) { }
        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) { }
        public void incomingRtpStarted(IvrIncomingRtpStartedEvent event) { }
        public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event) { }
        public void dtmfReceived(IvrDtmfReceivedConversationEvent event) { }
        
        private class RtpListener implements IncomingRtpStreamDataSourceListener {
            public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
                replaceDataSource(this, (PushBufferDataSource)dataSource);
            }

            public void streamClosing(IncomingRtpStream stream) { }
        }
    }    
}
