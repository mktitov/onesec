package org.onesec.raven.sms.queue;

import org.onesec.raven.sms.MessageUnit;
import org.onesec.raven.sms.MessageUnitListener;
import org.onesec.raven.sms.MessageUnitStatus;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.SmsConfig;
import org.raven.tree.impl.LoggerHelper;
import static org.onesec.raven.sms.MessageUnitStatus.*;
import org.onesec.raven.sms.ShortMessageListener;
import org.onesec.raven.sms.ShortTextMessage;
import org.weda.beans.ObjectUtils;

public class OutQueue implements MessageUnitListener, ShortMessageListener {

//    private int maxMesCount = 100;
    /**
     * Максимальное время нахождения сообщения в очереди (мс).
     */
//    private long mesLifeTime = 10 * 60 * 1000;
    /**
     * @see OutQueue.factorQF
     */
    private final long mesWaitQF = 60 * 1000;
    /**
     * @see OutQueue.factorTH
     */
    private final long mesWaitTH = 60 * 1000;
    /**
     * Максимальное время ожидания потверждения на отправленное сообщение (мс).
     */
//    private long maxWaitForResp = 60 * 1000;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 14 - QUEUE_FULL (мс). If QUEUE_FULL
     * : waitInterval = mesWaitQF * ( 1+factorQF*attempsCount )
     */
    private final int factorQF = 0;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 58 - THROTTLED (мс). If THROTTLED :
     * waitInterval = mesWaitTH * ( 1+factorTH*attempsCount )
     */
    private final int factorTH = 0;
//    private int maxAttempts = 5;
    
//    private final Map<Integer, MessageUnit> sended = new ConcurrentHashMap<Integer, MessageUnit>();
    private final Map<Integer, MessageUnit> submitted = new ConcurrentHashMap<Integer, MessageUnit>();
    private final ConcurrentHashMap<String, Long> blockedNums = new ConcurrentHashMap<String, Long>();
//    private final LinkedBlockingQueue<MessageUnit> queue = new LinkedBlockingQueue<MessageUnit>();
    private final Queue<MessageUnit> queue = new ConcurrentLinkedQueue<MessageUnit>();
    private final AtomicInteger mesCount = new AtomicInteger(0);
//    private final AtomicInteger submittedCount = new AtomicInteger(0);
    private final LoggerHelper logger;
    private final SmsConfig config;

    public OutQueue(SmsConfig config, LoggerHelper logger) {
        this.config = config;
        this.logger = new LoggerHelper(logger, "Queue. ");
    }

    //public static MessageUnit[] noack
    public int howManyUnconfirmed() {
        return submitted.size();
    }

    public boolean addMessage(ShortTextMessage sm) {
        if (mesCount.incrementAndGet() > config.getMaxMessagesInQueue()) {
            mesCount.decrementAndGet();
            if (logger.isWarnEnabled())
                logger.warn("Can't queue message. Queue is FULL");
            return false;
        } else {
            sm.addListener(this);
            MessageUnit[] units = sm.getUnits();
            for (int i = 0; i < units.length; i++) 
                queue.offer(units[i]);
            if (logger.isDebugEnabled())
                logger.debug("Message ({}) queued", sm);
            return true;
        }
//        synchronized (mesCount) {
//            if (mesCount.get() + 1 > config.getMaxMessagesInQueue()) 
//                return false;
//            for (int i = 0; i < units.length; i++) 
//                queue.offer(units[i]);
//            mesCount.incrementAndGet();
//        }
//        if (logger.isDebugEnabled())
//            logger.debug("Message ({}) queued", sm);
//        return true;
    }

//    public void submitted(MessageUnit u) {
//        submitted(u, System.currentTimeMillis());
//    }
//
//    public void submitted(MessageUnit u, long time) {
//        sended.put(u.getSequenceNumber(), u);
//        u.submitted();
//        
//    }
    
    public boolean isEmpty() {
        return queue.isEmpty() && submitted.isEmpty();
    }
    
    /**
     * Returns submitted message by sequence number or null
     * @param sequenceNumber sequence number
     */
    public MessageUnit getMessageUnit(int sequenceNumber) {
        return submitted.get(sequenceNumber);
    }

//    public void throttled(int sequence, long time) {
//        MessageUnit u = sended.remove(sequence);
//        if (u != null) 
//            blockDirection(u, mesWaitTH * (1 + factorTH * u.getAttempts()), time);
//    }
//
//    public void queueIsFull(int sequence, long time) {
//        MessageUnit u = sended.remove(sequence);
//        if (u != null) 
//            blockDirection(u, mesWaitQF * (1 + factorQF * u.getAttempts()), time);
//    }
//    
//    private void blockDirection(MessageUnit unit, long delay, long curTime) {
//        unit.delay(delay);
//        blockedNums.putIfAbsent(unit.getDst(), curTime);
//    }
//
//    public void confirmed(int sequence, long time) {
//        MessageUnit u = sended.remove(sequence);
//        if (u != null) 
//            u.confirmed();
//    }
//
//    public void failed(int sequence, long time) {
//        MessageUnit u = sended.remove(sequence);
//        if (u != null) 
//            u.fatal();
//    }

//    public void messageSubmitted(MessageUnit u, boolean ok) {
//        mesCount.decrementAndGet();
//    }

    public MessageUnit getNext() {
        if (submitted.size()<=config.getMaxUnconfirmed()) 
            for (Iterator<MessageUnit> it=queue.iterator(); it.hasNext();) {
                MessageUnit unit = it.next();
                if (isDirectionAvailable(unit.getDst()) && unit.checkStatus()==READY)
                    return unit;
                else if (ObjectUtils.in(unit.getStatus(), CONFIRMED, FATAL)) 
                    it.remove();
            }
        return null;
    }
//    public MessageUnit getNext() {
//        Iterator<MessageUnit> it = queue.iterator();
//        long t = System.currentTimeMillis();
//        MessageUnit prev = null;
//        boolean noPrev;
//        while (it.hasNext()) {
//            noPrev = false;
//            MessageUnit m = it.next();
//            synchronized (m) {
//                checkMessageUnitForFatal(t, m, prev);
//                noPrev = handleMessageUnitStatus(m, t, prev, it, noPrev);
//            }
//            if (m.getStatus() == READY) {
//                if (m.getAttempts() >= config.getMaxSubmitAttempts()) 
//                    m.fatal();
//                else if (!blockedNums.contains(m.getDst())) 
//                    return m;
//            }
//            if (!noPrev) 
//                prev = m;
//        }
//        return null;
//    }

//    private void checkMessageUnitForFatal(long curTime, MessageUnit unit, MessageUnit prev) {
//        boolean a = curTime - unit.getFd() > config.getMesLifeTime();
//        boolean b = prev!=null && prev.getMessageId()==unit.getMessageId() && prev.getStatus()==FATAL;
//        if (a || b) {
//            if (unit.getStatus() == DELAYED) 
//                blockedNums.remove(unit.getDst());
//            unit.fatal();
//        }
//    }
//
//    private boolean handleMessageUnitStatus(MessageUnit unit, long curTime, MessageUnit prevUnit, 
//            Iterator<MessageUnit> it, boolean noPrev) 
//    {
//        switch (unit.getStatus()) {
//            case DELAYED:
//                if (curTime > unit.getXTime()) {
//                    blockedNums.remove(unit.getDst());
//                    unit.ready();
//                }
//                break;
//            case SUBMITTED:
//                if (curTime - unit.getXTime() > config.getMaxWaitForResp()) 
//                    unit.ready();
//                break;
//            case CONFIRMED:
//                if (prevUnit == null || (prevUnit.getMessageId() != unit.getMessageId())) {
//                    it.remove();
//                    if (unit.isLastSeg()) 
//                        messageSubmitted(unit, true);
//                    noPrev = true;
//                }
//                break;
//            case FATAL:
//                it.remove();
//                sended.remove(unit.getSequenceNumber());
//                if (unit.isLastSeg()) 
//                    messageSubmitted(unit, false);
//                break;
//        }
//        return noPrev;
//    }

    public void statusChanged(MessageUnit unit, MessageUnitStatus oldStatus, MessageUnitStatus newStatus) {
        switch (newStatus) {
            case SUBMITTED: submitted.put(unit.getSequenceNumber(), unit); break;
            case DELAYED: blockDirection(unit.getDst(), unit.getXTime()); break; //block direction
        }
        switch (oldStatus) {
            case SUBMITTED: submitted.remove(unit.getSequenceNumber()); break;
        }
    }
    
    private void blockDirection(String dst, long untilTime) {
        blockedNums.putIfAbsent(dst, untilTime);
    }
    
    private boolean isDirectionAvailable(String dst) {
        Long time = blockedNums.get(dst);
        long curTime = System.currentTimeMillis();
        if (time!=null && time<curTime)
            return false;
        else if (time>=curTime) 
            blockedNums.remove(dst);
        return true;
    }

    public void messageHandled(boolean success, Object tag) {
        mesCount.decrementAndGet();
    }
}