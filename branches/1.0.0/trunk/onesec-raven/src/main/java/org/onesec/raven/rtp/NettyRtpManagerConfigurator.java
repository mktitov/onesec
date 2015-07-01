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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.net.NettyEventLoopGroupProvider;
import org.onesec.raven.net.impl.BufHolderToBufDecoder;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class NettyRtpManagerConfigurator extends BaseNode implements RtpManagerConfigurator {
    
    @Service
    private static RTPManagerService rtpManagerService;
    
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private NettyEventLoopGroupProvider eventLoopGroupProvider;

    public RTPManager configureOutboundManager(InetAddress localAddress, int localPort, 
            InetAddress remoteAddress, int remotePort, LoggerHelper logger) 
        throws Exception 
    {
        return createStaticManager(localAddress, localPort, remoteAddress, remotePort, logger);
    }

    public RTPManager configureInboundManager(InetAddress localAddress, int localPort, 
            InetAddress remoteAddress, int remotePort, LoggerHelper logger) 
        throws Exception 
    {
        return remotePort != SessionAddress.ANY_PORT || remoteAddress==null?
                createStaticManager(localAddress, localPort, remoteAddress, remotePort, logger) :
                createManagerForUnknownRemotePort(localAddress, localPort, remoteAddress, logger);
    }

    private Bootstrap createBootstrap() {
        return new Bootstrap()
                .group(eventLoopGroupProvider.getEventLoopGroup())
                .channel(NioDatagramChannel.class);
    }

    private RTPManager createStaticManager(final InetAddress localAddress, final int localPort, 
            final InetAddress remoteAddress, final int remotePort, LoggerHelper logger) 
    {
        ChannelFuture rtpChannel = createStaticChannel(localAddress, localPort, remoteAddress, remotePort);
        ChannelFuture rtcpChannel = createStaticChannel(localAddress, localPort+1, remoteAddress, remotePort+1);
        final RTPManager manager = rtpManagerService.createRtpManager();
        manager.initialize(new NettyRtpConnector(logger, rtpChannel, rtcpChannel));
        return manager;
    }
    
    private ChannelFuture createStaticChannel(InetAddress localAddr, int localPort, InetAddress remoteAddr, 
            int remotePort)
    {
        return createBootstrap()
            .handler(new StaticChannelInitializer(remoteAddr, remotePort))
            .bind(localAddr, localPort);
    }

    private RTPManager createManagerForUnknownRemotePort(InetAddress localAddress, int localPort, 
            final InetAddress remoteAddress, final LoggerHelper logger) 
    {
        ChannelFuture rtpChannel = createBootstrap()
            .handler(new ChannelInitializer() {
                @Override protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(
                        new InboundChannelInitializator(remoteAddress, new LoggerHelper(logger, "Data stream. ")) {
                            @Override protected void initChannel(InetSocketAddress sender, Channel ch) {
                                ch.pipeline().addFirst(new InboundPacketFilter(sender));
                            }
                        });
                    ch.pipeline().addLast(BufHolderToBufDecoder.INSTANCE);
                    ch.pipeline().addLast(new RtpInboundHandler(ch));
                }
        }).bind(localAddress, localPort);
        ChannelFuture rtcpChannel = createBootstrap()
            .handler(new ChannelInitializer() {
                @Override protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(
                        new InboundChannelInitializator(remoteAddress, new LoggerHelper(logger, "Control stream. ")) {
                            @Override protected void initChannel(InetSocketAddress sender, Channel ch) {
                                ch.pipeline().addFirst(new InboundPacketFilter(sender));
                                ch.pipeline().addLast(new OutboundPacketBuilderHandler(
                                        remoteAddress, sender.getPort()));
                            }
                        });
                    ch.pipeline().addLast(BufHolderToBufDecoder.INSTANCE);
                    ch.pipeline().addLast(new RtpInboundHandler(ch));
                    ch.pipeline().addLast(new RtpInboundHandler(ch));
                    ch.pipeline().addLast(new RtpOutboundHandler(ch));
                }
        }).bind(localAddress, localPort+1);
        final RTPManager manager = rtpManagerService.createRtpManager();
        manager.initialize(new NettyRtpConnector(logger, rtpChannel, rtcpChannel));
        return manager;
    }
    
    public NettyEventLoopGroupProvider getEventLoopGroupProvider() {
        return eventLoopGroupProvider;
    }

    public void setEventLoopGroupProvider(NettyEventLoopGroupProvider eventLoopGroupProvider) {
        this.eventLoopGroupProvider = eventLoopGroupProvider;
    }
    
    private final class StaticChannelInitializer extends ChannelInitializer<Channel> {
        private final int remotePort;
        private final InetAddress remoteAddress;

        public StaticChannelInitializer(InetAddress remoteAddress, int remotePort) {
            this.remotePort = remotePort;
            this.remoteAddress = remoteAddress;
        }
        
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(BufHolderToBufDecoder.INSTANCE);
            ch.pipeline().addLast(new RtpInboundHandler(ch));
            ch.pipeline().addLast(new RtpOutboundHandler(ch));
            ch.pipeline().addLast(new OutboundPacketBuilderHandler(remoteAddress, remotePort));
        }
    }
}
