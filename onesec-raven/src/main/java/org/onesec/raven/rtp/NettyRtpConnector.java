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
package org.onesec.raven.rtp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.io.IOException;
import javax.media.protocol.PushSourceStream;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class NettyRtpConnector implements RTPConnector {
    private final static Object[] EMPTY_CONTROLS = new Object[0];
    private final static int MINIMUM_TRANSFER_SIZE = 2048;
    private final LoggerHelper logger;
    private final ChannelFuture dataChannel;
    private final ChannelFuture controlChannel;
    
    public NettyRtpConnector(LoggerHelper logger, ChannelFuture dataChannel, ChannelFuture controlChannel) {
        this.logger = logger;
        this.dataChannel = dataChannel;
        this.controlChannel = controlChannel;
    }
    
    private Channel getChannel(ChannelFuture future) throws IOException {
        if (future.isDone()) {
            if (future.isSuccess()) return future.channel();
            else throw new IOException(future.cause());
        } else try {
            return getChannel(future.await());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public PushSourceStream getDataInputStream() throws IOException {
        return getChannel(dataChannel).pipeline().get(RtpInboundHandler.class).getInStream();
    }

    public OutputDataStream getDataOutputStream() throws IOException {
        return getChannel(dataChannel).pipeline().get(RtpOutboundHandler.class).getOutStream();
    }

    public PushSourceStream getControlInputStream() throws IOException {
        return getChannel(controlChannel).pipeline().get(RtpInboundHandler.class).getInStream();
    }

    public OutputDataStream getControlOutputStream() throws IOException {
        return getChannel(controlChannel).pipeline().get(RtpOutboundHandler.class).getOutStream();
    }

    public void close() {
        closeChannel(dataChannel);
        closeChannel(controlChannel);
    }
    
    private void closeChannel(ChannelFuture channel) {
        try {
            getChannel(channel).close();
        } catch (IOException ex) {
            if (logger.isErrorEnabled())
                logger.error("Close channel error", ex);
        }
    }
    
    public void setReceiveBufferSize(int paramInt) throws IOException {
    }

    public int getReceiveBufferSize() {
        return 0;
    }

    public void setSendBufferSize(int paramInt) throws IOException {
    }

    public int getSendBufferSize() {
        return 0;
    }

    public double getRTCPBandwidthFraction() {
        return 0.0D;
    }

    public double getRTCPSenderBandwidthFraction() {
        return 0.0D;
    }
}
