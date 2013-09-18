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

import org.onesec.raven.ivr.conference.Participant;

/**
 *
 * @author Mikhail Titov
 */
public class ParticipantImpl implements Participant {
    private final String phoneNumber;
    private final String joinTime;
    private final String disconnectTime;
    private final boolean active;

    public ParticipantImpl(String phoneNumber, String joinTime, String disconnectTime, boolean active) {
        this.phoneNumber = phoneNumber;
        this.joinTime = joinTime;
        this.active = active;
        this.disconnectTime = active? null : disconnectTime;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getJoinTime() {
        return joinTime;
    }

    public String getDisconnectTime() {
        return disconnectTime;
    }

    public boolean isActive() {
        return active;
    }
}
