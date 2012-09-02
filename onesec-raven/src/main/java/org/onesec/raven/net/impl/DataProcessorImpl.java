/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.net.impl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorImpl extends AbstractDataProcessor {

    public DataProcessorImpl(Node owner, LoggerHelper logger, int bufferSize, ByteBufferPool byteBufferPool) 
    {
        super(owner, logger, bufferSize, byteBufferPool);
    }

    @Override
    protected void doProcessData(SelectionKey key, ByteBuffer buffer) throws Exception {
        final PacketProcessor packetProcessor = (PacketProcessor) key.attachment();
        final SocketChannel channel = (SocketChannel) key.channel();
        buffer.clear();
        if (key.isReadable()) {
            if (channel.read(buffer)>0)
                packetProcessor.processInboundBuffer(buffer);
        }
        if (key.isWritable()) {
            buffer.clear();
            packetProcessor.processOutboundBuffer(buffer, channel);
        }
    }
}
