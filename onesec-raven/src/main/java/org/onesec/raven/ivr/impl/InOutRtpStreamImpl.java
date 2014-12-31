/*
 * Copyright 2014 Mikhail Titov.
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

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.rtp.RTPManager;
import org.onesec.raven.ivr.InOutRtpStream;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.RtpAddress;
import org.onesec.raven.ivr.RtpStream;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.rtp.RtpManagerConfigurator;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class InOutRtpStreamImpl extends AbstractRtpStream implements InOutRtpStream {
    private volatile RTPManager rtpManager;
    private final AtomicReference<InboundStream> inStream = new AtomicReference<InboundStream>();

    public InOutRtpStreamImpl(InetAddress address, int port, RtpManagerConfigurator configurator) {
        super(address, port, "IN/OUT", configurator);
    }

    public void open(String remoteHost, int remotePort) throws RtpStreamException {
        try {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Trying to open IN/OUT RTP channel with the remote host (%s:%s)"
                        , remoteHost, remotePort));
            rtpManager = rtpManagerConfigurator.configureInboundManager(
                    address, port, InetAddress.getByName(remoteHost), remotePort, logger);
        } catch(Exception e) {
            throw new RtpStreamException(logger.logMess(
                        "Error creating receiver for RTP stream from remote host (%s)"
                        , remoteHost)
                    , e);
        }
    }

    @Override
    public void doRelease() throws Exception {
        releaseStream(inStream.get());
    }

//    public long getHandledBytes() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    public long getHandledPackets() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    public IncomingRtpStream getIncomingRtpStream(Node owner) {
        InboundStream stream = inStream.get();
        releaseStream(stream);
//        if (stream!=null)
//            stream.release();
        stream = new InboundStream(address, port, getManager());
        inStream.set(stream);
        return stream;
    }
    
    private void releaseStream(RtpStream stream) {
        if (stream!=null)
            stream.release();
    }

    public OutgoingRtpStream getOutgoingRtpStream(Node owner) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RtpAddress reserveAddress(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unreserveAddress(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class InboundStream extends IncomingRtpStreamImpl {

        public InboundStream(InetAddress addr, int port, RtpStreamManagerNode manager) {
            super(addr, port, null);
            setManager(manager);
        }

        @Override
        public void open(String remoteHost) throws RtpStreamException {
            open();
        }

        @Override
        public void open(String remoteHost, int remotePort) throws RtpStreamException {
            open();
        }
        
        public void open() {
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                            "Trying to open incoming RTP stream, using existed RTPManager, "
                                    + "from the remote host (%s) and port (%s)"
                            , remoteHost, remotePort));
            rtpManager.addReceiveStreamListener(this);
        }

        @Override
        public void release() {
            if (inStream.compareAndSet(this, null)) {
                if (logger.isDebugEnabled())
                    logger.debug("Releasing stream...");
                rtpManager.removeReceiveStreamListener(this);            
                if (logger.isDebugEnabled())
                    logger.debug("Stream released");
            }
        }
    }
    
}
