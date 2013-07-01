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
package org.onesec.raven.sms;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;

/**
 *
 * @author Mikhail Titov
 */
public interface SmsConfig {
    public BindMode getBindMode();
    public String getBindAddr();
    public int getBindPort();
//    public int getBindTon();
//    public int getBindNpi();
//    public String getAddrRange();
    public AddressRange getServeAddr();
    public String getFromAddr();
    public boolean getAsync();
    public int getBindTimeout();
    public int getRebindInterval();
    public int getEnquireTimeout();
    public int getMaxEnquireAttempts();
    public int getSoTimeout();
    public int getReceiveTimeout();
    public int getNoRcvTimeout();
    public int getThrottledDelay();
    public int getMesThrottledDelay();
    public int getQueueFullDelay();
    public int getMesQueueFullDelay();
    public int getOnceSend();
    public String getSystemType();
    public String getServiceType();
    public Address getSrcAddr();
    public Address getSmscAddr();
    public String getValidityPeriod();
    public byte getEsmClass();
    public byte getProtocolId();
    public byte getPriorityFlag();
    public byte getRegisteredDelivery();
    public byte getReplaceIfPresentFlag();
    public byte getDataCoding();
    public byte getSmDefaultMsgId();
    public byte getDstTon();
    public byte getDstNpi();
//    public byte getSrcTon();
//    public byte getSrcNpi();
    public String getMessageCP();
    public boolean isUse7bit();
    public int getLongSmMode();
    public int getMaxUnconfirmed();
    public String getSystemId();
    public String getPassword();
}
