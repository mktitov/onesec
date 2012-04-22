package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import org.raven.tree.impl.LoggerHelper;

public class MessageUnit {

    public static final int READY = 1;
    public static final int WAIT = 2;
    public static final int SENDED = 3;
    public static final int CONFIRMED = 4;
    public static final int FATAL = 5;
    
    private final ShortTextMessage message;
    private final LoggerHelper logger;
    private final SubmitSM pdu;
    private final long fd;
    private final boolean last;
    
    private int status;
    private int attempts;
    private long xtime;

    public MessageUnit(SubmitSM sm, ShortTextMessage message, boolean last, LoggerHelper logger) {
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

    public synchronized void ready(long time) {
        status = READY;
        xtime = time;
        logger.debug("ready");
    }

    public synchronized void sended() {
        sended(System.currentTimeMillis());
    }

    public synchronized void sended(long time) {
        attempts++;
        status = SENDED;
        xtime = time;
        logger.debug("sended");
    }

    public synchronized void fatal() {
        fatal(System.currentTimeMillis());
    }

    public synchronized void fatal(long time) {
        status = FATAL;
        xtime = time;
        logger.debug("fatal");
    }

    public synchronized void confirmed() {
        confirmed(System.currentTimeMillis());
    }

    public synchronized void confirmed(long time) {
        status = CONFIRMED;
        xtime = time;
        logger.debug("confirmed");
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
        logger.info("waiting");
    }

    public SubmitSM getPdu() {
        return pdu;
    }

    public String getDst() {
        return pdu.getDestAddr().getAddress();
    }

    public synchronized int getStatus() {
        return status;
    }

    public long getMessageId() {
        return message.getId();
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

//    public int getSegId() {
//        return segId;
//    }
//	public void setXtime(long xtime) {
//		this.xtime = xtime;
//	}
}
