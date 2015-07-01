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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.sms.BindMode;
import org.onesec.raven.sms.queue.InQueue;
import org.onesec.raven.sms.queue.OutQueue;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ChildAttributesValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SmsTransceiverNode extends AbstractSafeDataPipe {
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String smscBindAttributes;
    @NotNull @Parameter(parent = "smscBindAttributes")
    private String addrRange;
    @NotNull @Parameter(parent = "smscBindAttributes")
    private String fromAddr;
    @NotNull @Parameter(defaultValue = "TRANSMITTER", parent = "smscBindAttributes")
    private BindMode bindMode;
    @NotNull @Parameter(parent = "smscBindAttributes")
    private String bindAddr;
    @NotNull @Parameter(defaultValue = "5016", parent = "smscBindAttributes")
    private Integer bindPort;
    @NotNull @Parameter(defaultValue = "5", parent = "smscBindAttributes")
    private Byte bindTon;
    @NotNull @Parameter(defaultValue = "0", parent = "smscBindAttributes")
    private Byte bindNpi;
    @NotNull @Parameter(defaultValue = "30000", parent = "smscBindAttributes")
    private Integer bindTimeout;
    @NotNull @Parameter(defaultValue = "60000", parent = "smscBindAttributes")
    private Integer rebindInterval;
    @NotNull @Parameter(defaultValue = "60000", parent = "smscBindAttributes")
    private Integer rebindOnTimeoutInterval;
    @NotNull @Parameter(defaultValue = "90000", parent = "smscBindAttributes")
    private Long enquireInterval;
    @NotNull @Parameter(defaultValue = "100", parent = "smscBindAttributes")
    private Integer enquireTimeout;
    @NotNull @Parameter(defaultValue = "3", parent = "smscBindAttributes")
    private Integer maxEnquireAttempts;
    @NotNull @Parameter(parent = "smscBindAttributes")
    private String systemId;
    @NotNull @Parameter(parent = "smscBindAttributes")
    private String password;
    @Parameter(parent = "smscBindAttributes")
    private String systemType;
    @NotNull @Parameter(defaultValue = "1", parent = "smscBindAttributes")
    private Byte dstTon;
    @NotNull @Parameter(defaultValue = "1", parent = "smscBindAttributes")
    private Byte dstNpi;
    @NotNull @Parameter(defaultValue = "5", parent = "smscBindAttributes")
    private Byte srcTon;
    @NotNull @Parameter(defaultValue = "0", parent = "smscBindAttributes")
    private Byte srcNpi;
    
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String smscConnectionAttributes;
    @NotNull @Parameter(defaultValue = "100", parent = "smscConnectionAttributes")
    private Integer soTimeout;
    @NotNull @Parameter(defaultValue = "100", parent = "smscConnectionAttributes")
    private Integer receiveTimeout;
    @NotNull @Parameter(defaultValue = "90000", parent = "smscConnectionAttributes")
    private Integer noRcvTimeout;
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String smscCodingAttributes;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte esmClass;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte protocolId;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte priorityFlag;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte registeredDelivery;
    @NotNull @Parameter(defaultValue = "01:00:00", parent = "smscCodingAttributes")
    private String messageExpireTime;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte replaceIfPresentFlag;
    @NotNull @Parameter(defaultValue = "8", parent = "smscCodingAttributes")
    private Byte dataCoding;
    @NotNull @Parameter(defaultValue = "0", parent = "smscCodingAttributes")
    private Byte smDefaultMsgId;
    @NotNull @Parameter(defaultValue = "cp1251", parent = "smscCodingAttributes")
    private String messageCP;
    @NotNull @Parameter(defaultValue = "false", parent = "smscCodingAttributes")
    private Boolean use7bit;
    @NotNull @Parameter(defaultValue = "3", parent = "smscCodingAttributes")
    private Integer longSmMode;
    
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String smscQueueAttributes;
    @NotNull @Parameter(defaultValue = "30000", parent = "smscQueueAttributes")
    private Integer throttledDelay;
    @NotNull @Parameter(defaultValue = "30000", parent = "smscQueueAttributes")
    private Integer mesThrottledDelay;
    @NotNull @Parameter(defaultValue = "30000", parent = "smscQueueAttributes")
    private Integer queueFullDelay;
    @NotNull @Parameter(defaultValue = "30000", parent = "smscQueueAttributes")
    private Integer mesQueueFullDelay;
    @NotNull @Parameter(defaultValue = "10", parent = "smscQueueAttributes")
    private Integer onceSend;
    @NotNull @Parameter(defaultValue = "10", parent = "smscQueueAttributes")
    private Integer maxUnconfirmed;
    @NotNull @Parameter(defaultValue = "100", parent = "smscQueueAttributes")
    private Integer maxMessagesInQueue;
    @NotNull @Parameter(defaultValue = "600000", parent = "smscQueueAttributes")
    private Integer mesLifeTime;
    @NotNull @Parameter(defaultValue = "5", parent = "smscQueueAttributes")
    private Integer maxSubmitAttempts;
    @NotNull @Parameter(defaultValue = "60000", parent = "smscQueueAttributes")
    private Long maxWaitForResp;   
    @NotNull @Parameter(defaultValue = "1000", parent = "smscQueueAttributes")
    private Long maxMessageUnitsPerTimeUnit;
    @NotNull @Parameter(defaultValue = "MINUTES", parent = "smscQueueAttributes")
    private TimeUnit maxMessageUnitsTimeUnit;
    @NotNull @Parameter(defaultValue = "1", parent = "smscQueueAttributes")
    private Long maxMessageUnitsTimeQuantity;  
    @NotNull @Parameter(defaultValue = "60000", parent = "smscQueueAttributes")
    private Long messageQueueWaitTimeout;
    
    @Parameter(valueHandlerType = ChildAttributesValueHandlerFactory.TYPE)
    private String smscInboundQueueAttributes;
    @NotNull @Parameter(defaultValue = "5", parent = "smscInboundQueueAttributes")
    private Long concatenatedMessageReceiveTimeout;
    @NotNull @Parameter(defaultValue = "MINUTES", parent = "smscInboundQueueAttributes")
    private TimeUnit concatenatedMessageReceiveTimeoutTimeUnit;
    @NotNull @Parameter(defaultValue = "64", parent = "smscInboundQueueAttributes")
    private Integer maxInboundQueueSize;
    
    
    
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
//    @NotNull @Parameter
//    private DataSource dataSource;

    private ReentrantReadWriteLock dataLock;
    private Condition messageProcessed;
    private AtomicReference<SmsTransceiverWorker> worker;
//    private 

    @Override
    protected void initFields() {
        super.initFields();
        dataLock = new ReentrantReadWriteLock();
        messageProcessed = dataLock.writeLock().newCondition();
        worker = new AtomicReference<SmsTransceiverWorker>();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
        worker.set(new SmsTransceiverWorker(this, new SmsConfigImpl(this), executor));
        worker.get().start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        SmsTransceiverWorker _worker = worker.getAndSet(null);
        if (_worker!=null)
            _worker.stop();
    }
    
    private void initNodes(boolean start) {
        SmsDeliveryReceiptChannel channel = getDeliveryReceiptChannel();
        if (channel==null) {
            channel = new SmsDeliveryReceiptChannel();
            addAndSaveChildren(channel);
        }
        SmsIncomingMessageChannel inChannel = getIncomingMessageChannel();
        if (inChannel==null) {
            inChannel = new SmsIncomingMessageChannel();
            addAndSaveChildren(inChannel);
        }
        if (start) {
            channel.start();
            inChannel.start();
        }
    }
    
    public SmsDeliveryReceiptChannel getDeliveryReceiptChannel() {
        return (SmsDeliveryReceiptChannel) getNode(SmsDeliveryReceiptChannel.NAME);
    }
    
    public SmsIncomingMessageChannel getIncomingMessageChannel() {
        return (SmsIncomingMessageChannel) getNode(SmsIncomingMessageChannel.NAME);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data==null)
            DataSourceHelper.sendDataToConsumers(this, data, context);
        else {
            try {
                Record rec = converter.convert(Record.class, data, null);
                SmsTransceiverWorker _worker = worker.get();
                if (_worker==null) {
                    DataSourceHelper.executeContextCallbacks(this, context, data);
                    return;
                }
                queueMessage(rec, context, _worker);
            } catch (Throwable e) {
                context.addError(this, new Exception("Error processing sms request", e));
                DataSourceHelper.executeContextCallbacks(this, context, data);
            }
        }
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }

//    public void setData(DataSource dataSource, Object data, DataContext context) {
//    }
//    
    public void messageHandled(boolean success, RecordHolder holder) {
        try {
            if (dataLock.writeLock().tryLock())
                try {
                    messageProcessed.signal();
                } finally {
                    dataLock.writeLock().unlock();
                }
            holder.record.setValue(SmsRecordSchemaNode.COMPLETION_CODE, SmsRecordSchemaNode.SUCCESSFUL_STATUS);
            holder.record.setValue(SmsRecordSchemaNode.SEND_TIME, new Date());
            DataSourceHelper.sendDataToConsumers(this, holder.record, holder.context);
        } catch (RecordException e) {
            if (isLogLevelEnabled(LogLevel.ERROR)) 
                getLogger().error("Error processing SMS record", e);
        }
    }
    
    private void queueMessage(Record rec, DataContext context, SmsTransceiverWorker _worker) 
        throws Exception 
    {
        final RecordHolder holder = new RecordHolder(rec, context);
        if (!_worker.addMessage(holder)) {
            long timeout = System.currentTimeMillis() + messageQueueWaitTimeout;
            boolean queued = false;
            do {
                if (dataLock.writeLock().tryLock(1000, TimeUnit.MILLISECONDS)) 
                    try {
                        messageProcessed.await(1000, TimeUnit.MILLISECONDS);
                    } finally {
                        dataLock.writeLock().unlock();
                    }
                queued = _worker.addMessage(holder);
            } while (!queued && System.currentTimeMillis() <= timeout);
            if (!queued) {
                rec.setValue(SmsRecordSchemaNode.COMPLETION_CODE, SmsRecordSchemaNode.QUEUE_FULL_STATUS);
                DataSourceHelper.sendDataToConsumers(this, rec, context);
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("Can't send SMS because of queue is full. {}", rec);
            }
        }
    }
    
//    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
//        return null;
//    }

//    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
//        return dataSource.getDataImmediate(dataConsumer, context);
//    }

//    public Collection<NodeAttribute> generateAttributes() {
//        return null;
//    }
//    
//    public Boolean getStopProcessingOnError() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

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

    public Integer getRebindOnTimeoutInterval() {
        return rebindOnTimeoutInterval;
    }

    public void setRebindOnTimeoutInterval(Integer rebindOnTimeoutInterval) {
        this.rebindOnTimeoutInterval = rebindOnTimeoutInterval;
    }

    public Long getEnquireInterval() {
        return enquireInterval;
    }

    public void setEnquireInterval(Long enquireInterval) {
        this.enquireInterval = enquireInterval;
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

    public String getMessageExpireTime() {
        return messageExpireTime;
    }

    public void setMessageExpireTime(String messageExpireTime) {
        this.messageExpireTime = messageExpireTime;
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

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
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

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Long getMessageQueueWaitTimeout() {
        return messageQueueWaitTimeout;
    }

    public void setMessageQueueWaitTimeout(Long messageQueueWaitTimeout) {
        this.messageQueueWaitTimeout = messageQueueWaitTimeout;
    }

    public String getSmscConnectionAttributes() {
        return smscConnectionAttributes;
    }

    public void setSmscConnectionAttributes(String smscConnectionAttributes) {
        this.smscConnectionAttributes = smscConnectionAttributes;
    }

    public String getSmscCodingAttributes() {
        return smscCodingAttributes;
    }

    public void setSmscCodingAttributes(String smscCodingAttributes) {
        this.smscCodingAttributes = smscCodingAttributes;
    }

    public String getSmscQueueAttributes() {
        return smscQueueAttributes;
    }

    public void setSmscQueueAttributes(String smscQueueAttributes) {
        this.smscQueueAttributes = smscQueueAttributes;
    }

//    public DataSource getDataSource() {
//        return dataSource;
//    }
//
//    public void setDataSource(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }

    public Long getMaxMessageUnitsPerTimeUnit() {
        return maxMessageUnitsPerTimeUnit;
    }

    public void setMaxMessageUnitsPerTimeUnit(Long maxMessageUnitsPerTimeUnit) {
        this.maxMessageUnitsPerTimeUnit = maxMessageUnitsPerTimeUnit;
    }

    public TimeUnit getMaxMessageUnitsTimeUnit() {
        return maxMessageUnitsTimeUnit;
    }

    public void setMaxMessageUnitsTimeUnit(TimeUnit maxMessageUnitsTimeUnit) {
        this.maxMessageUnitsTimeUnit = maxMessageUnitsTimeUnit;
    }

    public Long getMaxMessageUnitsTimeQuantity() {
        return maxMessageUnitsTimeQuantity;
    }

    public void setMaxMessageUnitsTimeQuantity(Long maxMessageUnitsTimeQuantity) {
        this.maxMessageUnitsTimeQuantity = maxMessageUnitsTimeQuantity;
    }

    public String getSmscBindAttributes() {
        return smscBindAttributes;
    }

    public void setSmscBindAttributes(String smscBindAttributes) {
        this.smscBindAttributes = smscBindAttributes;
    }

    public String getSmscInboundQueueAttributes() {
        return smscInboundQueueAttributes;
    }

    public void setSmscInboundQueueAttributes(String smscInboundQueueAttributes) {
        this.smscInboundQueueAttributes = smscInboundQueueAttributes;
    }

    public Long getConcatenatedMessageReceiveTimeout() {
        return concatenatedMessageReceiveTimeout;
    }

    public void setConcatenatedMessageReceiveTimeout(Long concatenatedMessageReceiveTimeout) {
        this.concatenatedMessageReceiveTimeout = concatenatedMessageReceiveTimeout;
    }

    public TimeUnit getConcatenatedMessageReceiveTimeoutTimeUnit() {
        return concatenatedMessageReceiveTimeoutTimeUnit;
    }

    public void setConcatenatedMessageReceiveTimeoutTimeUnit(TimeUnit concatenatedMessageReceiveTimeoutTimeUnit) {
        this.concatenatedMessageReceiveTimeoutTimeUnit = concatenatedMessageReceiveTimeoutTimeUnit;
    }

    public Integer getMaxInboundQueueSize() {
        return maxInboundQueueSize;
    }

    public void setMaxInboundQueueSize(Integer maxInboundQueueSize) {
        this.maxInboundQueueSize = maxInboundQueueSize;
    }

    @Parameter(readOnly = true)
    public Long getMessagesTotal() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getTotalMessages();
    }
    
    @Parameter(readOnly = true)
    public Long getMessagesSuccess() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getSuccessMessages();
    }
    
    @Parameter(readOnly = true)
    public Long getMessagesUnsuccess() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getUnsuccessMessages();
    }
    
    @Parameter(readOnly = true)
    public Integer getMessagesUnconfirmed() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.howManyUnconfirmed();
    }
    
    @Parameter(readOnly = true)
    public Integer getMessagesInQueue() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getMessagesInQueue();
    }
    
    @Parameter(readOnly = true)
    public Long getMessagesAvgSentTime() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getAvgSentTime();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsTotal() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getTotalUnits();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsSubmitted() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getSubmittedUnits();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsConfirmed() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getConfirmedUnits();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsFatal() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getFatalUnits();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsAvgConfirmTime() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getAvgConfirmTime();
    }
    
    @Parameter(readOnly = true)
    public Long getUnitsSentInCurrentPeriod() {
        OutQueue queue = getQueue();
        return queue==null? null : queue.getUnitsInPeriod();
    }
    
    @Parameter(readOnly = true)
    public Boolean getProcessorActive() {
        SmsTransceiverWorker _worker = worker.get();
        return _worker==null? null : _worker.isProcessorActive();
    }
    
    @Parameter(readOnly = true)
    public String getSmsAgentStatus() {
        SmsTransceiverWorker _worker = worker.get();
        return _worker==null? null : _worker.getSmsAgentStatus();
    }
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedPackets() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_PACKETS);
    }
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedMessages() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_MESSAGES);
    }
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedDataSMPackets() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_DATASM_PACKETS);
    }      
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedDeliverySMPackets() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_DELIVERYSM_PACKETS);
    }      
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedSarPackets() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_SAR_PACKETS);
    }      
    
    @Parameter(readOnly = true)
    public Long getInboundQueueReceivedUdhPackets() {
        return getStatFromInQueue(InQueue.GET_RECEIVED_UDH_PACKETS);
    }      
    
    @Parameter(readOnly = true)
    public Long getInboundQueueProcessingErrors() {
        return getStatFromInQueue(InQueue.GET_PROCESSING_ERRORS);
    }      
    
    private OutQueue getQueue() {
        SmsTransceiverWorker _worker = worker.get();
        return _worker==null? null : _worker.getQueue();
    }
    
    private Long getStatFromInQueue(Object statType) {
        SmsTransceiverWorker _worker = worker.get();
        if (_worker==null)
            return null;
        DataProcessorFacade inQueue = _worker.getInboundQueue();
        if (inQueue==null)
            return null;
        return (Long) inQueue.ask(statType).getOrElse(null, 500);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        return null;
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public static class RecordHolder {
        private final Record record;
        private final DataContext context;

        public RecordHolder(Record rec, DataContext context) {
            this.record = rec;
            this.context = context;
        }
        
        public Record getRecord() {
            return record;
        }

        public DataContext getContext() {
            return context;
        }
    }
}
