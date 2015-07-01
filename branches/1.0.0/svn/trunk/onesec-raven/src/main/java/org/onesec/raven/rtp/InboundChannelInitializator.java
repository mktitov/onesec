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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class InboundChannelInitializator extends ChannelInboundHandlerAdapter {
    private final InetAddress sender;
    private final LoggerHelper logger;

    public InboundChannelInitializator(InetAddress sender, LoggerHelper logger) {
        this.sender = sender;
        this.logger = logger;
    }
    
    protected abstract void initChannel(InetSocketAddress sender, Channel ctx);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DatagramPacket) {
            final InetSocketAddress senderSocket = ((DatagramPacket)msg).sender();
            if (logger.isTraceEnabled()) 
                logger.debug("Received initial packet from ({})", senderSocket.toString());
            if (sender==null || sender.equals(senderSocket.getAddress())) {
                initChannel(senderSocket, ctx.channel());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg);
            }
        }
    }
}
