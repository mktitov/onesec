package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageUnit {

    private static Logger log = LoggerFactory.getLogger(MessageUnit.class);
    public static final int READY = 1;
    public static final int WAIT = 2;
    public static final int SENDED = 3;
    public static final int CONFIRMED = 4;
    public static final int FATAL = 5;
    
    private final SubmitSM pdu;
    private final int messageId;
    private final boolean segmented;
    private final long fd;
    private final int segCount;
    private final int segId;
    
    private int status;
    private int attempts;
    private long xtime;

    public MessageUnit(SubmitSM sm, int mesId, int segCnt, int segID) {
        pdu = sm;
        status = READY;
        messageId = mesId;
        xtime = 0;
        attempts = 0;
        segmented = segCnt > 1 ? true : false;
        segCount = segCnt;
        segId = segID;
        fd = System.currentTimeMillis();
    }

    public boolean isLastSeg() {
        if (segCount == (segId + 1)) {
            return true;
        }
        return false;
    }

    public synchronized void ready() {
        ready(System.currentTimeMillis());
    }

    public synchronized void ready(long time) {
        status = READY;
        xtime = time;
        log.info("mu:{} ready", messageId);
    }

    public synchronized void sended() {
        sended(System.currentTimeMillis());
    }

    public synchronized void sended(long time) {
        attempts++;
        status = SENDED;
        xtime = time;
        log.info("mu:{} sended", messageId);
    }

    public synchronized void fatal() {
        fatal(System.currentTimeMillis());
    }

    public synchronized void fatal(long time) {
        status = FATAL;
        xtime = time;
        log.info("mu:{} fatal", messageId);
    }

    public synchronized void confirmed() {
        confirmed(System.currentTimeMillis());
    }

    public synchronized void confirmed(long time) {
        status = CONFIRMED;
        xtime = time;
        log.info("mu:{} confirmed", messageId);
    }

    public synchronized void waiting(long interval) {
        waiting(System.currentTimeMillis(), interval);
    }

    public synchronized void waitingFor(long time) {
        waiting(0, time);
    }

    public synchronized void waiting(long interval, long time) {
        status = WAIT;
        xtime = time + interval;
        log.info("mu:{} waiting", messageId);
    }

    public SubmitSM getPdu() {
        return pdu;
    }
//	public void setPdu(SubmitSM pdu) {
//		this.pdu = pdu;
//	}

    public String getDst() {
        return pdu.getDestAddr().getAddress();
    }

    public synchronized int getStatus() {
        return status;
    }

//	public void setStatus(int status) {
//		this.status = status;
//	}
    public int getMessageId() {
        return messageId;
    }

    public boolean isSegmented() {
        return segmented;
    }

    public synchronized long getXtime() {
        return xtime;
    }

    public synchronized int getAttempts() {
        return attempts;
    }

    public long getFd() {
        return fd;
    }

    public int getSegCount() {
        return segCount;
    }

    public int getSegId() {
        return segId;
    }
//	public void setXtime(long xtime) {
//		this.xtime = xtime;
//	}
}
