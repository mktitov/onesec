/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onesec.raven.net.impl;

import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *
 * @author Mikhail Titov
 */
@ChannelHandler.Sharable
public class BufHolderToBufDecoder extends ChannelInboundHandlerAdapter {
    
    public final static BufHolderToBufDecoder INSTANCE = new BufHolderToBufDecoder();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBufHolder) 
            ctx.fireChannelRead(((ByteBufHolder)msg).content());
    }
}
