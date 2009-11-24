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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStream;
import org.onesec.raven.ivr.RtpStreamManager;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class RtpStreamManagerNode extends BaseNode implements RtpStreamManager
{
    private Map<String, RtpStream> incomingStreams;
    private Map<String, RtpStream> outgoingStreams;

    private ReentrantReadWriteLock isLock;
    private ReentrantReadWriteLock osLock;

    @Override
    protected void initFields()
    {
        super.initFields();
        incomingStreams = new HashMap<String, RtpStream>();
        outgoingStreams = new HashMap<String, RtpStream>();

        isLock = new ReentrantReadWriteLock();
        osLock = new ReentrantReadWriteLock();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();

        releaseStreams(isLock, incomingStreams);
        releaseStreams(osLock, outgoingStreams);
    }

    public IncomingRtpStream getIncomingRtpStream()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OutgoingRtpStream getOutgoingRtpStream(String remoteHost, int remotePort)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void releaseStreams(ReadWriteLock lock, Map<String, RtpStream> streams)
    {
        lock.writeLock().lock();
        try
        {
            if (streams.size()>0)
            {
                Collection<RtpStream> list = new ArrayList<RtpStream>(streams.values());
                for (RtpStream stream: list)
                    stream.release();
            }
            streams.clear();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}
