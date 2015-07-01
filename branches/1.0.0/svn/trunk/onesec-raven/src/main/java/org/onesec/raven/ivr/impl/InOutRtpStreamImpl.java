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
import org.onesec.raven.ivr.RtpStreamManager;
import org.onesec.raven.rtp.RtpManagerConfigurator;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class InOutRtpStreamImpl extends AbstractRtpStream implements InOutRtpStream {
    private volatile RTPManager rtpManager;
    private final AtomicReference<InboundStream> inStream = new AtomicReference<InboundStream>();
    private final AtomicReference<OutboundStream> outStream = new AtomicReference<OutboundStream>();

    public InOutRtpStreamImpl(InetAddress address, int port, RtpManagerConfigurator configurator) {
        super(address, port, "(In/Out)bound RTP", configurator);
    }

    public void open(String remoteHost, int remotePort) throws RtpStreamException {
        synchronized(this) {
            try {
                if (rtpManager!=null)
                    return;
                this.remoteHost = remoteHost;
                this.remotePort = remotePort;
                if (logger.isDebugEnabled())
                    logger.debug(String.format(
                            "Trying to open IN/OUT RTP channel with the remote host (%s:%s)"
                            , remoteHost, remotePort));
                rtpManager = rtpManagerConfigurator.configureInboundManager(
                        address, port, InetAddress.getByName(remoteHost), remotePort, logger);
                InboundStream _inStream = inStream.get();
                if (_inStream.waitingForOpen)
                    _inStream.open();
            } catch(Exception e) {
                throw new RtpStreamException(logger.logMess(
                            "Error creating receiver for RTP stream from remote host (%s)"
                            , remoteHost)
                        , e);
            }
        }
    }

    @Override
    public void doRelease() throws Exception {
        releaseStream(inStream.get());
        releaseStream(outStream.get());
        RTPManager _rtpManager = rtpManager;
        if (_rtpManager!=null) {            
            _rtpManager.dispose();
            
        }
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
        stream = new InboundStream(address, port, this);
        inStream.set(stream);
        return stream;
    }
    
    private void releaseStream(RtpStream stream) {
        if (stream!=null)
            stream.release();
    }

    public OutgoingRtpStream getOutgoingRtpStream(Node owner) {
        OutboundStream stream =  outStream.get();
        releaseStream(stream);
        stream = new OutboundStream(address, port, this);
        outStream.set(stream);
        return stream;
    }

    public InOutRtpStream getInOutRtpStream(Node owner) {        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RtpAddress reserveAddress(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unreserveAddress(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void incHandledBytes(RtpStream stream, long bytes) {
        getManager().incHandledBytes(stream, bytes);
    }

    public void incHandledPackets(RtpStream stream, long packets) {
        getManager().incHandledPackets(stream, packets);
    }

    public void releaseStream(RtpAddress stream) {
    }
    
    private class OutboundStream extends OutgoingRtpStreamImpl {

        public OutboundStream(InetAddress address, int portNumber, RtpStreamManager manager) {
            super(address, portNumber, null);
            setManager(manager);
        }

        @Override
        protected RTPManager createRtpManager() throws Exception {
            if (rtpManager==null) {
                InOutRtpStreamImpl.this.open(remoteHost, remotePort);
//                throw new Exception("Can't create RTPManager because of InOutRtpStream not opened");
            }
            return rtpManager;
        }

        @Override
        public void doRelease() throws Exception {
            super.doRelease();
            outStream.compareAndSet(this, null);
        }                

        @Override
        protected void releaseRtpManager() {
            //
        }                        
    }

    private class InboundStream extends IncomingRtpStreamImpl {
        private volatile boolean waitingForOpen = true;
        private volatile boolean opened = false;

        public InboundStream(InetAddress addr, int port, RtpStreamManager manager) {
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
            synchronized(InOutRtpStreamImpl.this) {
                if (rtpManager==null) {
                    waitingForOpen = true;
                    if (logger.isDebugEnabled())
                        logger.debug("Can't open connection, because of RTPManager not initialized. Waiting...");
                } else {
                    waitingForOpen = false;
                    if (logger.isDebugEnabled())
                        logger.debug(String.format(
                                    "Trying to open incoming RTP stream, using existed RTPManager, "
                                            + "from the remote host (%s) and port (%s)"
                                    , remoteHost, remotePort));
                    rtpManager.addReceiveStreamListener(this);
                }
            }
        }

        @Override
        public void doRelease() throws Exception {
            super.doRelease();
            inStream.compareAndSet(this, null);
        }

        @Override
        protected void releaseRtpManager() {
        }


//        @Override
//        public void release() {
//            synchronized(this) {               
//                if (inStream.compareAndSet(this, null)) {
//                    if (logger.isDebugEnabled())
//                        logger.debug("Releasing stream...");
//                    if (rtpManager!=null) {
//                        rtpManager.removeReceiveStreamListener(this);            
//                        rtpManager.removeTargets("Disconnected");
//
//                    } if (logger.isDebugEnabled())
//                        logger.debug("Stream released");
//                }
//            }
//        }
    }
    
}
