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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;

/**
 *
 * @author Mikhail Titov
 */
public class InboundPacketFilter extends ChannelInboundHandlerAdapter {
    private final InetSocketAddress sender;

    public InboundPacketFilter(InetSocketAddress sender) {
        this.sender = sender;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        
        if (((DatagramPacket)msg).sender().equals(sender))
            ctx.fireChannelRead(msg);
    }
}
