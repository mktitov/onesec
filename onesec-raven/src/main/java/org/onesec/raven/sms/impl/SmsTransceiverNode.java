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
package org.onesec.raven.sms.impl;

import com.logica.smpp.pdu.Address;
import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.sms.BindMode;
import org.onesec.raven.sms.queue.ShortTextMessage;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SmsTransceiverNode extends BaseNode implements DataPipe {
    
    @NotNull @Parameter()
    private String addrRange;
    @NotNull @Parameter()
    private String fromAddr;
    @NotNull @Parameter(defaultValue = "TRANSMITTER")
    private BindMode bindMode;
    @NotNull @Parameter()
    private String bindAddr;
    @NotNull @Parameter(defaultValue = "5016")
    private Integer bindPort;
    @NotNull @Parameter(defaultValue = "5")
    private Byte bindTon;
    @NotNull @Parameter(defaultValue = "0")
    private Byte bindNpi;
    @NotNull @Parameter(defaultValue = "30000")
    private Integer bindTimeout;
    @NotNull @Parameter(defaultValue = "60000")
    private Integer rebindInterval;
    @NotNull @Parameter(defaultValue = "90000")
    private Integer enquireTimeout;
    @NotNull @Parameter(defaultValue = "3")
    private Integer maxEnquireAttempts;
    @NotNull @Parameter(defaultValue = "100")
    private Integer soTimeout;
    @NotNull @Parameter(defaultValue = "100")
    private Integer receiveTimeout;
    @NotNull @Parameter(defaultValue = "90000")
    private Integer noRcvTimeout;
    @NotNull @Parameter(defaultValue = "30000")
    private Integer throttledDelay;
    @NotNull @Parameter(defaultValue = "30000")
    private Integer mesThrottledDelay;
    @NotNull @Parameter(defaultValue = "30000")
    private Integer queueFullDelay;
    @NotNull @Parameter(defaultValue = "30000")
    private Integer mesQueueFullDelay;
    @NotNull @Parameter(defaultValue = "10")
    private Integer onceSend;
    @NotNull @Parameter(defaultValue = "0")
    private Byte esmClass;
    @NotNull @Parameter(defaultValue = "0")
    private Byte protocolId;
    @NotNull @Parameter(defaultValue = "0")
    private Byte priorityFlag;
    @NotNull @Parameter(defaultValue = "0")
    private Byte registeredDelivery;
    @NotNull @Parameter(defaultValue = "0")
    private Byte replaceIfPresentFlag;
    @NotNull @Parameter(defaultValue = "8")
    private Byte dataCoding;
    @NotNull @Parameter(defaultValue = "0")
    private Byte smDefaultMsgId;
    @NotNull @Parameter(defaultValue = "1")
    private Byte dstTon;
    @NotNull @Parameter(defaultValue = "1")
    private Byte dstNpi;
    @NotNull @Parameter(defaultValue = "5")
    private Byte srcTon;
    @NotNull @Parameter(defaultValue = "0")
    private Byte srcNpi;
    @NotNull @Parameter(defaultValue = "cp1251")
    private String messageCP;
    @NotNull @Parameter(defaultValue = "false")
    private Boolean use7bit;
    @NotNull @Parameter(defaultValue = "3")
    private Integer longSmMode;
    @NotNull @Parameter(defaultValue = "10")
    private Integer maxUnconfirmed;
    @NotNull @Parameter
    private String systemId;
    @NotNull @Parameter
    private String password;
    @NotNull @Parameter(defaultValue = "100")
    private Integer maxMessagesInQueue;
    @NotNull @Parameter(defaultValue = "600000")
    private Integer mesLifeTime;
    @NotNull @Parameter(defaultValue = "5")
    private Integer maxSubmitAttempts;
    @NotNull @Parameter(defaultValue = "60000")
    private Long maxWaitForResp;

    private ReentrantReadWriteLock dataLock;
    private Condition messageProcessed;
//    private 

    @Override
    protected void initFields() {
        super.initFields();
        dataLock = new ReentrantReadWriteLock();
        messageProcessed = dataLock.writeLock().newCondition();
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (data==null)
            DataSourceHelper.sendDataToConsumers(dataSource, data, context);
        
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        return null;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        return true;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
    
    public void messageHandled(ShortTextMessage message, boolean success) {
        
    }

    public Boolean getStopProcessingOnError() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public BindMode getBindMode() {
        return bindMode;
    }

    public void setBindMode(BindMode bindMode) {
        this.bindMode = bindMode;
    }

    public String getBindAddr() {
        return bindAddr;
    }

    public void setBindAddr(String bindAddr) {
        this.bindAddr = bindAddr;
    }

    public Integer getBindPort() {
        return bindPort;
    }

    public void setBindPort(Integer bindPort) {
        this.bindPort = bindPort;
    }

    public Byte getBindTon() {
        return bindTon;
    }

    public void setBindTon(Byte bindTon) {
        this.bindTon = bindTon;
    }

    public Byte getBindNpi() {
        return bindNpi;
    }

    public void setBindNpi(Byte bindNpi) {
        this.bindNpi = bindNpi;
    }

    public String getAddrRange() {
        return addrRange;
    }

    public void setAddrRange(String addrRange) {
        this.addrRange = addrRange;
    }

    public String getFromAddr() {
        return fromAddr;
    }

    public void setFromAddr(String fromAddr) {
        this.fromAddr = fromAddr;
    }

    public Integer getBindTimeout() {
        return bindTimeout;
    }

    public void setBindTimeout(Integer bindTimeout) {
        this.bindTimeout = bindTimeout;
    }

    public Integer getRebindInterval() {
        return rebindInterval;
    }

    public void setRebindInterval(Integer rebindInterval) {
        this.rebindInterval = rebindInterval;
    }

    public Integer getEnquireTimeout() {
        return enquireTimeout;
    }

    public void setEnquireTimeout(Integer enquireTimeout) {
        this.enquireTimeout = enquireTimeout;
    }

    public Integer getMaxEnquireAttempts() {
        return maxEnquireAttempts;
    }

    public void setMaxEnquireAttempts(Integer maxEnquireAttempts) {
        this.maxEnquireAttempts = maxEnquireAttempts;
    }

    public Integer getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(Integer soTimeout) {
        this.soTimeout = soTimeout;
    }

    public Integer getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Integer receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public Integer getNoRcvTimeout() {
        return noRcvTimeout;
    }

    public void setNoRcvTimeout(Integer noRcvTimeout) {
        this.noRcvTimeout = noRcvTimeout;
    }

    public Integer getThrottledDelay() {
        return throttledDelay;
    }

    public void setThrottledDelay(Integer throttledDelay) {
        this.throttledDelay = throttledDelay;
    }

    public Integer getMesThrottledDelay() {
        return mesThrottledDelay;
    }

    public void setMesThrottledDelay(Integer mesThrottledDelay) {
        this.mesThrottledDelay = mesThrottledDelay;
    }

    public Integer getQueueFullDelay() {
        return queueFullDelay;
    }

    public void setQueueFullDelay(Integer queueFullDelay) {
        this.queueFullDelay = queueFullDelay;
    }

    public Integer getMesQueueFullDelay() {
        return mesQueueFullDelay;
    }

    public void setMesQueueFullDelay(Integer mesQueueFullDelay) {
        this.mesQueueFullDelay = mesQueueFullDelay;
    }

    public Integer getOnceSend() {
        return onceSend;
    }

    public void setOnceSend(Integer onceSend) {
        this.onceSend = onceSend;
    }

    public Byte getEsmClass() {
        return esmClass;
    }

    public void setEsmClass(Byte esmClass) {
        this.esmClass = esmClass;
    }

    public Byte getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(Byte protocolId) {
        this.protocolId = protocolId;
    }

    public Byte getPriorityFlag() {
        return priorityFlag;
    }

    public void setPriorityFlag(Byte priorityFlag) {
        this.priorityFlag = priorityFlag;
    }

    public Byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public void setRegisteredDelivery(Byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public Byte getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }

    public void setReplaceIfPresentFlag(Byte replaceIfPresentFlag) {
        this.replaceIfPresentFlag = replaceIfPresentFlag;
    }

    public Byte getDataCoding() {
        return dataCoding;
    }

    public void setDataCoding(Byte dataCoding) {
        this.dataCoding = dataCoding;
    }

    public Byte getSmDefaultMsgId() {
        return smDefaultMsgId;
    }

    public void setSmDefaultMsgId(Byte smDefaultMsgId) {
        this.smDefaultMsgId = smDefaultMsgId;
    }

    public Byte getDstTon() {
        return dstTon;
    }

    public void setDstTon(Byte dstTon) {
        this.dstTon = dstTon;
    }

    public Byte getDstNpi() {
        return dstNpi;
    }

    public void setDstNpi(Byte dstNpi) {
        this.dstNpi = dstNpi;
    }

    public Byte getSrcTon() {
        return srcTon;
    }

    public void setSrcTon(Byte srcTon) {
        this.srcTon = srcTon;
    }

    public Byte getSrcNpi() {
        return srcNpi;
    }

    public void setSrcNpi(Byte srcNpi) {
        this.srcNpi = srcNpi;
    }

    public String getMessageCP() {
        return messageCP;
    }

    public void setMessageCP(String messageCP) {
        this.messageCP = messageCP;
    }

    public Boolean getUse7bit() {
        return use7bit;
    }

    public void setUse7bit(Boolean use7bit) {
        this.use7bit = use7bit;
    }

    public Integer getLongSmMode() {
        return longSmMode;
    }

    public void setLongSmMode(Integer longSmMode) {
        this.longSmMode = longSmMode;
    }

    public Integer getMaxUnconfirmed() {
        return maxUnconfirmed;
    }

    public void setMaxUnconfirmed(Integer maxUnconfirmed) {
        this.maxUnconfirmed = maxUnconfirmed;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaxMessagesInQueue() {
        return maxMessagesInQueue;
    }

    public void setMaxMessagesInQueue(Integer maxMessagesInQueue) {
        this.maxMessagesInQueue = maxMessagesInQueue;
    }

    public Integer getMesLifeTime() {
        return mesLifeTime;
    }

    public void setMesLifeTime(Integer mesLifeTime) {
        this.mesLifeTime = mesLifeTime;
    }

    public Integer getMaxSubmitAttempts() {
        return maxSubmitAttempts;
    }

    public void setMaxSubmitAttempts(Integer maxSubmitAttempts) {
        this.maxSubmitAttempts = maxSubmitAttempts;
    }

    public Long getMaxWaitForResp() {
        return maxWaitForResp;
    }

    public void setMaxWaitForResp(Long maxWaitForResp) {
        this.maxWaitForResp = maxWaitForResp;
    }
}
