package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import org.raven.tree.impl.LoggerHelper;
import static org.onesec.raven.sms.queue.MessageUnitStatus.*;

public class MessageUnitImpl implements MessageUnit {

//    public static final int READY = 1;
//    public static final int WAIT = 2;
//    public static final int SENDED = 3;
//    public static final int CONFIRMED = 4;
//    public static final int FATAL = 5;
    
    private final ShortTextMessageImpl message;
    private final LoggerHelper logger;
    private final SubmitSM pdu;
    private final long fd;
    private final boolean last;
    
    private MessageUnitStatus status;
    private int attempts;
    private long xtime;

    public MessageUnitImpl(SubmitSM sm, ShortTextMessageImpl message, boolean last, LoggerHelper logger) {
        this.logger = logger;
        this.pdu = sm;
        this.status = READY;
        this.message = message;
        this.last = last;
        
        this.xtime = 0;
        this.attempts = 0;
        this.fd = System.currentTimeMillis();
    }

    public boolean isLastSeg() {
        return last;
    }

    public synchronized void ready() {
        ready(System.currentTimeMillis());
    }

    private synchronized void ready(long time) {
        status = READY;
        xtime = time;
        logger.debug("ready");
    }

    public synchronized void submitted() {
        sended(System.currentTimeMillis());
    }

    private synchronized void sended(long time) {
        attempts++;
        status = SUBMITTED;
        xtime = time;
        logger.debug("sended");
    }

    public synchronized void fatal() {
        fatal(System.currentTimeMillis());
    }

    private synchronized void fatal(long time) {
        status = FATAL;
        xtime = time;
        logger.debug("fatal");
    }

    public synchronized void confirmed() {
        confirmed(System.currentTimeMillis());
    }

    private synchronized void confirmed(long time) {
        status = CONFIRMED;
        xtime = time;
        logger.debug("confirmed");
    }

    public void delay(long interval) {
        waiting(interval, System.currentTimeMillis());
    }
    
//    public synchronized void waiting(long interval) {
//        waiting(System.currentTimeMillis(), interval);
//    }
//
//    public synchronized void waitingFor(long time) {
//        waiting(0, time);
//    }

    private synchronized void waiting(long interval, long time) {
        status = DELAYED;
        xtime = time + interval;
        logger.info("waiting");
    }

    public SubmitSM getPdu() {
        return pdu;
    }

    public String getDst() {
        return pdu.getDestAddr().getAddress();
    }

    public int getSequenceNumber() {
        return pdu.getSequenceNumber();
    }

    public synchronized MessageUnitStatus getStatus() {
        return status;
    }

    public long getMessageId() {
        return message.getId();
    }

    public synchronized long getXTime() {
        return xtime;
    }

    public synchronized int getAttempts() {
        return attempts;
    }

    public long getFd() {
        return fd;
    }

//    public int getSegId() {
//        return segId;
//    }
//	public void setXtime(long xtime) {
//		this.xtime = xtime;
//	}
}
