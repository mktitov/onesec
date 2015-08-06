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
import io.netty.channel.ChannelOutboundHandlerAdapter;
import javax.media.rtp.OutputDataStream;

/**
 *
 * @author Mikhail Titov
 */
public class RtpOutboundHandler extends ChannelOutboundHandlerAdapter {
    public final static String NAME = "OutStream";
    private final OutStream outStream;

    public RtpOutboundHandler(final Channel channel, final ByteBufAllocator bufAllocator) {
        channel.config().setAllocator(bufAllocator);
        this.outStream = new OutStream(channel);
    }

    public OutputDataStream getOutStream() {
        return outStream;
    }
    
    private class OutStream implements OutputDataStream {
        private final Channel channel;

        public OutStream(Channel channel) {
            this.channel = channel;
        }
        
        public int write(final byte[] bytes, final int offset, final int len) {
            if (len==0 || bytes==null) return 0;
            else {
                final ByteBuf buf = channel.alloc().buffer(len);
                buf.writeBytes(bytes, offset, len);
                channel.writeAndFlush(buf);
                return len;
            }
        }
    }

}
