package org.onesec.raven.sms.queue;

import org.onesec.raven.sms.MessageUnit;
import org.onesec.raven.sms.MessageUnitStatus;
import com.logica.smpp.pdu.SubmitSM;
import java.util.ArrayList;
import java.util.List;
import org.onesec.raven.sms.MessageUnitListener;
import org.raven.tree.impl.LoggerHelper;
import static org.onesec.raven.sms.MessageUnitStatus.*;
import org.onesec.raven.sms.ShortTextMessage;
import org.onesec.raven.sms.SmsConfig;

public class MessageUnitImpl implements MessageUnit {

    private final LoggerHelper logger;
    private final SubmitSM pdu;
    private final long fd;
    private final List<MessageUnitListener> listeners = new ArrayList<MessageUnitListener>(2);
    private final SmsConfig config;
    private final ShortTextMessage message;
    
    private MessageUnitStatus status;
    private int attempts;
    private volatile long xtime;
    private volatile long submitTime = 0;
    private volatile long confirmTime = 0;

    public MessageUnitImpl(ShortTextMessage message, SubmitSM sm, SmsConfig config, LoggerHelper logger) {
        this.message = message;
        this.logger = logger;
        this.pdu = sm;
        this.status = READY;
        this.config = config;
        
        this.xtime = 0;
        this.attempts = 0;
        this.fd = System.currentTimeMillis();
    }

    public MessageUnit addListener(MessageUnitListener listener) {
        listeners.add(listener);
        return this;
    }

    public ShortTextMessage getMessage() {
        return message;
    }

    private boolean changeStatusTo(MessageUnitStatus newStatus, long interval) {
        MessageUnitStatus oldStatus;
        synchronized(this) {
            //if already in final status
            if (status==CONFIRMED || status==FATAL)
                return false;
            //if same status
            if (status==newStatus)
                return false;
            oldStatus = status;
            if (newStatus==SUBMITTED && status==READY) {
                status = newStatus;
                ++attempts;
            } else if ((newStatus==DELAYED || newStatus==TRY_WHEN_READY) && status==SUBMITTED)
                status = newStatus;
            else if (newStatus==CONFIRMED || newStatus==FATAL)
                status = newStatus;
            else if (newStatus==READY) 
                status = newStatus;
        }
        if (oldStatus!=status) {
            xtime = System.currentTimeMillis()+interval;
            if (logger.isDebugEnabled())
                logger.debug(String.format("Status changed from %s to %s", oldStatus, status));
            for (MessageUnitListener listener: listeners)
                listener.statusChanged(this, oldStatus, status);
            //send message to listeners
        }
        return oldStatus!=status;
    }

    public MessageUnitStatus checkStatus() {
        long curTime = System.currentTimeMillis();
        if (status==DELAYED && curTime>xtime)
            changeStatusTo(READY, 0);
        if (status==TRY_WHEN_READY)
            changeStatusTo(READY, 0);
        if (status==SUBMITTED && curTime - xtime > config.getMaxWaitForResp()) 
            changeStatusTo(attempts <= config.getMaxSubmitAttempts()? READY : FATAL, 0);
        if (status==READY && attempts > config.getMaxSubmitAttempts())
            changeStatusTo(FATAL, 0);
        return status;
    }
    
//    public synchronized void ready() {
//        ready(System.currentTimeMillis());
//    }
//
//    private synchronized void ready(long time) {
//        status = READY;
//        xtime = time;
//        logger.debug("ready");
//    }

    public  void submitted() {
        submitTime = System.currentTimeMillis();
        changeStatusTo(SUBMITTED, 0);
//        sended(System.currentTimeMillis());
    }

//    private synchronized void sended(long time) {
//        attempts++;
//        status = SUBMITTED;
//        xtime = time;
//        logger.debug("sended");
//    }

    public  void fatal() {
        changeStatusTo(FATAL, 0);
//        fatal(System.currentTimeMillis());
    }

//    private synchronized void fatal(long time) {
//        status = FATAL;
//        xtime = time;
//        logger.debug("fatal");
//    }

    public  void confirmed() {
        confirmTime = System.currentTimeMillis();
        changeStatusTo(CONFIRMED, 0);
//        confirmed(System.currentTimeMillis());
    }

    @Override
    public void tryWhenReady() {
        changeStatusTo(TRY_WHEN_READY, 0);
    }
    
    public long getConfirmTime() {
        return confirmTime-submitTime;
    }

//    private synchronized void confirmed(long time) {
//        status = CONFIRMED;
//        xtime = time;
//        logger.debug("confirmed");
//    }

    public void delay(long interval) {
        changeStatusTo(DELAYED, interval);
//        waiting(interval, System.currentTimeMillis());
    }
    
//    private synchronized void waiting(long interval, long time) {
//        status = DELAYED;
//        xtime = time + interval;
//        logger.info("waiting");
//    }

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

//    public long getMessageId() {
//        return message.getId();
//    }

    public synchronized long getXTime() {
        return xtime;
    }

    public synchronized int getAttempts() {
        return attempts;
    }

    public long getFd() {
        return fd;
    }
}
