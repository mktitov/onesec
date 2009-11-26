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

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.RtpReleaser;
import org.onesec.raven.ivr.RtpStat;
import org.onesec.raven.ivr.RtpStream;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractRtpStream implements RtpStream
{
    protected final InetAddress address;
    protected final int port;

    private AtomicLong handledPackets;
    private AtomicLong handledBytes;
    private RtpStreamManagerNode manager;
    private RtpStat globalStat;
    private Node owner;

    public AbstractRtpStream(InetAddress address, int port)
    {
        this.address = address;
        this.port = port;
    }

    Node getOwner()
    {
        return owner;
    }

    void setOwner(Node owner)
    {
        this.owner = owner;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    void setManager(RtpStreamManagerNode manager)
    {
        this.manager = manager;
    }

    void setGlobalRtpStat(RtpStat globalStat)
    {
        this.globalStat = globalStat;
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
        manager.releaseStream(this);
    }
}
