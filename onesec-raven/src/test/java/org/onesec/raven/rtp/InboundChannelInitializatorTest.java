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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
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
public class InboundChannelInitializatorTest extends Assert {
    private final static int BIND_PORT = 1234;
    private InetAddress localhost;
    private List<Byte> bytes = Collections.synchronizedList(new ArrayList<Byte>(2));
    private final Logger log = LoggerFactory.getLogger(InboundChannelInitializatorTest.class);
    private final LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "Test logger", "", log);
    
    @Before
    public void prepare() throws Exception {
        localhost = Inet4Address.getLocalHost();
    }
    
    @Test
    public void test() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(4);
        Bootstrap serverBootstrap = createServerBootstrap(group);
        Bootstrap clientBootstrap = createClientBootstrap(group);
        
        final Channel server = createServerBootstrap(group).bind(localhost, BIND_PORT).await().channel();
        final Channel client = createClientBootstrap(group).connect(localhost, BIND_PORT).await().channel();
        System.out.println("WRITING bytes");
        client.write(new byte[]{1});
        client.write(new byte[]{2});
        System.out.println("Bytes sended");
        server.closeFuture().await();   
        client.close();
        assertEquals(2, bytes.size());
        assertArrayEquals(new Object[]{(byte)1,(byte)2}, bytes.toArray());
    }

    private Bootstrap createServerBootstrap(NioEventLoopGroup group) {
        return new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(
            new InboundChannelInitializator(localhost, logger) {
                @Override protected void initChannel(InetSocketAddress sender, Channel ctx) {
                    System.out.println("INITIALIZING CHANNEL for sender: "+sender);
                    ctx.pipeline().addLast(new InboundPacketFilter(sender));
                    ctx.pipeline().addLast(new BufHolderToBufDecoder());
                    ctx.pipeline().addLast(new InboundHandler());
                }
        });
    }

    private Bootstrap createClientBootstrap(NioEventLoopGroup group) {
        return new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new OutboundHandler());
            }
        });
    }
    
    public class InboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("RECEIVED data");
            ByteBuf buf = (ByteBuf) msg;
            byte[] _bytes = new byte[]{0};
            buf.readBytes(_bytes);
            bytes.add(_bytes[0]);
            if (bytes.size()==2)
                ctx.close();
        }
    }
    
    public class OutboundHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ByteBuf buf = ctx.alloc().buffer(1).writeBytes((byte[])msg);
            ctx.writeAndFlush(buf);
        }
    }
}