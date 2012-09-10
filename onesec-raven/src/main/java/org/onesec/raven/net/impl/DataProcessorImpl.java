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

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorImpl extends AbstractDataProcessor {

    public DataProcessorImpl(Node owner, AbstractPacketDispatcher packetDispatcher, LoggerHelper logger) 
    {
        super(owner, packetDispatcher, logger);
    }

    @Override
    protected void doProcessData(SelectionKey key) throws Exception {
        final PacketProcessor packetProcessor = (PacketProcessor) key.attachment();
        if (key.isReadable()) {
//            if (key.isValid())
//                logger.debug(key.readyOps()+":"+key.attachment());
            packetProcessor.processInboundBuffer((ReadableByteChannel)key.channel());
        } if (key.isWritable()) 
            packetProcessor.processOutboundBuffer((WritableByteChannel)key.channel());
    }
}
