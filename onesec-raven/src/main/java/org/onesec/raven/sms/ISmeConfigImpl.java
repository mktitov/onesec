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
package org.onesec.raven.sms;

import com.logica.smpp.pdu.Address;

/**
 *
 * @author Mikhail Titov
 */
public class ISmeConfigImpl implements ISmeConfig {
    private int bindMode = 3;
    private String bindAddr;
    private int bindPort = 5016;
    private int bindTon = 5;
    private int bindNpi = 0;
    private String addrRange = ""; //Raven
    private String fromAddr = "";
    private boolean async = true;

    public int getBindMode() {
        return bindMode;
    }

    public void setBindMode(int bindMode) {
        this.bindMode = bindMode;
    }

    public String getBindAddr() {
        return bindAddr;
    }

    public void setBindAddr(String bindAddr) {
        this.bindAddr = bindAddr;
    }

    public int getBindPort() {
        return bindPort;
    }

    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }

    public int getBindTon() {
        return bindTon;
    }

    public void setBindTon(int bindTon) {
        this.bindTon = bindTon;
    }

    public int getBindNpi() {
        return bindNpi;
    }

    public void setBindNpi(int bindNpi) {
        this.bindNpi = bindNpi;
    }

    public String getAddrRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getFromAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getAsync() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getBindTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getRebindInterval() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getEnquireTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxEnquireAttempts() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getSoTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getReceiveTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNoRcvTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getThrottledDelay() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMesThrottledDelay() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getQueueFullDelay() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMesQueueFullDelay() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getOnceSend() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSystemType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getServiceType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Address getSrcAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Address getSmscAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getValidityPeriod() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getEsmClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getProtocolId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getPriorityFlag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getRegisteredDelivery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getReplaceIfPresentFlag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getDataCoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getSmDefaultMsgId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getDstTon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getDstNpi() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getSrcTon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getSrcNpi() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMessageCP() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isUse7bit() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getLongSmMode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxUnconfirmed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
