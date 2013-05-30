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
package org.onesec.raven.ivr.conference;

import java.io.File;
import java.util.Date;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public interface ConferenceManager {
    Conference createConference(String name, Date fromDate, Date toDate, int channelCount, 
            ConferenceInitiator initiator) throws ConferenceException;
    void removeConference(int conferenceId) throws ConferenceException;
    void join(IvrEndpointConversation conversation, String conferenceId, String accessCode, 
            ConferenceSessionListener listener);
    void checkConferenceNode(final Conference conf) throws ConferenceException;
    public File getRecordingPath(Conference conference) throws Exception;
    public AudioFile getOneMinuteLeftAudio();
}
