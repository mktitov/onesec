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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceTransferHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.net.impl.BufHolderToBufDecoder;

/**
 *
 * @author Mikhail Titov
 */
public class RtpInboundHandlerTest extends Assert {
    private final static int BIND_PORT = 7777;
    private final static byte[] TEST_BUFFER = new byte[]{1,2,3,4,5};
    private volatile Channel serverChannel;
    private volatile Channel clientChannel;
    private AtomicReference<byte[]> receivedBuffer = new AtomicReference<byte[]>();
    private InetAddress localhost;
    
    @Before
    public void prepare() throws Exception {
        localhost = Inet4Address.getLocalHost();
    }
    
    @Test
    public void test() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(4);
        Bootstrap serverBootstrap = createServerBootstrap(group);
        Bootstrap clientBootstrap = createClientBootstrap(group);
        
        ChannelFuture future = serverBootstrap.bind(Inet4Address.getLocalHost(), BIND_PORT);
        future.await();
        serverChannel = future.channel();
        RtpInboundHandler handler = serverChannel.pipeline().get(RtpInboundHandler.class);
        assertNotNull(handler);
        handler.getInStream().setTransferHandler(new Handler());
        
//        ChannelFuture clientFuture = clientBootstrap.connect(Inet4Address.getLocalHost(), BIND_PORT);
        ChannelFuture clientFuture = clientBootstrap.bind(Inet4Address.getLocalHost(), BIND_PORT+1);
        clientChannel = clientFuture.await().channel();
        assertNotNull(clientChannel);
        InetSocketAddress addr = new InetSocketAddress(localhost, BIND_PORT);
        ByteBuf writeBuf = clientChannel.alloc().buffer(5).writeBytes(TEST_BUFFER);
        clientChannel.writeAndFlush(new DatagramPacket(writeBuf, addr)).addListener(new GenericFutureListener() {
            public void operationComplete(Future future) throws Exception {
                clientChannel.close();
            }
        });
        
        serverChannel.closeFuture().await();
        assertNotNull(receivedBuffer.get());
        assertArrayEquals(TEST_BUFFER, receivedBuffer.get());
        
    }

    private Bootstrap createServerBootstrap(NioEventLoopGroup group) {
        return createDatagramBootstrap(group).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new BufHolderToBufDecoder()).addLast(new RtpInboundHandler(ch, PooledByteBufAllocator.DEFAULT));
            }
        });
    }

    private Bootstrap createClientBootstrap(NioEventLoopGroup group) {
        return createDatagramBootstrap(group).handler(new ChannelInitializer() {
            @Override protected void initChannel(Channel ch) throws Exception {
            }
        });
    }

    private Bootstrap createDatagramBootstrap(NioEventLoopGroup group) {
        return new Bootstrap().group(group).channel(NioDatagramChannel.class);
    }

    private class Handler implements SourceTransferHandler {
        public void transferData(PushSourceStream stream) {
            final byte[] buf = new byte[]{0,0,0,0,0};
            try {
                int len = stream.read(buf, 0, buf.length);
                receivedBuffer.set(buf);
                assertEquals(TEST_BUFFER.length, len);
                assertEquals(0, stream.read(new byte[]{0}, 0, 1));
                serverChannel.close();
            } catch (IOException ex) {
                Logger.getLogger(RtpInboundHandlerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }

    
}