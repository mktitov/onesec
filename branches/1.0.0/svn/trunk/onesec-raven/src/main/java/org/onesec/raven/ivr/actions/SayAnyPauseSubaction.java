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
package org.onesec.raven.ivr.actions;

import org.onesec.raven.ivr.SayAnyActionException;
import org.onesec.raven.ivr.SayAnySubaction;
import org.onesec.raven.ivr.SubactionPauseResult;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyPauseSubaction implements SayAnySubaction<SubactionPauseResult> {
    private final SubactionPauseResult pause;

    public SayAnyPauseSubaction(String strPause) throws SayAnyActionException {
        long seconds = 1;
        if (strPause.endsWith("s")) {
            seconds = 1000;
            strPause = strPause.substring(0, strPause.length()-1);
        }
        if (strPause.isEmpty())
            throw new SayAnyActionException(String.format("Invalid pause (%s)", strPause));
        try {
            pause = new SubactionPauseResultImpl(Long.parseLong(strPause) * seconds);
        } catch (NumberFormatException e) {
            throw new SayAnyActionException(String.format("Invalid pause (%s)", strPause));
        }
    }

    public SubactionPauseResult getResult() {
        return pause;
    }
}
