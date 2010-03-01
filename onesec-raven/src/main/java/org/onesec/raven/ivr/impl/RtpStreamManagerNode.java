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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpStream;
import org.onesec.raven.ivr.RtpStreamManager;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RtpStreamManagerNode extends BaseNode implements RtpStreamManager
{
    @NotNull @Parameter(defaultValue="20")
    private Integer maxStreamCount;

    private Map<InetAddress, NavigableMap<Integer, RtpStream>> streams;

    private ReentrantReadWriteLock streamsLock;

    private AtomicLong sendedBytes;
    private AtomicLong recievedBytes;
    private AtomicLong sendedPackets;
    private AtomicLong recievedPackets;

    @Override
    protected void initFields()
    {
        super.initFields();
        streams = new HashMap<InetAddress, NavigableMap<Integer, RtpStream>>();
        streamsLock = new ReentrantReadWriteLock();
        sendedBytes = new AtomicLong();
        recievedBytes = new AtomicLong();
        recievedPackets = new AtomicLong();
        sendedPackets = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        sendedBytes.set(0);
        recievedBytes.set(0);
        sendedPackets.set(0);
        recievedPackets.set(0);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();

        releaseStreams(streams);
    }

    public Integer getMaxStreamCount()
    {
        return maxStreamCount;
    }

    public void setMaxStreamCount(Integer maxStreamCount)
    {
        this.maxStreamCount = maxStreamCount;
    }

    public IncomingRtpStream getIncomingRtpStream(Node owner)
    {
        return (IncomingRtpStream)createStream(true, owner);
    }

    public OutgoingRtpStream getOutgoingRtpStream(Node owner)
    {
        return (OutgoingRtpStream)createStream(false, owner);
    }

    void incHandledBytes(RtpStream stream, long bytes)
    {
        if (stream instanceof OutgoingRtpStream)
            sendedBytes.addAndGet(bytes);
        else
            recievedBytes.addAndGet(bytes);
    }

    void incHandledPackets(RtpStream stream, long packets)
    {
        if (stream instanceof OutgoingRtpStream)
            sendedPackets.addAndGet(packets);
        else
            recievedPackets.addAndGet(packets);
    }

    void releaseStream(RtpStream stream)
    {
        Map portStreams = streams.get(stream.getAddress());
        if (portStreams!=null)
            portStreams.remove(stream.getPort());
    }

    Map<InetAddress, NavigableMap<Integer, RtpStream>> getStreams()
    {
        return streams;
    }

    private void releaseStreams(Map<InetAddress, NavigableMap<Integer, RtpStream>> streams)
    {
        streamsLock.writeLock().lock();
        try
        {
            for (Map<Integer, RtpStream> portStreams: streams.values())
            {
                if (portStreams.size()>0)
                {
                    Collection<RtpStream> list = new ArrayList<RtpStream>(portStreams.values());
                    for (RtpStream stream: list)
                        stream.release();
                }
                portStreams.clear();
            }
        }
        finally
        {
            streamsLock.writeLock().unlock();
        }
    }

    private RtpStream createStream(boolean incomingStream, Node owner)
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        try
        {
            streamsLock.writeLock().lock();
            try
            {
                if (getStreamsCount() >= maxStreamCount)
                    throw new Exception("Max streams count exceded");

                RtpStream stream = null;
                NavigableMap<Integer, RtpStream> portStreams = null;
                InetAddress address = null;
                int portNumber = 0;

                Map<InetAddress, RtpAddressNode> avalAddresses = getAvailableAddresses();
                for (Map.Entry<InetAddress, RtpAddressNode> addr: avalAddresses.entrySet())
                {
                    if (!streams.containsKey(addr.getKey()))
                    {
                        portNumber = addr.getValue().getStartingPort();
                        portStreams = new TreeMap<Integer, RtpStream>();
                        address = addr.getKey();
                        streams.put(addr.getKey(), portStreams);
                        break;
                    }
                }

                if (portStreams==null)
                    for (Map.Entry<InetAddress, NavigableMap<Integer, RtpStream>> streamEntry: streams.entrySet())
                        if (portStreams==null || streamEntry.getValue().size()<portStreams.size())
                        {
                            portStreams = streamEntry.getValue();
                            address = streamEntry.getKey();
                            int startingPort = avalAddresses.get(streamEntry.getKey()).getStartingPort();
                            if (portStreams.size()==0)
                                portNumber = startingPort;
                            else if (portStreams.firstKey()>startingPort)
                                portNumber = portStreams.firstKey()-2;
                            else
                                portNumber = portStreams.lastKey()+2;
                        }

                if (incomingStream)
                    stream = new IncomingRtpStreamImpl(address, portNumber);
                else
                    stream = new OutgoingRtpStreamImpl(address, portNumber);

                ((AbstractRtpStream)stream).setManager(this);
                ((AbstractRtpStream)stream).setOwner(owner);
                portStreams.put(portNumber, stream);

                return stream;
            }
            finally
            {
                streamsLock.writeLock().unlock();
            }
        }
        catch (Exception e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "Error creating %s RTP stream. %s"
                        , incomingStream? "incoming" : "outgoing", e.getMessage())
                    , e);
            return null;
        }
    }

    @Parameter(readOnly=true)
    public Integer getStreamsCount()
    {
        if (!Status.STARTED.equals(getStatus()))
            return 0;
        streamsLock.readLock().lock();
        try
        {
            int count = 0;
            for (Map portStreams: streams.values())
                count += portStreams==null? 0 : portStreams.size();

            return count;
        }
        finally
        {
            streamsLock.readLock().unlock();
        }
    }

    private Map<InetAddress, RtpAddressNode> getAvailableAddresses() throws UnknownHostException
    {
        Collection<Node> childs = getChildrens();
        Map<InetAddress, RtpAddressNode> res = null;
        if (childs!=null && !childs.isEmpty())
        {
            for (Node child: childs)
                if (child instanceof RtpAddressNode && Status.STARTED.equals(child.getStatus()))
                {
                    if (res==null)
                        res = new HashMap<InetAddress, RtpAddressNode>();
                    res.put(InetAddress.getByName(child.getName()), (RtpAddressNode)child);
                }
        }

        return res==null? Collections.EMPTY_MAP : res;
    }
}
