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

import com.sun.corba.se.pept.transport.InboundConnectionCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.Inet4Address;
import java.net.InetAddress;
import org.junit.Test;
import org.onesec.raven.net.impl.BufHolderToBufDecoder;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class NettyRtpConnectorTest {
    private final Logger logger = LoggerFactory.getLogger(NettyRtpConnectorTest.class);
    private final LoggerHelper loggerHelper = new LoggerHelper(
            LogLevel.TRACE, "RtpConnector", "RtpConnector", logger);
    private InetAddress localhost;
    private NioEventLoopGroup group;
    
    public void prepare() throws Exception {
        localhost = Inet4Address.getLocalHost();
        group = new NioEventLoopGroup(4);
    }

    @Test
    public void test() {
        
    }
    
    private ChannelFuture createChannel(int port) {
        return new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new BufHolderToBufDecoder());
                ch.pipeline().addLast(new RtpInboundHandler(ch));
                ch.pipeline().addLast(new RtpOutboundHandler(ch));
            }
        }).bind(localhost, port);
    }
    
}