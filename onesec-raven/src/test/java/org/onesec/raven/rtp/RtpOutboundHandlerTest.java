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
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.net.impl.BufHolderToBufDecoder;

/**
 *
 * @author Mikhail Titov
 */
public class RtpOutboundHandlerTest extends Assert {
    private final static int BIND_PORT = 7777;
    private final static byte[] TEST_BUFFER = new byte[]{1,2,3,4,5};
    private final AtomicReference<byte[]> receivedBytes = new AtomicReference<>();
    
    @Test
    public void test() throws Exception {
        InetAddress localhost = Inet4Address.getLocalHost();
        NioEventLoopGroup group = new NioEventLoopGroup(4);
        //server
        Channel server = createServerBootstrap(group).bind(localhost, BIND_PORT).await().channel();
        //client
        final Channel client = createClientBootstrap(group).connect(localhost, BIND_PORT).await().channel();
        RtpOutboundHandler handler = client.pipeline().get(RtpOutboundHandler.class);
        assertNotNull(handler);
        handler.getOutStream().write(TEST_BUFFER, 0, TEST_BUFFER.length);
        
        server.closeFuture().await();
        client.close();
        assertArrayEquals(TEST_BUFFER, receivedBytes.get());
    }
    
    private Bootstrap createServerBootstrap(NioEventLoopGroup group) {
        return createDatagramBootstrap(group).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new BufHolderToBufDecoder()).addLast(new Handler());
            }
        });
    }

    private Bootstrap createClientBootstrap(NioEventLoopGroup group) {
        return createDatagramBootstrap(group).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new RtpOutboundHandler(ch, PooledByteBufAllocator.DEFAULT));
            }
        });
    }

    private Bootstrap createDatagramBootstrap(NioEventLoopGroup group) {
        return new Bootstrap().group(group).channel(NioDatagramChannel.class);
    }
    
    private class Handler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[]{0,0,0,0,0};
                buf.readBytes(bytes);
                receivedBytes.set(bytes);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.channel().close();
        }
    }

}