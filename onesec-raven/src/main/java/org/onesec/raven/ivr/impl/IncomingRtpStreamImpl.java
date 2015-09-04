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
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.rtp.RtpManagerConfigurator;

/**
 *
 * @author Mikhail Titov
 */
public class IncomingRtpStreamImpl extends AbstractRtpStream 
        implements IncomingRtpStream, ReceiveStreamListener
{

    public enum Status {INITIALIZING, OPENED, CLOSED}
    
    public final static AudioFormat FORMAT = new AudioFormat(
            AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED) ;


    private RTPManager rtpManager;
    private ReceiveStream stream;
    private DataSourceCloneBuilder sourceCloneBuilder; //SourceClonable
    private final List<Consumer> consumers;
    private final Lock lock;
//    private volatile GlobalReceptionStats rtpStat;
    private Status status;

    public IncomingRtpStreamImpl(InetAddress address, int port, RtpManagerConfigurator configurator)
    {
        super(address, port, "Inbound RTP", configurator);
        
        status = Status.INITIALIZING;
        consumers = new LinkedList<>();
        lock = new ReentrantLock();
    }

    @Override
    public void doRelease() throws Exception
    {
        try {
            try {
                try {
                    if (sourceCloneBuilder!=null)
                        sourceCloneBuilder.close();
                } finally {
                    if (!consumers.isEmpty())
                        for (Consumer consumer: consumers)
                            consumer.fireStreamClosingEvent();
                }
            } finally {
                if (rtpManager!=null) {
                    GlobalReceptionStats stats = rtpManager.getGlobalReceptionStats();
                    incHandledBytesBy(stats.getBytesRecd());
                    incHandledPacketsBy(stats.getPacketsRecd());
                    rtpManager.removeTargets("Disconnected");
                }
            }
        } finally {
            releaseRtpManager();
        }
    }
    
    protected Map<String, Object> getStatFor(GlobalReceptionStats gStat) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put(CallCdrRecordSchemaNode.IN_RTP_LOCAL_ADDR, getAddress().getHostAddress());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_LOCAL_PORT, getPort());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_REMOTE_ADDR, getRemoteHost());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_REMOTE_PORT, getRemotePort());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_BAD_RTCP_PACKETS, gStat.getBadRTCPPkts());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_BAD_RTCP_PACKETS, gStat.getBadRTPkts());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_BYTES_RECEIVED, gStat.getBytesRecd());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_LOCAL_COLLISIONS, gStat.getLocalColls());
        stat.put("malformedBye", gStat.getMalformedBye());
        stat.put("malformedRR", gStat.getMalformedRR());
        stat.put("malformedSDES", gStat.getMalformedSDES());
        stat.put("malformedSR", gStat.getMalformedSR());
        stat.put("packetsLooped", gStat.getPacketsLooped());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_PACKETS_RECEIVED, gStat.getPacketsRecd());
        stat.put("RTCPRecd", gStat.getRTCPRecd());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_REMOTE_COLLISIONS, gStat.getRemoteColls());
        stat.put("SRRecd", gStat.getSRRecd());
        stat.put(CallCdrRecordSchemaNode.IN_RTP_TRANSMIT_FAILED, gStat.getTransmitFailed());
        stat.put("unknownTypes", gStat.getUnknownTypes());
        return stat;
        
    }

    @Override
    public Map<String, Object> getStat() {
        final RTPManager _rtpManager = rtpManager;
        if (_rtpManager==null)
            return Collections.EMPTY_MAP;
        else 
            return getStatFor(_rtpManager.getGlobalReceptionStats());
    }
    
    protected void releaseRtpManager() {
        if (rtpManager!=null)
            rtpManager.dispose();        
    }

    public void open(String remoteHost) throws RtpStreamException {
        open(remoteHost, SessionAddress.ANY_PORT);
    }
    
    public void open(String remoteHost, int remotePort) throws RtpStreamException {
        try {
            this.remoteHost = remoteHost;
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Trying to open incoming RTP stream from the remote host (%s)"
                        , remoteHost));
            rtpManager = rtpManagerConfigurator.configureInboundManager(
                    address, port, InetAddress.getByName(remoteHost), remotePort, logger);
            rtpManager.addReceiveStreamListener(this);
        } catch(Exception e) {
            throw new RtpStreamException(logger.logMess(
                        "Error creating receiver for RTP stream from remote host (%s)"
                        , remoteHost)
                    , e);
        }
    }
    
    public boolean addDataSourceListener(IncomingRtpStreamDataSourceListener listener, AudioFormat format)
        throws RtpStreamException
    {
        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)){
                try{
                    if (status==Status.CLOSED)
                        return false;
                    else if (status==Status.OPENED || status==Status.INITIALIZING){
                        Consumer consumer = new Consumer(listener, format);
                        consumers.add(consumer);
                        if (status==Status.OPENED)
                            consumer.fireDataSourceCreatedEvent();
                    } 
                }finally{
                    lock.unlock();
                }
                return true;
            } else {
                if (logger.isErrorEnabled())
                    logger.error("Error adding listener. Lock wait timeout");
                throw new RtpStreamException("Error adding listener. Lock wait timeout");
            }
        } catch(InterruptedException e) {
            if (logger.isErrorEnabled())
                logger.error("Error adding listener", e);
            throw new RtpStreamException("Error adding listener to the IncomingRtpStream", e);
        }
    }

    public void update(final ReceiveStreamEvent event)
    {
        try {
            if (logger.isDebugEnabled())
                logger.debug(String.format("Received stream event (%s)", event.getClass().getName()));

            if (event instanceof NewReceiveStreamEvent) {
                initStream(event);
            } else if (event instanceof ByeEvent) {
                lock.lock();
                try {
                    status = Status.CLOSED;
                } finally {
                    lock.unlock();
                }
            } else if (event instanceof RemotePayloadChangeEvent) {
                RemotePayloadChangeEvent payloadEvent = (RemotePayloadChangeEvent) event;
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Payload changed to %d", payloadEvent.getNewPayload()));
                if (payloadEvent.getNewPayload()<19) {
                    if (logger.isDebugEnabled())
                        logger.debug("Trying to handle received stream");
                    initStream(event);
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled())
                logger.error("Error initializing rtp data source", e);
            status = Status.CLOSED;
        }
    }
    
    private void initStream(final ReceiveStreamEvent event) throws IOException {
        stream = event.getReceiveStream();
        if (logger.isDebugEnabled())
            logger.debug("Received new stream");

        sourceCloneBuilder = new DataSourceCloneBuilder((PushBufferDataSource)stream.getDataSource(), logger);
        sourceCloneBuilder.open();
        lock.lock();
        try{
            if (logger.isDebugEnabled())
                logger.debug("Sending dataSourceCreatedEvent to consumers");
            status = Status.OPENED;
            if (!consumers.isEmpty())
                for (Consumer consumer: consumers)
                    consumer.fireDataSourceCreatedEvent();

        }finally{
            lock.unlock();
        }
    }

    private class Consumer {
        private final IncomingRtpStreamDataSourceListener listener;
        private final AudioFormat format;

        private Processor processor;
        private DataSource inDataSource;
        private DataSource outDataSource;
        private boolean closed = false;

        public Consumer(IncomingRtpStreamDataSourceListener listener, AudioFormat format) {
            this.listener = listener;
            this.format = format==null? FORMAT : format; 
        }

        private void fireDataSourceCreatedEvent() {
            try {
                synchronized(this) {
                    if (closed) {
                        if (logger.isDebugEnabled())
                            logger.debug("Can't create data source for consumer because "
                                    + "of consumer already closed");
                        return;
                    }
                    inDataSource = sourceCloneBuilder.createClone();
                    inDataSource = new RealTimeDataSource((PushBufferDataSource)inDataSource, logger);
                }
                listener.dataSourceCreated(IncomingRtpStreamImpl.this, inDataSource);
            } catch(Exception e) {
                if (logger.isErrorEnabled())
                    logger.error("Error creating data source for consumer", e);
                listener.dataSourceCreated(IncomingRtpStreamImpl.this, null);
            }
        }

        private void fireStreamClosingEvent() {
            try{
                try {
                    listener.streamClosing(IncomingRtpStreamImpl.this);
                } finally{
                    closeResources();
                }
            }catch(Exception e){
                if (logger.isErrorEnabled())
                    logger.error("Error closing data source consumer resources", e);
            }
        }
        
        private synchronized void closeResources() throws Exception {
            closed = true;
            try {
                try {
                    try {
                        if (inDataSource!=null)
                            inDataSource.disconnect();
                    } finally {
                        if (processor!=null)
                            processor.stop();
                    }
                } finally {
                    if (processor!=null)
                        processor.close();
                }
            } finally {
                if (outDataSource!=null)
                    outDataSource.stop();
            }
        }
    }
}