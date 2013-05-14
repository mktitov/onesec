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
    
    private final int causeCode;
            
    public ConferenceException(String msg) {
        super(msg);
        this.causeCode = 0;
    }

    public ConferenceException(String msg, int causeCode) {
        super(msg);
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
