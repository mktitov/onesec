/*
 * Created on 15.01.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.onesec.raven.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onesec.raven.sms.queue.MessageUnit;
import org.onesec.raven.sms.queue.OutQueue;
import org.onesec.raven.sms.queue.ShortTextMessage;
import org.onesec.raven.sms.sm.SMTextFactory;
import com.logica.smpp.Data;
import com.logica.smpp.SmppException;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.PDUException;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import com.logica.smpp.util.TerminatingZeroNotFoundException;
import org.raven.tree.impl.LoggerHelper;

/**
 * @author psn
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SmeTransceiver extends Thread implements ISmeConfig {

    private static final Logger log = LoggerFactory.getLogger(SmeTransceiver.class);
    private SmeA sme = null;
    private boolean stopFlag = false;
    //private SmCoder smCoder =  null; 
    private OutQueue queue;
    private SMTextFactory smFactory;
    private long pleaseWait = 0;
    protected int bindMode = 3;
    protected String bindAddr = "";
    protected int bindPort = 5016;
    protected int bindTon = 5;
    protected int bindNpi = 0;
    protected String addrRange = ""; //Raven
    protected String fromAddr = "";
    protected boolean async = true;
    protected int bindTimeout = 30 * 1000;
    protected int rebindInterval = 60 * 1000;
    protected int enquireTimeout = 90 * 1000;
    protected int maxEnquireAttempts = 3;
    protected int soTimeout = 100;
    protected int receiveTimeout = 100;
    protected int noRcvTimeout = 90 * 1000;
    protected int throttledDelay = 30 * 1000;
    protected int mesThrottledDelay = 30 * 1000;
    protected int queueFullDelay = 30 * 1000;
    protected int mesQueueFullDelay = 30 * 1000;
    protected int onceSend = 10;
    protected String systemId = "";
    protected String password = "";
    protected String systemType = "";
    protected String serviceType = "";
    protected Address srcAddr = null;
    //protected Address dstAddr = null;
    protected Address smscAddr = null;
    //protected String scheduleDeliveryTime = "";
    protected String validityPeriod = "";
    protected byte esmClass = 0;
    protected byte protocolId = 0;
    protected byte priorityFlag = 0;
    protected byte registeredDelivery = 0;
    protected byte replaceIfPresentFlag = 0;
    protected byte dataCoding = 8;
    protected byte smDefaultMsgId = 0;
    protected byte dstTon = 1;
    protected byte dstNpi = 1;
    protected byte srcTon = 5;
    protected byte srcNpi = 0; //1
    protected String messageCP = "cp1251";
    protected boolean use7bit = false;
    protected int longSmMode = 3;
    protected int maxUnconfirmed = 10;

    /**
     * На сколько отложить отправку сообщений при ошибке 14 - QUEUE_FULL (мс). private long waitQF =
     * 0; На сколько отложить отправку сообщений при ошибке 58 - THROTTLED (мс). private long waitTH
     * = 0;
     */
    public SmeTransceiver() throws SmppException {
        init();
    }

    public void init() throws SmppException //throws Exception
    {
        setSrcAddr(new Address((byte) srcTon, (byte) srcNpi, fromAddr));
        smFactory = new SMTextFactory(this);
//        queue = new OutQueue(smFactory, new LoggerHelper(null, fromAddr));
        SmeInit();
    }

    public void initz2() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                stopWork();
            }
        });
    }

    public SmeA SmeInit() throws SmppException {
        sme = new SmeA(this);
        sme.setReceiveTimeout(receiveTimeout);
        sme.setSoTimeout(soTimeout);
        switch (bindMode) {
            case 1:
                sme.setBindMode("r");
                setName("ReceiverSME");
                break;
            case 2:
                sme.setBindMode("t");
                setName("TransmitterSME");
                break;
            case 3:
                sme.setBindMode("tr");
                setName("TransceiverSME");
                break;
        }

        sme.setLastPduRcvTime(System.currentTimeMillis());
        sme.setSystemId(systemId);
        sme.setPassword(password);
        sme.setSystemType(systemType);
        sme.setSmscIpAddr(bindAddr);
        sme.setSmscIpPort(bindPort);
        return sme;
    }

//	public ISmeConfig getSmeParams()
//	{
//		return sme.getSmePar();
//	}
    public void stopWork() {
        log.warn("stop signal!");
        setStopFlag(true);
        try {
            Thread.sleep(600);
        } catch (Exception e) {
        }
    }

    private void pduIsResponse(PDU pdu) {
        int sq = pdu.getSequenceNumber();
        long time = System.currentTimeMillis();
        switch (pdu.getCommandStatus()) {
            case Data.ESME_ROK:
                queue.confirmed(sq, time);
                break;
            case Data.ESME_RTHROTTLED:
                if (throttledDelay > 0) {
                    pleaseWait = time + throttledDelay;
                }
                queue.throttled(sq, time);
                break;
            case Data.ESME_RMSGQFUL:
                if (queueFullDelay > 0) {
                    pleaseWait = time + queueFullDelay;
                }
                queue.queueIsFull(sq, time);
                break;
            default:
                queue.failed(sq, time);
        }
    }

    private void pduIsRequest(PDU pdu) {
        log.warn("New incoming request:{}", pdu.debugString());
    }

    /**
     *
     * @param time - current time
     * @return
     */
    public boolean checkBind(long time) {
        if (!sme.isBound()) {
            log.debug("not bound");
            if (time - sme.getLastBindAttemptTime() > bindTimeout) {
                //if(sme.getLastBindAttemptTime()>0) sme.unbind();
                sme.bind();
            }
            log.debug("wait bind ask");
            return false;
        }
        return true;
    }

    public void receiveAll() {
        PDU pdu;
        do { // принимаем всё из очереди
            log.debug("begin receive");
            pdu = sme.receive();
            log.debug("end receive");
            if (pdu == null) {
                break;
            }
            if (pdu.isResponse()) {
                pduIsResponse(pdu);
            } else if (pdu.isRequest()) {
                pduIsRequest(pdu);
            }
        } while (isStopFlag() == false);
    }

    public void run() {
        int sendfl = 0;
        long sleepint = 10; //10
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                stopWork();
            }
        });
        log.warn("started");

        sme.bind();
        int once = async ? onceSend : 1;
        for (int i = 0;; i++) {
            if (sendfl == 1) {
                sendfl = 0;
            } else {
                try {
                    Thread.sleep(sleepint);
                } catch (Exception e) {
                    break;
                }
            }

            if (isStopFlag() == true) {
                break;
            }

            long tm = System.currentTimeMillis();

            if (!checkBind(tm)) {
                continue;
            }

            receiveAll();

            if (isStopFlag() == true) {
                break;
            }

            if (sme.isNeedUnbind() == true) {
                sme.unbind();
                sme.setNeedUnbind(false);
                log.warn("Unbind by request");
                continue;
            }

            if (sme.getCntEnqLinkAttempt() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("count EnqLink:{}", sme.getCntEnqLinkAttempt());
                }
                if (sme.getCntEnqLinkAttempt() > 2) {
                    sme.unbind();
                    continue;
                }
                if (tm - sme.getLastEnqLinkAttemptTime() > enquireTimeout) {
                    log.info("EL: tm={} lastELAT={}", tm, sme.getLastEnqLinkAttemptTime());
                    sme.enquireLink();
                }
                continue;
            }

            if (tm - sme.getLastPduRcvTime() > noRcvTimeout) {
                if (log.isDebugEnabled()) {
                    log.debug("EL+: tm={} lastPDUTIME={}", tm, sme.getLastPduRcvTime());
                }
                sme.enquireLink();
                continue;
            }
            if (tm < pleaseWait) {
                continue;
            }
            pleaseWait = 0;
            if (bindMode == 1 || queue.howManyUnconfirmed() >= maxUnconfirmed) {
                continue;
            }

            // log.info("begin getNextPdu");
            SubmitSM sm = null;
            try {
                int snd = 0;
                for (int k = 0; queue.howManyUnconfirmed() <= maxUnconfirmed; k++) {
                    if (isStopFlag()) {
                        break;
                    }
                    MessageUnit mu = queue.getNext();
                    if (mu == null) {
                        break;
                    }
                    log.info("new PDU for submit found:");
                    sm = mu.getPdu();
                    sm.assignSequenceNumber(true);
                    if (log.isInfoEnabled()) {
                        log.info("SubmitSM: {}", sm.debugString());
                    }
                    sme.submit254(sm);
                    queue.sended(mu);
                    sendfl = 1;
                    if (++snd > once) {
                        log.info("now go to rcv");
                        break;
                    }
                }
            } catch (NotEnoughDataInByteBufferException ne) {
                log.error("pdu is bad: " + sm.debugString(), ne);
                continue;
            } catch (PDUException ne) {
                log.error("pdu is bad: " + sm.debugString(), ne);
                continue;
            } catch (TerminatingZeroNotFoundException ne) {
                log.error("pdu is bad: " + sm.debugString(), ne);
                continue;
            } catch (SmppException e) {
                log.error("submit error ...", e);
                break;
            } //} // end for
            // log.info("send loop end");
            catch (Exception e) {
                log.error("On getNextPdu:", e);
            }
        } //end big for
        sme.unbind();
        log.warn("stoped");
    } //end run 

    public synchronized boolean isStopFlag() {
        return stopFlag;
    }

    public synchronized void setStopFlag(boolean stopFlag) {
        this.stopFlag = stopFlag;
    }

    public OutQueue getQueue() {
        return queue;
    }

    public int getBindMode() {
        return bindMode;
    }

    public void setBindMode(int bindMode) {
        if (bindMode > 3 || bindMode < 1) {
            bindMode = 3;
        }
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

    public static void main(String[] args) {
        new BindTransciever();
        SmeTransceiver sme;
        try {
            sme = new SmeTransceiver();
        } catch (SmppException e1) {
            log.error("...", e1);
            return;
        }

        OutQueue q = sme.getQueue();

        sme.start();

//        q.addMessage(new ShortTextMessage("79128672947", "Test message"));
//        q.addMessage(new ShortTextMessage("79128672947", "Тестовое сообщение длинное длинное длинное0 длинное длинное длинное1 длинное длинное длинное длинное2"));

        //sme.
        while (true) {
            try {
                sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                sme.stopWork();
                return;
            }
        }
    }
}
