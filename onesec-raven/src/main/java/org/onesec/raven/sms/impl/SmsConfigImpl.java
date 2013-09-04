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
package org.onesec.raven.sms.impl;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.sms.BindMode;
import org.onesec.raven.sms.SmsConfig;

/**
 *
 * @author Mikhail Titov
 */
public class SmsConfigImpl implements SmsConfig {
    private final BindMode bindMode;
    private final String bindAddr;
    private final int bindPort;
    private final AddressRange serveAddr;
    private final String fromAddr;
    private final int bindTimeout;
    private final int rebindInterval;
    private final int enquireTimeout;
    private final int maxEnquireAttempts;
    private final int soTimeout;
    private final int receiveTimeout;
    private final int noRcvTimeout;
    private final int throttledDelay;
    private final int mesThrottledDelay;
    private final int queueFullDelay;
    private final int mesQueueFullDelay;
    private final int onceSend;
    private final byte esmClass;
    private final byte protocolId;
    private final byte priorityFlag;
    private final byte registeredDelivery;
    private final byte replaceIfPresentFlag;
    private final byte dataCoding;
    private final byte smDefaultMsgId;
    private final byte dstTon;
    private final byte dstNpi;
    private final Address srcAddr;
    private final String messageCP;
    private final boolean use7bit;
    private final int longSmMode;
    private final int maxUnconfirmed;
    private final String systemId;
    private final String password;
    private final int maxMessagesInQueue;
    private final int mesLifeTime;
    private final int maxSubmitAttempts;
    private final long maxWaitForResp;
    private final long maxMessagesPerTimeUnit;
    private final TimeUnit maxMessagesTimeUnit;
    private final long maxMessagesTimeQuantity;

    public SmsConfigImpl(SmsTransceiverNode node) throws Exception {
        bindMode = node.getBindMode();
        bindAddr = node.getBindAddr();
        bindPort = node.getBindPort();
        fromAddr = node.getFromAddr();
        bindTimeout = node.getBindTimeout();
        rebindInterval = node.getRebindInterval();
        enquireTimeout = node.getEnquireTimeout();
        maxEnquireAttempts = node.getMaxEnquireAttempts();
        soTimeout = node.getSoTimeout();
        receiveTimeout = node.getReceiveTimeout();
        noRcvTimeout = node.getNoRcvTimeout();
        throttledDelay = node.getThrottledDelay();
        mesThrottledDelay = node.getMesThrottledDelay();
        queueFullDelay = node.getQueueFullDelay();
        mesQueueFullDelay = node.getQueueFullDelay();
        onceSend = node.getOnceSend();
        esmClass = node.getEsmClass();
        protocolId = node.getProtocolId();
        priorityFlag = node.getPriorityFlag();
        registeredDelivery = node.getRegisteredDelivery();
        replaceIfPresentFlag = node.getReplaceIfPresentFlag();
        dataCoding = node.getDataCoding();
        smDefaultMsgId = node.getSmDefaultMsgId();
        dstTon = node.getDstTon();
        dstNpi = node.getDstNpi();
        messageCP = node.getMessageCP();
        use7bit = node.getUse7bit();
        longSmMode = node.getLongSmMode();
        maxUnconfirmed = node.getMaxUnconfirmed();
        systemId = node.getSystemId();
        password = node.getPassword();
        srcAddr = new Address(node.getSrcTon(), node.getSrcNpi(), fromAddr);
        serveAddr = new AddressRange(node.getBindTon(), node.getBindNpi(), node.getAddrRange());
        maxMessagesInQueue = node.getMaxMessagesInQueue();
        mesLifeTime = node.getMesLifeTime();
        maxSubmitAttempts = node.getMaxSubmitAttempts();
        maxWaitForResp = node.getMaxWaitForResp();
        maxMessagesPerTimeUnit = node.getMaxMessagesPerTimeUnit();
        maxMessagesTimeUnit = node.getMaxMessagesTimeUnit();
        maxMessagesTimeQuantity = node.getMaxMessagesTimeQuantity();
    }

    public BindMode getBindMode() {
        return bindMode;
    }

    public String getBindAddr() {
        return bindAddr;
    }

    public int getBindPort() {
        return bindPort;
    }

//    public int getBindTon() {
//        return bindTon;
//    }
//
//    public int getBindNpi() {
//        return bindNpi;
//    }
//
//    public String getAddrRange() {
//        return addrRange;
//    }

    public AddressRange getServeAddr() {
        return serveAddr;
    }

    public String getFromAddr() {
        return fromAddr;
    }

    public boolean getAsync() {
        return true;
    }

    public int getBindTimeout() {
        return bindTimeout;
    }

    public int getRebindInterval() {
        return rebindInterval;
    }

    public int getEnquireTimeout() {
        return enquireTimeout;
    }

    public int getMaxEnquireAttempts() {
        return maxEnquireAttempts;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public int getReceiveTimeout() {
        return receiveTimeout;
    }

    public int getNoRcvTimeout() {
        return noRcvTimeout;
    }

    public int getThrottledDelay() {
        return throttledDelay;
    }

    public int getMesThrottledDelay() {
        return mesThrottledDelay;
    }

    public int getQueueFullDelay() {
        return queueFullDelay;
    }

    public int getMesQueueFullDelay() {
        return mesQueueFullDelay;
    }

    public int getOnceSend() {
        return onceSend;
    }

    public String getSystemType() {
        return "";
    }

    public String getServiceType() {
        return "";
    }

    public Address getSrcAddr() {
        return srcAddr;
    }

    public Address getSmscAddr() {
        return null;
    }

    public String getValidityPeriod() {
        return "";
    }

    public byte getEsmClass() {
        return esmClass;
    }

    public byte getProtocolId() {
        return protocolId;
    }

    public byte getPriorityFlag() {
        return priorityFlag;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public byte getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }

    public byte getDataCoding() {
        return dataCoding;
    }

    public byte getSmDefaultMsgId() {
        return smDefaultMsgId;
    }

    public byte getDstTon() {
        return dstTon;
    }

    public byte getDstNpi() {
        return dstNpi;
    }

    public String getMessageCP() {
        return messageCP;
    }

    public boolean isUse7bit() {
        return use7bit;
    }

    public int getLongSmMode() {
        return longSmMode;
    }

    public int getMaxUnconfirmed() {
        return maxUnconfirmed;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxMessagesInQueue() {
        return maxMessagesInQueue;
    }

    public int getMesLifeTime() {
        return mesLifeTime;
    }

    public int getMaxSubmitAttempts() {
        return maxSubmitAttempts;
    }

    public long getMaxWaitForResp() {
        return maxWaitForResp;
    }

    public long getMaxMessagesPerTimeUnit() {
        return maxMessagesPerTimeUnit;
    }

    public TimeUnit getMaxMessagesTimeUnit() {
        return maxMessagesTimeUnit;
    }

    public long getMaxMessagesTimeQuantity() {
        return maxMessagesTimeQuantity;
    }
    
}
