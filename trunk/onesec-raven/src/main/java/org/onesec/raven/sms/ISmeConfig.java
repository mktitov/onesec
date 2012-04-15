package org.onesec.raven.sms;

import com.logica.smpp.pdu.Address;

public interface ISmeConfig 
{
	public static final int RECEIVER 	= 1;
	public static final int TRANSMITTER = 2;
	public static final int TRANSCEIVER = 3;

	public abstract int getBindMode();

	public abstract String getBindAddr();

	public abstract int getBindPort();

	public abstract int getBindTon();

	public abstract int getBindNpi();

	public abstract String getAddrRange();

	public abstract String getFromAddr();
	
	public abstract boolean getAsync();
	
	public abstract int getBindTimeout();

	public abstract int getRebindInterval();

	public abstract int getEnquireTimeout();
	
	public abstract int getMaxEnquireAttempts();

	public abstract int getSoTimeout();
	
	public abstract int getReceiveTimeout();
	
	public abstract int getNoRcvTimeout();

	public abstract int getThrottledDelay();

	public abstract int getMesThrottledDelay();

	public abstract int getQueueFullDelay();

	public abstract int getMesQueueFullDelay();
	
	public abstract int getOnceSend();

	public abstract String getSystemType();

	public abstract String getServiceType();

	public abstract Address getSrcAddr();

	public abstract Address getSmscAddr();

	public abstract String getValidityPeriod();

	public abstract byte getEsmClass();

	public abstract byte getProtocolId();

	public abstract byte getPriorityFlag();

	public abstract byte getRegisteredDelivery();

	public abstract byte getReplaceIfPresentFlag();

	public abstract byte getDataCoding();

	public abstract byte getSmDefaultMsgId();

	public abstract byte getDstTon();

	public abstract byte getDstNpi();

	public abstract byte getSrcTon();

	public abstract byte getSrcNpi();

	public abstract String getMessageCP();

//	public abstract boolean isUseSarTags();

	public abstract boolean isUse7bit();

	public abstract int getLongSmMode();
	
	public abstract int getMaxUnconfirmed();

}