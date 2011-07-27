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

package org.onesec.raven.ivr.conference.impl;

import java.util.List;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.conference.IvrConference;
import org.onesec.raven.ivr.conference.IvrConferenceListener;

/**
 *
 * @author Mikhail Titov
 */
public class IvrConferenceImpl implements IvrConference
{

    public void addRecorder(AudioStream stream) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addConversation(IvrEndpointConversation conversation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<AudioStream> getRecorders() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<IvrEndpointConversation> getConversations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addConferenceListener(IvrConferenceListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeListener(IvrConferenceListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void startConference() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stopConference() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
