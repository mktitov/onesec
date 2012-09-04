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

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractPacketProcessor implements PacketProcessor {
    protected final AtomicBoolean validFlag = new AtomicBoolean(true);
    private final SocketAddress address;
    private final boolean needInboundProcessing;
    private final boolean needOutboundProcessing;
    private final boolean serverSideProcessor;
    private final boolean datagramProcessor;
    private final String desc;
    protected final LoggerHelper logger;
    
    private final AtomicBoolean processing = new AtomicBoolean(false);

    public AbstractPacketProcessor(SocketAddress address, boolean needInboundProcessing
            , boolean needOutboundProcessing, boolean serverSideProcessor, boolean datagramProcessor
            , String desc
            , LoggerHelper logger) 
    {
        this.address = address;
        this.needInboundProcessing = needInboundProcessing;
        this.needOutboundProcessing = needOutboundProcessing;
        this.serverSideProcessor = serverSideProcessor;
        this.datagramProcessor = datagramProcessor;
        this.desc = desc+" ("+address.toString()+")";
        this.logger = logger;
    }
    
    public boolean isValid() {
        return validFlag.get();
    }

    public boolean isNeedInboundProcessing() {
        return needInboundProcessing;
    }

    public boolean isNeedOutboundProcessing() {
        return needOutboundProcessing;
    }

    public boolean isServerSideProcessor() {
        return serverSideProcessor;
    }

    public boolean isDatagramProcessor() {
        return datagramProcessor;
    }

    public void stopUnexpected(Throwable e) {
        if (!validFlag.compareAndSet(true, false))
            return;
        if (logger.isErrorEnabled())
            logger.error("Unexpected processing stop", e);
        doStopUnexpected(e);
    }
    
    protected abstract void doStopUnexpected(Throwable e);

    public SocketAddress getAddress() {
        return address;
    }

    public boolean isProcessing() {
        return processing.get();
    }

    public boolean changeToProcessing() {
        return processing.compareAndSet(false, true);
    }

    public void changeToUnprocessing() {
        processing.compareAndSet(true, false);
    }

    @Override
    public String toString() {
        return desc;
    }
}
