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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushSourceStream;
import static javax.media.protocol.SourceStream.LENGTH_UNKNOWN;
import javax.media.protocol.SourceTransferHandler;

/**
 *
 * @author Mikhail Titov
 */
public class RtpInboundHandler extends ChannelInboundHandlerAdapter {
    public final static String NAME = "InStream";
    private final static Object[] EMPTY_CONTROLS = new Object[0];
    private final static int MINIMUM_TRANSFER_SIZE = 2048;
    
//    private final RingQueue<ByteBuf> buffers = new RingQueueImpl<ByteBuf>(5);
    private final BlockingQueue<ByteBuf> buffers = new ArrayBlockingQueue<>(10);
    private final InStream inStream = new InStream();
    private final Channel channel;
    private volatile SourceTransferHandler handler;

    public RtpInboundHandler(Channel channel, final ByteBufAllocator bufAllocator) {
        channel.config().setAutoRead(false);
        channel.config().setAllocator(bufAllocator);
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!buffers.offer((ByteBuf) msg)) 
            ReferenceCountUtil.release(msg);
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        final SourceTransferHandler _handler = handler;
        if (_handler!=null)
            _handler.transferData(inStream);
    }    

    public PushSourceStream getInStream() {
        channel.config().setAutoRead(true);
        return inStream;
    }
    
    private class InStream implements PushSourceStream {
        
        public int read(final byte[] bytes, final int offset, final int len) throws IOException {
            final ByteBuf buf = buffers.peek();
            if (buf==null) return 0;
            else {
                final int cnt = Math.min(len, buf.readableBytes());
                buf.readBytes(bytes, offset, cnt);
                if (!buf.isReadable()) {
                    buffers.poll();
                    buf.release();
                }
                return cnt;
            }
        }

        public int getMinimumTransferSize() {
            return MINIMUM_TRANSFER_SIZE;
        }

        public void setTransferHandler(SourceTransferHandler ahandler) {
            handler = ahandler;
        }

        public ContentDescriptor getContentDescriptor() {
            return null;
        }

        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        public boolean endOfStream() {
            return false;
        }

        public Object[] getControls() {
            return EMPTY_CONTROLS;
        }

        public Object getControl(String paramString) {
            return null;
        }
    }
}
