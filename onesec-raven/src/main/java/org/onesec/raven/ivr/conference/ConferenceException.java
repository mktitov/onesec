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

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceException extends Exception {
    public enum CauseCode {UNKNWON_CAUSE, FROM_DATE_AFTER_TO_DATE, CURRENT_DATE_AFTER_TO_DATE, 
        CONFERENCE_TO_LONG, CONFERENCE_TO_FAR_IN_FUTURE, NOT_ENOUGH_CHANNELS, NULL_CONFERENCE_NAME, 
        NULL_FROM_DATE, NULL_TO_DATE, INVALID_CHANNELS_COUNT, CONFERENCE_MANAGER_BUSY, 
        CONFERENCE_MANAGER_STOPPED}
    
    private final CauseCode causeCode;
            
    public ConferenceException(String msg) {
        super(msg);
        this.causeCode = CauseCode.UNKNWON_CAUSE;
    }

    public ConferenceException(CauseCode causeCode) {
        super(causeCode.name());
        this.causeCode = causeCode;
    }

    public ConferenceException(String message, Throwable cause) {
        super(message, cause);
        this.causeCode = CauseCode.UNKNWON_CAUSE;
    }

    public ConferenceException(Throwable cause) {
        super(cause);
        this.causeCode = CauseCode.UNKNWON_CAUSE;
    }

    public CauseCode getCauseCode() {
        return causeCode;
    }
}
