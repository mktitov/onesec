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
    public final static int UNKNWON_CAUSE = 0;
    public final static int FROM_DATE_AFTER_TO_DATE = 1;
    public final static int DATE_AFTER_CURRENT_DATE = 2;
    public final static int CONFERENCE_TO_LONG = 3;
    public final static int CONFERENCE_TO_FAR_IN_FUTURE = 4;
    public final static int NOT_ENOUGH_CHANNELS = 5;
    public final static int NULL_CONFERENCE_NAME = 6;
    public final static int NULL_FROM_DATE = 7;
    public final static int NULL_TO_DATE = 8;
    public final static int INVALID_CHANNELS_COUNT = 9;
    public final static int CONFERENCE_MANAGER_BUSY = 10;
    public final static int CONFERENCE_MANAGER_STOPPED = 11;
    
    private final int causeCode;
            
    public ConferenceException(String msg) {
        super(msg);
        this.causeCode = 0;
    }

    public ConferenceException(int causeCode) {
        super();
        this.causeCode = causeCode;
    }

    public ConferenceException(String message, Throwable cause) {
        super(message, cause);
        this.causeCode = UNKNWON_CAUSE;
    }

    public ConferenceException(Throwable cause) {
        super(cause);
        this.causeCode = UNKNWON_CAUSE;
    }

    public int getCauseCode() {
        return causeCode;
    }
}
