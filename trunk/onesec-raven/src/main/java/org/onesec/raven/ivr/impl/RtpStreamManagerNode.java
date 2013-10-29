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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.ivr.RtpAddress;
import org.onesec.raven.ivr.RtpStream;
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.rtp.RtpManagerConfigurator;
import org.onesec.raven.rtp.StandartRtpManagerConfigurator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RtpStreamManagerNode extends BaseNode implements RtpStreamManager, Viewable
{
    @Service
    private static RTPManagerService rtpManagerService;
    
    @NotNull @Parameter(defaultValue="20")
    private Integer maxStreamCount;
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private RtpManagerConfigurator rtpManagerConfigurator;

    private Map<InetAddress, NavigableMap<Integer, RtpStream>> streams;
    private Map<Node, RtpAddress> reservedAddresses;
    private Map<InetAddress, Set<Integer>> busyPorts;
    private ReentrantReadWriteLock streamsLock;

    private AtomicLong sendedBytes;
    private AtomicLong recievedBytes;
    private AtomicLong sendedPackets;
    private AtomicLong recievedPackets;
    private AtomicLong rejectedStreamCreations;
    private AtomicLong streamCreations;
    
    private final static RtpManagerConfigurator standartRtpManagerConfigurator = 
            new StandartRtpManagerConfigurator(rtpManagerService);

    @Message private static String busyPortsMessage;
    @Message private static String statMessage;
    @Message private static String sendedBytesMessage;
    @Message private static String sendedPacketsMessage;
    @Message private static String recievedBytesMessage;
    @Message private static String recievedPacketsMessage;
    @Message private static String createdStreamsMessage;
    @Message private static String rejectedStreamsMessage;
    @Message private static String incomingStreamMessage ;
    @Message private static String outgoingStreamMessage ;
    @Message private static String localAddressMessage;
    @Message private static String localPortMessage;
    @Message private static String remoteAddressMessage;
    @Message private static String remotePortMessage;
    @Message private static String creationTimeMessage;
    @Message private static String durationMessage;
    @Message private static String managerBusyMessage;
    @Message private static String streamOwnerMessage;

    @Override
    protected void initFields()
    {
        super.initFields();
        streams = new HashMap<InetAddress, NavigableMap<Integer, RtpStream>>();
        busyPorts =  new HashMap<InetAddress, Set<Integer>>();
        reservedAddresses = new HashMap<Node, RtpAddress>();
        streamsLock = new ReentrantReadWriteLock();
        sendedBytes = new AtomicLong();
        recievedBytes = new AtomicLong();
        recievedPackets = new AtomicLong();
        sendedPackets = new AtomicLong();
        rejectedStreamCreations = new AtomicLong();
        streamCreations = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        sendedBytes.set(0);
        recievedBytes.set(0);
        sendedPackets.set(0);
        recievedPackets.set(0);
        rejectedStreamCreations.set(0);
        streamCreations.set(0);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        releaseStreams(streams);
        streams.clear();
        busyPorts.clear();
    }
    
    private RtpManagerConfigurator getRtpConfigurator() {
        RtpManagerConfigurator configurator = rtpManagerConfigurator;
        return configurator==null? standartRtpManagerConfigurator : configurator;
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        
        List<ViewableObject> vos = new ArrayList<ViewableObject>();
        if (streamsLock.readLock().tryLock(500, TimeUnit.MILLISECONDS)){
            try {
                TableImpl statTable = new TableImpl(new String[]{
                    createdStreamsMessage, rejectedStreamsMessage, sendedBytesMessage, sendedPacketsMessage,
                    recievedBytesMessage, recievedPacketsMessage});
                statTable.addRow(new Object[]{streamCreations, rejectedStreamCreations,
                    sendedBytes, sendedPackets, recievedBytes, recievedPackets});
                
                TableImpl busyPortsTable = new TableImpl(new String[]{localAddressMessage, localPortMessage});
                for (Entry<InetAddress, Set<Integer>> addr: busyPorts.entrySet())
                    for (Integer port: addr.getValue())
                        busyPortsTable.addRow(new Object[]{addr.getKey().getHostAddress(), port});

                String[] colnames = {
                    localAddressMessage, localPortMessage, remoteAddressMessage, remotePortMessage,
                    creationTimeMessage, durationMessage, streamOwnerMessage};
                TableImpl inStreams = new TableImpl(colnames);
                TableImpl outStreams = new TableImpl(colnames);
                SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                for (Map.Entry<InetAddress, NavigableMap<Integer, RtpStream>> addr: streams.entrySet()){
                    NavigableMap<Integer, RtpStream> ports = addr.getValue();
                    if (ports!=null)
                        for (RtpStream stream: ports.values())
                            if (stream instanceof OutgoingRtpStream)
                                outStreams.addRow(createRowFromStream(stream, fmt));
                            else
                                inStreams.addRow(createRowFromStream(stream, fmt));
                }
                vos = new ArrayList<ViewableObject>();
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, statMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, statTable));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, busyPortsMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, busyPortsTable));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, outgoingStreamMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, outStreams));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, incomingStreamMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, inStreams));
            } finally {
                streamsLock.readLock().unlock();
            }
        } else
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, managerBusyMessage));
        
        return vos;
    }

    private Object[] createRowFromStream(RtpStream stream, SimpleDateFormat fmt)
    {
        return new Object[]{
            stream.getAddress().toString(), stream.getPort(),
            stream.getRemoteHost(), stream.getRemotePort(),
            fmt.format(new Date(stream.getCreationTime())),
            (System.currentTimeMillis()-stream.getCreationTime())/1000,
            new ViewableObjectImpl(Viewable.RAVEN_NODE_MIMETYPE, ((AbstractRtpStream)stream).getOwner().getPath())};
    }

    public Integer getMaxStreamCount() {
        return maxStreamCount;
    }

    public void setMaxStreamCount(Integer maxStreamCount) {
        this.maxStreamCount = maxStreamCount;
    }

    public RtpManagerConfigurator getRtpManagerConfigurator() {
        return rtpManagerConfigurator;
    }

    public void setRtpManagerConfigurator(RtpManagerConfigurator rtpManagerConfigurator) {
        this.rtpManagerConfigurator = rtpManagerConfigurator;
    }

    public IncomingRtpStream getIncomingRtpStream(Node owner)
    {
        return (IncomingRtpStream)createStreamOrReserveAddress(true, owner, false);
    }

    public OutgoingRtpStream getOutgoingRtpStream(Node owner)
    {
        return (OutgoingRtpStream)createStreamOrReserveAddress(false, owner, false);
    }

    public RtpAddress reserveAddress(Node node)
    {
        return createStreamOrReserveAddress(false, node, true);
    }

    public void unreserveAddress(Node node)
    {
        streamsLock.writeLock().lock();
        try{
            RtpAddress rtpAddress = reservedAddresses.remove(node);
            if (rtpAddress!=null) {
                if (getLogger().isDebugEnabled())
                    getLogger().debug("Unreserving rtp address for node ({})", node.getPath());
                releaseStream(rtpAddress);
            }
        } finally {
            streamsLock.writeLock().unlock();
        }
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

    void releaseStream(RtpAddress stream) {
        streamsLock.writeLock().lock();
        try{
            Map portStreams = streams.get(stream.getAddress());
            if (portStreams!=null)
                portStreams.remove(stream.getPort());
        }finally{
            streamsLock.writeLock().unlock();
        }
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

    private RtpAddress createStreamOrReserveAddress(
            boolean incomingStream, Node owner, boolean reserve)
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        try
        {
            streamsLock.writeLock().lock();
            try
            {
                if (reserve && reservedAddresses.containsKey(owner)) {
                    if (getLogger().isWarnEnabled())
                        getLogger().warn("The node ({}) is already reserved the address and port ");
                    return reservedAddresses.get(owner);
                }
                if (getStreamsCount() >= maxStreamCount)
                    throw new Exception("Max streams count exceded");

                RtpStream stream = null;
                NavigableMap<Integer, RtpStream> portStreams = null;
                InetAddress address = null;
                int portNumber = 0;

                Map<InetAddress, RtpAddressNode> avalAddresses = getAvailableAddresses();
                for (Map.Entry<InetAddress, RtpAddressNode> addr: avalAddresses.entrySet()) {
                    if (!streams.containsKey(addr.getKey())) {
                        address = addr.getKey();
                        portStreams = new TreeMap<Integer, RtpStream>();
//                        portNumber = addr.getValue().getStartingPort();
                        portNumber = getPortNumber(address, addr.getValue().getStartingPort()
                                , addr.getValue().getMaxPortNumber(), portStreams);
                        streams.put(addr.getKey(), portStreams);
                        break;
                    }
                }

                if (portStreams==null)
                    for (Map.Entry<InetAddress, NavigableMap<Integer, RtpStream>> streamEntry: streams.entrySet())
                        if (portStreams==null || streamEntry.getValue().size()<portStreams.size()) {
                            portStreams = streamEntry.getValue();
                            address = streamEntry.getKey();
                            int startingPort = avalAddresses.get(streamEntry.getKey()).getStartingPort();
                            int maxPortNumber = avalAddresses.get(streamEntry.getKey()).getMaxPortNumber();
                            portNumber = getPortNumber(address, startingPort, maxPortNumber, portStreams);
                        }

                if (!reserve) {
                    stream = incomingStream?
                            new IncomingRtpStreamImpl(address, portNumber, getRtpConfigurator()) :
                            new OutgoingRtpStreamImpl(address, portNumber, getRtpConfigurator());
//                    if (incomingStream)
//                        stream = new IncomingRtpStreamImpl(address, portNumber, getRtpConfigurator());
//                    else
//                        stream = new OutgoingRtpStreamImpl(address, portNumber, getRtpConfigurator());
                    ((AbstractRtpStream)stream).setManager(this);
                    ((AbstractRtpStream)stream).setOwner(owner);
                    portStreams.put(portNumber, stream);

                    streamCreations.incrementAndGet();

                    return stream;
                } else {
                    portStreams.put(portNumber, null);
                    RtpAddress rtpAddress = new RtpAddressImpl(address, portNumber);
                    reservedAddresses.put(owner, rtpAddress);

                    return rtpAddress;
                }
            } finally {
                streamsLock.writeLock().unlock();
            }
        } catch (Exception e) {
            rejectedStreamCreations.incrementAndGet();
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "Error creating %s RTP stream. %s"
                        , incomingStream? "incoming" : "outgoing", e.getMessage())
                    , e);
            return null;
        }
    }
    
    private int getPortNumber(InetAddress addr, int startingPort, int maxPortNumber
            , NavigableMap<Integer, RtpStream> portStreams) throws Exception 
    {
        int port = portStreams.isEmpty()? startingPort : portStreams.firstKey()-2;
        while (startingPort<=port)
            if (checkPort(addr, port)) 
                return port;
            else
                port-=2;
        port = portStreams.isEmpty()? startingPort+2 : portStreams.lastKey()+2;
        while (maxPortNumber>=port)
            if (checkPort(addr, port))
                return port;
            else
                port+=2;
        //fullscan
        int fromPort = portStreams.isEmpty()? startingPort : portStreams.firstKey()+2;
        int toPort = portStreams.isEmpty()? maxPortNumber : portStreams.lastKey()-2;
        for (port = fromPort; port<=toPort; port+=2) 
            if (!portStreams.containsKey(port) && checkPort(addr, port))
                return port;
        throw new Exception("No free port");
    }
    
    private boolean checkPort(InetAddress addr, int port) {
        Set<Integer> ports = busyPorts.get(addr);
        if (ports!=null && (ports.contains(port) || ports.contains(port+1)))
                return false;
        for (int p=port; p<=port+1; ++p) {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(p);
                socket.setReuseAddress(true);
            } catch (IOException e) {
                if (ports==null) {
                    ports = new HashSet<Integer>();
                    busyPorts.put(addr, ports);
                }
                ports.add(p);
                return false;
            } finally { 
                if (socket != null) 
                    socket.close(); 
            }            
        }
        return true;
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
