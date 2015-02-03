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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.RtpStream;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.rtp.RtpManagerConfigurator;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractRtpStream implements RtpStream
{
    protected final InetAddress address;
    protected final int port;
    private final String streamType;
    protected final long creationTime;
    protected final RtpManagerConfigurator rtpManagerConfigurator;

    protected String remoteHost;
    protected int remotePort;

    private final AtomicLong handledPackets;
    private final AtomicLong handledBytes;
    private RtpStreamManager manager;
    protected Node owner;
//    protected String logPrefix;
    private final AtomicBoolean released;
    protected volatile LoggerHelper logger;

    public AbstractRtpStream(InetAddress address, int port, String streamType, 
            RtpManagerConfigurator configurator)
    {
        this.address = address;
        this.port = port;
        this.streamType = streamType;
        this.rtpManagerConfigurator = configurator;
        this.creationTime = System.currentTimeMillis();

        handledBytes = new AtomicLong();
        handledPackets = new AtomicLong();
        released = new AtomicBoolean(false);
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    Node getOwner()
    {
        return owner;
    }

    void setOwner(Node owner)
    {
        this.owner = owner;
        if (logger==null)
            this.logger = new LoggerHelper(owner, streamType);
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    void setManager(RtpStreamManager manager)
    {
        this.manager = manager;
    }
    
    protected RtpStreamManager getManager() {
        return manager;
    }

//    public String getLogPrefix() {
//        return logPrefix;
//    }

//    public void setLogPrefix(String logPrefix) {
//        this.logPrefix = logPrefix;
//    }

    protected void incHandledPacketsBy(long packets)
    {
        handledPackets.addAndGet(packets);
        manager.incHandledPackets(this, packets);
    }

    protected void incHandledBytesBy(long bytes)
    {
        handledBytes.addAndGet(bytes);
        manager.incHandledBytes(this, bytes);
    }

    public void release() {
        if (!released.compareAndSet(false, true)) {
            if (logger.isDebugEnabled())
                logger.debug("Can't release stream because of already released");
            return;
        }
        try {
            if (logger.isDebugEnabled())
                logger.debug("Releasing stream...");
            doRelease();
            if (logger.isDebugEnabled())
                logger.debug("Stream realeased");
            manager.releaseStream(this);
        } catch(Exception e) {
            if (logger.isErrorEnabled())
                logger.error("Error releasing RTP stream", e);
        }
    }
    
//    protected String logMess(String mess, Object... args)
//    {
//        return (logPrefix==null? "" : logPrefix)+streamType+". "+String.format(mess, args);
//    }
    
//    private LoggerHelper createLogger(LoggerHelper) {
//        return new LoggerHelper()
//    }

    public void setLogger(LoggerHelper logger) {
        this.logger = new LoggerHelper(logger, streamType+". ");
    }
        

    public abstract void doRelease() throws Exception;
}
