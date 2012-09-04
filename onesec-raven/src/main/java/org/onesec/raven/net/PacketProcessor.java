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
package org.onesec.raven.net;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author Mikhail Titov
 */
public interface PacketProcessor {
    public boolean isValid();
//    public boolean processInboundBuffer(ByteBuffer buffer);
//    public void processOutboundBuffer(ByteBuffer buffer, SocketChannel channel);
    public void processInboundBuffer(ReadableByteChannel channel);
    public void processOutboundBuffer(WritableByteChannel channel);
    public boolean isNeedInboundProcessing();
    public boolean isNeedOutboundProcessing();
    public boolean isServerSideProcessor();
    public boolean isDatagramProcessor();
    public boolean hasPacketForOutboundProcessing();
    public void stopUnexpected(Throwable e);
    public SocketAddress getAddress();
    public boolean isProcessing();
    public boolean changeToProcessing();
    public void changeToUnprocessing(); 
    public void stop();
}
