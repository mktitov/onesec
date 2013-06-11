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
package org.onesec.raven.ivr.impl;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mikhail Titov
 */
public class TickHelper {
    private final long tickInterval;
    private long packetNumber = 1;
    private final long startTime = System.currentTimeMillis();
    
    private long timeDiff;
    private long expectedPacketNumber;
    private long correction;
    private long sleepTime;

    public TickHelper(long tickInterval) {
        this.tickInterval = tickInterval;
    }
   
    public void sleep() throws InterruptedException {
        timeDiff = System.currentTimeMillis() - startTime;
        expectedPacketNumber = timeDiff/tickInterval;
        correction = timeDiff % tickInterval;
        sleepTime = (packetNumber-expectedPacketNumber) * tickInterval - correction;
        if (sleepTime>0)
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        packetNumber++;
    }
}
