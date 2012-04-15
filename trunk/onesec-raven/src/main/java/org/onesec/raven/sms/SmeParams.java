package org.onesec.raven.sms;

import com.logica.smpp.pdu.Address;

public class SmeParams extends Thread implements ISmeConfig 
{
	protected int bindMode = 3;
	
	protected String bindAddr = "127.0.0.1";
	protected int bindPort = 5016;
	
	protected int bindTon = 5;
	protected int bindNpi = 0;
	protected String addrRange = "Raven"; //Raven
	protected String fromAddr = "0001";
	
	protected boolean async = true;

	protected int bindTimeout = 30*1000;
	protected int rebindInterval = 60*1000;
	
	protected int enquireTimeout = 90*1000;
	protected int maxEnquireAttempts = 3;

	protected int soTimeout = 100;
	protected int receiveTimeout = 100;
	
	protected int noRcvTimeout = 90*1000;
	
	protected int throttledDelay = 30*1000;
	protected int mesThrottledDelay = 30*1000;
	
	protected int queueFullDelay = 30*1000;
	protected int mesQueueFullDelay = 30*1000;
	
	protected int onceSend = 10;

	protected String systemId = "sysid";
	protected String password = "passw";
	protected String systemType = "";
	protected String serviceType = "";
	protected Address srcAddr = null;
	//protected Address dstAddr = null;
	protected Address smscAddr = null; 
	//protected String scheduleDeliveryTime = "";
	protected String validityPeriod = "";
	protected byte esmClass               = 0;
	protected byte protocolId             = 0;
	protected byte priorityFlag           = 0;
	protected byte registeredDelivery     = 0;
	protected byte replaceIfPresentFlag	= 0;
	protected byte dataCoding           = 8;
	protected byte smDefaultMsgId         = 0;
	protected byte dstTon = 1;
	protected byte dstNpi = 1;
	protected byte srcTon = 5;
	protected byte srcNpi = 0; //1
 	protected String messageCP = "cp1251";
//	protected boolean useSarTags = false;
	protected boolean use7bit = false;
	protected int longSmMode = 3;
	
	protected int maxUnconfirmed = 10;
	
	public int getBindMode() {
		return bindMode;
	}
	public void setBindMode(int bindMode) {
		if(bindMode>3 || bindMode<1) 
			bindMode = 3;
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
	public boolean getAsync() {
		return async;
	}
	public void setAsync(boolean async) {
		this.async = async;
	}
	public int getBindTimeout() {
		return bindTimeout;
	}
	public void setBindTimeout(int bindWait) {
		this.bindTimeout = bindWait;
	}
	public int getEnquireTimeout() {
		return enquireTimeout;
	}
	public void setEnquireTimeout(int enqWait) {
		this.enquireTimeout = enqWait;
	}
	public int getNoRcvTimeout() {
		return noRcvTimeout;
	}
	public void setNoRcvTimeout(int norcvWait) {
		this.noRcvTimeout = norcvWait;
	}
	public int getThrottledDelay() {
		return throttledDelay;
	}
	public void setThrottledDelay(int delay) {
		throttledDelay = delay;
	}
	public int getOnceSend() {
		return onceSend;
	}
	public void setOnceSend(int oncesend) {
		onceSend = oncesend;
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
	public String getServiceType() {
		return serviceType;
	}
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}
	public Address getSrcAddr() {
		return srcAddr;
	}
	public void setSrcAddr(Address srcAddr) {
		this.srcAddr = srcAddr;
	}
	public Address getSmscAddr() {
		return smscAddr;
	}
	public void setSmscAddr(Address smscAddr) {
		this.smscAddr = smscAddr;
	}
	public String getValidityPeriod() {
		return validityPeriod;
	}
	public void setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
	}
	public byte getEsmClass() {
		return esmClass;
	}
	public void setEsmClass(byte esmClass) {
		this.esmClass = esmClass;
	}
	public byte getProtocolId() {
		return protocolId;
	}
	public void setProtocolId(byte protocolId) {
		this.protocolId = protocolId;
	}
	public byte getPriorityFlag() {
		return priorityFlag;
	}
	public void setPriorityFlag(byte priorityFlag) {
		this.priorityFlag = priorityFlag;
	}
	public byte getRegisteredDelivery() {
		return registeredDelivery;
	}
	public void setRegisteredDelivery(byte registeredDelivery) {
		this.registeredDelivery = registeredDelivery;
	}
	public byte getReplaceIfPresentFlag() {
		return replaceIfPresentFlag;
	}
	public void setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
		this.replaceIfPresentFlag = replaceIfPresentFlag;
	}
	public byte getDataCoding() {
		return dataCoding;
	}
	public void setDataCoding(byte dataCoding) {
		this.dataCoding = dataCoding;
	}
	public byte getSmDefaultMsgId() {
		return smDefaultMsgId;
	}
	public void setSmDefaultMsgId(byte smDefaultMsgId) {
		this.smDefaultMsgId = smDefaultMsgId;
	}
	public byte getDstTon() {
		return dstTon;
	}
	public void setDstTon(byte dstTon) {
		this.dstTon = dstTon;
	}
	public byte getDstNpi() {
		return dstNpi;
	}
	public void setDstNpi(byte dstNpi) {
		this.dstNpi = dstNpi;
	}
	public byte getSrcTon() {
		return srcTon;
	}
	public void setSrcTon(byte srcTon) {
		this.srcTon = srcTon;
	}
	public byte getSrcNpi() {
		return srcNpi;
	}
	public void setSrcNpi(byte srcNpi) {
		this.srcNpi = srcNpi;
	}
	public String getMessageCP() {
		return messageCP;
	}
	public void setMessageCP(String messageCP) {
		this.messageCP = messageCP;
	}

	public boolean isUse7bit() {
		return use7bit;
	}
	public void setUse7bit(boolean use7bit) {
		this.use7bit = use7bit;
	}
	public int getLongSmMode() {
		return longSmMode;
	}
	public void setLongSmMode(int longSmMode) {
		this.longSmMode = longSmMode;
	}
	
	public int getMaxUnconfirmed() {
		return maxUnconfirmed;
	}
	public void setMaxUnconfirmed(int maxUnconfirmed) {
		this.maxUnconfirmed = maxUnconfirmed;
	}
	
	public int getMesQueueFullDelay() {
		return mesQueueFullDelay;
	}
	public void setMesQueueFullDelay(int delay) {
		mesQueueFullDelay = delay;
	}
	
	public int getMesThrottledDelay() {
		return mesThrottledDelay;
	}
	public void setMesThrottledDelay(int delay) {
		mesThrottledDelay = delay;
	}

	public int getQueueFullDelay() {
		return queueFullDelay;
	}
	
	public void setQueueFullDelay(int delay) {
		queueFullDelay = delay;
	}
	public int getMaxEnquireAttempts() {
		return maxEnquireAttempts;
	}
	public void setMaxEnquireAttempts(int maxEnquireAttempts) {
		this.maxEnquireAttempts = maxEnquireAttempts;
	}
	public int getRebindInterval() {
		return rebindInterval;
	}
	public void setRebindInterval(int rebindInterval) {
		this.rebindInterval = rebindInterval;
	}
	public int getReceiveTimeout() {
		return receiveTimeout;
	}
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
	public int getSoTimeout() {
		return soTimeout;
	}
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}
	
}
