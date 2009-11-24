/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.RtpReleaser;
import org.onesec.raven.ivr.RtpStat;
import org.onesec.raven.ivr.RtpStream;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractRtpStream implements RtpStream
{
    private AtomicLong handledPackets;
    private AtomicLong handledBytes;
    private RtpStreamManagerNode manager;
    private RtpStat globalStat;
    private RtpReleaser releaser;

    void setManager(RtpStreamManagerNode manager)
    {
        this.manager = manager;
    }

    void setGlobalRtpStat(RtpStat globalStat)
    {
        this.globalStat = globalStat;
    }

    public void setReleaser(RtpReleaser releaser)
    {
        this.releaser = releaser;
    }

    protected void incHandledPacketsBy(long packets)
    {
        handledPackets.addAndGet(packets);
        globalStat.incHandledPacketsBy(packets);
    }

    protected void incHandledBytesBy(long bytes)
    {
        handledBytes.addAndGet(bytes);
        globalStat.incHandledBytesBy(bytes);
    }

    public void release()
    {
        releaser.release(this);
    }
}
