/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.conference;

import java.util.List;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrConference
{
    /**
     * Adds recorded to the conference
     */
    public void addRecorder(AudioStream stream);
    /**
     * Adds conversation to the conference
     */
    public void addConversation(IvrEndpointConversation conversation);
    /**
     * Returns all records or empty list
     */
    public List<AudioStream> getRecorders();
    /**
     * Returns all conversations
     */
    public List<IvrEndpointConversation> getConversations();
    /**
     * Adds listener to the conference
     */
    public void addConferenceListener(IvrConferenceListener listener);
    /**
     * Removes listener from the conference
     */
    public void removeListener(IvrConferenceListener listener);
    /**
     * Starts the conference
     */
    public void startConference();
    /**
     * Stops the conference
     */
    public void stopConference();
}
