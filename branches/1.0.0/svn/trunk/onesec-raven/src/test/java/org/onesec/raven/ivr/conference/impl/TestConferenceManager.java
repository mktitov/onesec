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
import java.util.Date;
import java.util.List;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.conference.ChannelUsage;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceException;
import org.onesec.raven.ivr.conference.ConferenceInitiator;
import org.onesec.raven.ivr.conference.ConferenceManager;
import org.onesec.raven.ivr.conference.ConferenceSessionListener;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail TItov
 */
public class TestConferenceManager extends BaseNode implements ConferenceManager {
    
    private Node plannedConferences;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        plannedConferences = new ContainerNode("planned conferences");
        addAndSaveChildren(plannedConferences);
        plannedConferences.start();
    }
    
    public Node getPlannedConferencesNode() {
        return plannedConferences;
    }

    public void removeConference(int conferenceId) throws ConferenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void join(IvrEndpointConversation conversation, String conferenceId, String accessCode, ConferenceSessionListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void checkConferenceNode(Conference conf) throws ConferenceException {
    }

    public File getRecordingPath(Conference conference) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioFile getOneMinuteLeftAudio() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioFile getConferenceStoppedAudio() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public List<ChannelUsage> getChannelUsageSchedule(Date fromDate, Date toDate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<Conference> getConferencesByInitiatorId(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<Conference> getConferences() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Conference createConference(String name, Date fromDate, Date toDate, int channelCount, ConferenceInitiator initiator, boolean enableRecording) throws ConferenceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Conference getConferenceById(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
