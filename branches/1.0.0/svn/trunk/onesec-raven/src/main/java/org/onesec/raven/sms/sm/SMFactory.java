package org.onesec.raven.sms.sm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onesec.raven.sms.ISmeConfig;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;

public abstract class SMFactory {

    private static Logger log = LoggerFactory.getLogger(SMFactory.class);
    protected static final int maxAsciiLen = 254;
    protected static final int maxUsc2Len = 126;
    protected static final int maxSegAsciiLen = 100;
    protected static final int maxSegUsc2Len = 69;
    protected String systemType = "";
    protected String serviceType = "";
    protected Address srcAddr = null;
    protected Address dstAddr = null;
    protected Address smscAddr = null;
    protected String scheduleDeliveryTime = "";
    protected String validityPeriod = "";
    protected byte esmClass = 0;
    protected byte protocolId = 0;
    protected byte priorityFlag = 0;
    protected byte registeredDelivery = 0;
    protected byte replaceIfPresentFlag = 0;
    protected byte dataCoding = 0;
    protected byte smDefaultMsgId = 0;
    protected String messageCP = "cp1251";
    //protected boolean useSarTags = false;
    protected boolean use7bit = false;
    protected ISmeConfig config;

    //SMFactory() { }
    SMFactory(ISmeConfig sp) {
        setParams(sp);
        config = sp;
    }

    protected SubmitSM setRequestParams(SubmitSM request) {
        request.setDestAddr(dstAddr);
        request.setSourceAddr(srcAddr);
        request.setReplaceIfPresentFlag(replaceIfPresentFlag);
        request.setEsmClass(esmClass);
        request.setProtocolId(protocolId);
        request.setPriorityFlag(priorityFlag);
        request.setRegisteredDelivery(registeredDelivery);
        request.setSmDefaultMsgId(smDefaultMsgId);
        //request.assignSequenceNumber(true);
        request.setDataCoding(dataCoding);
        try {
            request.setServiceType(serviceType);
            request.setScheduleDeliveryTime(scheduleDeliveryTime);
            request.setValidityPeriod(validityPeriod);
        } catch (Exception e) {
            log.error("Set request params: ", e);
        }
        return request;
    }

    protected void setParams(ISmeConfig sp) {
        srcAddr = sp.getSrcAddr();
        replaceIfPresentFlag = sp.getReplaceIfPresentFlag();
        esmClass = sp.getEsmClass();
        protocolId = sp.getProtocolId();
        priorityFlag = sp.getPriorityFlag();
        registeredDelivery = sp.getRegisteredDelivery();
        dataCoding = sp.getDataCoding();
        //smDefaultMsgId 
        systemType = sp.getSystemType();
        validityPeriod = sp.getValidityPeriod();
        //useSarTags = sp.isUseSarTags();
        use7bit = sp.isUse7bit();
    }
//	public abstract int getPDU(SmeParams sp,OutMes om,ArrayList al);
}
