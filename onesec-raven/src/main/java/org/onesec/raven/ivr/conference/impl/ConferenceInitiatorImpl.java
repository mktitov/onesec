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

import org.onesec.raven.ivr.conference.ConferenceInitiator;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceInitiatorImpl implements ConferenceInitiator {
    private final String initiatorId;
    private final String initiatorName;
    private final String initiatorPhone;
    private final String initiatorEmail;

    public ConferenceInitiatorImpl(String initiatorId, String initiatorName, String initiatorPhone, String initiatorEmail) {
        this.initiatorId = initiatorId;
        this.initiatorName = initiatorName;
        this.initiatorPhone = initiatorPhone;
        this.initiatorEmail = initiatorEmail;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public String getInitiatorPhone() {
        return initiatorPhone;
    }

    public String getInitiatorEmail() {
        return initiatorEmail;
    }
}
