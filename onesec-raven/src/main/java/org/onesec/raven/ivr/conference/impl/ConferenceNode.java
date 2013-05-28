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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.onesec.raven.ivr.conference.ConferenceManager;
import org.onesec.raven.ivr.conference.ConferenceMixerSession;
import org.onesec.raven.ivr.conference.ConferenceSession;
import org.onesec.raven.ivr.conference.ConferenceSessionListener;
import org.onesec.raven.ivr.impl.AudioFileWriterDataSource;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=PlannedConferencesNode.class)
public class ConferenceNode extends BaseNode implements Conference {
    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    @Service
    private CodecManager codecManager;
    
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
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter
    private Integer noiseLevel;
    
    @NotNull @Parameter
    private Integer maxGainCoef;
    
    
    private AtomicMarkableReference<ConferenceController> controller;
    private ConferenceRecordingsNode recordingsNode;
    
    private ConferenceManager getManager() {
        return (ConferenceManager) getParent().getParent();
    }

    @Override
    protected void initFields() {
        super.initFields();
        controller = new AtomicMarkableReference<ConferenceController>(null, false);
    }
    
    private void initNodes(boolean start) {
        recordingsNode = (ConferenceRecordingsNode) getNode(ConferenceRecordingsNode.NAME);
        if (recordingsNode==null) {
            recordingsNode = new ConferenceRecordingsNode();
            addAndSaveChildren(recordingsNode);
            if (start) 
                recordingsNode.start();            
        }
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
        getManager().checkConferenceNode(this);
        controller.set(null, true);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        final ConferenceController _controller;
        synchronized(this) {
            _controller = controller.getReference();
            controller.set(null, false);
        }
        if (_controller!=null) 
            _controller.stop();
    }
    
    public void join(final IvrEndpointConversation conversation, final String accessCode, 
            final ConferenceSessionListener listener)
    {
        long curTime = System.currentTimeMillis();
        if (!isStarted() || curTime<getStartTime().getTime() || curTime>getEndTime().getTime()) 
            sendConferenceNotActive(listener);
        else {
            if (!this.accessCode.equals(accessCode))
                executor.executeQuietly(new AbstractTask(this, "Pushing 'invalid access code' to conversation") {
                    @Override public void doRun() throws Exception {
                        listener.invalidAccessCode();
                    }
                });
            else {
                final ConferenceController _controller = getOrCreateConferenceController();
                if (_controller==null) 
                    sendConferenceNotActive(listener);
                else
                    _controller.join(conversation, listener);
            }
        }
    }
    
    private void sendConferenceNotActive(final ConferenceSessionListener listener) {
        executor.executeQuietly(new AbstractTask(this, "Pushing 'conference not active' to conversation") {
            @Override public void doRun() throws Exception {
                listener.conferenceNotActive();
            }
        });
    }
    
    private ConferenceController getOrCreateConferenceController() {
        boolean[] holder = new boolean[1];
        ConferenceController _controller = controller.get(holder);
        if (holder[0] && _controller!=null)
            return _controller;
        synchronized(this) {
            try {
                _controller = new ConferenceController(this, codecManager, executor, channelsCount, 
                        channelsCount);
                if (!controller.compareAndSet(null, _controller, true, true)) _controller.stop();
                else {
                    final ConferenceController finalController = _controller;
                    executor.executeQuietly(getEndTime().getTime()-System.currentTimeMillis(), 
                        new AbstractTask(this, "Stop conference task") {
                            @Override public void doRun() throws Exception {
                                finalController.stop();
                            }
                        });
                }
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR)) 
                    getLogger().error("Conference creating error", e);
            }
        }
        return controller.getReference();
    }

    public Integer getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(Integer noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Integer getMaxGainCoef() {
        return maxGainCoef;
    }

    public void setMaxGainCoef(Integer maxGainCoef) {
        this.maxGainCoef = maxGainCoef;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
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
    
    private synchronized ConferenceRecordingNode createRecordingNode() {
        int i = recordingsNode.getNodesCount()+1;
        while (recordingsNode.getNode(""+i)!=null)
            ++i;        
        ConferenceRecordingNode node = new ConferenceRecordingNode();
        node.setName(""+i);
        recordingsNode.addAndSaveChildren(node);
        node.start();
        return node;
    }
    
    private class ConferenceController {
        private final RealTimeConferenceMixer mixer;
        private final Map<Integer, ConferenceSessionImpl> sessions = 
                new ConcurrentHashMap<Integer, ConferenceSessionImpl>();
        private final LoggerHelper logger;
        private final ExecutorService executor;
        private final AtomicBoolean stopped = new AtomicBoolean();
        private final AtomicInteger idGenerator = new AtomicInteger();

        public ConferenceController(ConferenceNode conferenceNode, CodecManager codecManager, 
                ExecutorService executor, int noiseLevel, double maxGainCoef) throws IOException 
        {
            this.executor = executor;
            this.logger = new LoggerHelper(conferenceNode, "Conference. ");
            mixer = new RealTimeConferenceMixer(codecManager, conferenceNode, logger, executor, noiseLevel, 
                    maxGainCoef);
            mixer.connect();
            mixer.start();
        }
        
        public void join(IvrEndpointConversation conv, final ConferenceSessionListener listener) {
            try {
                final Integer id = idGenerator.incrementAndGet();
                final ConferenceSessionImpl session = new ConferenceSessionImpl(this, id, conv, mixer, logger, listener);
                conv.addConversationListener(session);
                sessions.put(id, session);
                executor.executeQuietly(new AbstractTask(ConferenceNode.this, "Pushing 'conference session created' to the conversation") {
                    @Override public void doRun() throws Exception {
                        listener.sessionCreated(session);
                    }
                });
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Error creating session for conversation: "+conv, ex);
                executor.executeQuietly(new AbstractTask(ConferenceNode.this, "Pushing 'session creation error' to conference") {
                    @Override public void doRun() throws Exception {
                        listener.sessionCreationError();
                    }
                });
            }
        }
        
        public void removeSession(ConferenceSessionImpl session) {
            if (!stopped.get())
                sessions.remove(session.id);
        }
        
        public void stop() {
            if (stopped.compareAndSet(false, true))
                executor.executeQuietly(new AbstractTask(ConferenceNode.this, "Stopping conference controller") {
                    @Override public void doRun() throws Exception {
                        for (ConferenceSessionImpl session: sessions.values()) {
                            session.stop();
                            session.sessionListener.conferenceStopped();
                        }
                        sessions.clear();
                    }
                });
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
        private final ConferenceSessionListener sessionListener;
        private final Integer id;

        public ConferenceSessionImpl(ConferenceController controller, Integer id, IvrEndpointConversation conversation, 
                RealTimeConferenceMixer mixer, LoggerHelper logger, ConferenceSessionListener listener) 
                throws Exception 
        {
            this.id = id;
            this.controller = controller;
            this.conversation = conversation;
            this.sessionListener = listener;
            this.logger = new LoggerHelper(logger, "["+id+", "+conversation.getCallingNumber()+"]. ");
            this.mixerSession = mixer.addParticipant(+id+", "+conversation.getCallingNumber(), null);
        }
        
        public void start() {
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
            try {
                mixerSession.stopSession();
                controller.removeSession(this);
                if (logger.isDebugEnabled())
                    logger.debug("Disconnected");
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error("Error sopping mixer session", e);
            }
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
    
    private class Recorder {
        private final long startTime = System.currentTimeMillis();
        private final ConferenceRecordingNode recordingNode;
        private final AudioFileWriterDataSource fileWriter;
        private final LoggerHelper logger;
        
        public Recorder(PushBufferDataSource ds, LoggerHelper logger, CodecManager codecManager) throws Exception {
            recordingNode = createRecordingNode();
            File path = getManager().getRecordingPath(ConferenceNode.this);
            String filename = String.format("%s/%s_%s_%s.wav",
                    path.getAbsolutePath(),
                    ConferenceNode.this.getId(),
                    recordingNode.getName(),
                    new SimpleDateFormat("yyyyMMdd_HHmmss"));
            this.logger = new LoggerHelper(logger, "Recorder. ");
            if (this.logger.isDebugEnabled())
                this.logger.debug("Starting record the conference to file ({})", filename);
            fileWriter = new AudioFileWriterDataSource(
                    new File(filename), ds, codecManager, FileTypeDescriptor.WAVE, this.logger);
            fileWriter.start();
        }

        public void stop() {
            fileWriter.stop();
        }
    }
}
