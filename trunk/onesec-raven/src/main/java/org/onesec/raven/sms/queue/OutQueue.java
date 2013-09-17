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
import java.util.concurrent.atomic.AtomicLong;
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
//    private final long mesWaitQF = 60 * 1000;
    /**
     * @see OutQueue.factorTH
     */
//    private final long mesWaitTH = 60 * 1000;
    /**
     * Максимальное время ожидания потверждения на отправленное сообщение (мс).
     */
//    private long maxWaitForResp = 60 * 1000;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 14 - QUEUE_FULL (мс). If QUEUE_FULL
     * : waitInterval = mesWaitQF * ( 1+factorQF*attempsCount )
     */
//    private final int factorQF = 0;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 58 - THROTTLED (мс). If THROTTLED :
     * waitInterval = mesWaitTH * ( 1+factorTH*attempsCount )
     */
//    private final int factorTH = 0;
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
    private final AtomicLong timePeriod = new AtomicLong();
    private final AtomicLong submittedInPeriod = new AtomicLong();
    
    private final AtomicLong totalUnits = new AtomicLong();
    private final AtomicLong submittedUnits = new AtomicLong();
    private final AtomicLong confirmedUnits = new AtomicLong();
    private final AtomicLong fatalUnits = new AtomicLong();
    private final AtomicLong confirmTime = new AtomicLong();
    
    private final AtomicLong totalMessages = new AtomicLong();
    private final AtomicLong successMessages = new AtomicLong();
    private final AtomicLong unsuccessMessages = new AtomicLong();
    private final AtomicLong sentTime = new AtomicLong();

    public OutQueue(SmsConfig config, LoggerHelper logger) {
        this.config = config;
        this.logger = new LoggerHelper(logger, "Queue. ");
    }
    
    public void clear() {
        queue.clear();
        submitted.clear();
    }

    //public static MessageUnit[] noack
    public int howManyUnconfirmed() {
        return submitted.size();
    }
    
    public long getAvgSentTime() {
        return sentTime.get()==0l? 0 : (successMessages.get()+unsuccessMessages.get())/sentTime.get();
    }
    
    public long getAvgConfirmTime() {
        return confirmTime.get()==0l? 0 : confirmedUnits.get() / confirmTime.get();
    }
    
    public long getTotalMessages() {
        return totalMessages.get();
    }
    
    public long getSuccessMessages() {
        return successMessages.get();
    }
    
    public long getUnsuccessMessages() {
        return unsuccessMessages.get();
    }
    
    public int getMessagesInQueue() {
        return mesCount.get();
    }
    
    public long getTotalUnits() {
        return totalUnits.get();
    }
    
    public long getSubmittedUnits() {
        return submittedUnits.get();
    }
    
    public long getConfirmedUnits() {
        return confirmedUnits.get();
    }
    
    public long getFatalUnits() {
        return fatalUnits.get();
    }

    public boolean addMessage(ShortTextMessage sm) {
        if (mesCount.incrementAndGet() > config.getMaxMessagesInQueue()) {
            mesCount.decrementAndGet();
            if (logger.isWarnEnabled())
                logger.warn("Can't queue message. Queue is FULL");
            return false;
        } else {
            totalMessages.incrementAndGet();
            sm.addListener(this);
            MessageUnit[] units = sm.getUnits();
            totalUnits.addAndGet(units.length);
            for (int i = 0; i < units.length; i++) 
                queue.offer(units[i].addListener(this));
            if (logger.isDebugEnabled())
                logger.debug("Message ({}) queued", sm);
            return true;
        }
    }

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

    public MessageUnit getNext() {
        if (checkSubmitted()) 
            for (Iterator<MessageUnit> it=queue.iterator(); it.hasNext();) {
                MessageUnit unit = it.next();
                if (isDirectionAvailable(unit.getDst()) && unit.checkStatus()==READY)
                    return unit;
                else if (ObjectUtils.in(unit.getStatus(), CONFIRMED, FATAL)) 
                    it.remove();
            }
        return null;
    }
    
    private boolean checkSubmitted() {
        if (submitted.size()>=config.getMaxUnconfirmed()) {
            if (logger.isTraceEnabled())
                logger.trace("Can't provide message unit. Too many unconfirmed message units");
            return false;
        }
        if (config.getMaxMessageUnitsPerTimeUnit()<=0)
            return true;
        if (getUnitsInPeriod() < config.getMaxMessageUnitsPerTimeUnit()) return true;
        else {
            if (logger.isTraceEnabled()) 
                logger.trace(String.format("Exceeded max messages per time unit. %s messages per %s %s"
                        , config.getMaxMessageUnitsPerTimeUnit()
                        , config.getMaxMessageUnitsTimeQuantity()
                        , config.getMaxMessageUnitsTimeUnit().toString()));
            return false;
        }
    }
    
    public long getUnitsInPeriod() {
        checkPeriod();
        return submittedInPeriod.get();
    }
    
    private void checkPeriod() {
        if (config.getMaxMessageUnitsPerTimeUnit()<=0)
            return;
        final long curPeriod = getCurPeriod();
        if (timePeriod.get()!=curPeriod) {
            timePeriod.set(curPeriod);
            submittedInPeriod.set(0);
        }
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
            case SUBMITTED: 
                submittedUnits.incrementAndGet();
                submitted.put(unit.getSequenceNumber(), unit); 
                checkPeriod();
                submittedInPeriod.incrementAndGet();
                break;
            case DELAYED: blockDirection(unit.getDst(), unit.getXTime()); break; //block direction
            case CONFIRMED: 
                confirmedUnits.incrementAndGet(); 
                confirmTime.addAndGet(unit.getConfirmTime());
                break;
            case FATAL: fatalUnits.incrementAndGet(); break;
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
        if (time==null) 
            return true;
        else if (time>System.currentTimeMillis()) 
            return false;
        else {
            blockedNums.remove(dst);
            return true;
        }
    }

    public void messageHandled(ShortTextMessage msg, boolean success, Object tag) {
        mesCount.decrementAndGet();
        sentTime.addAndGet(msg.getHandledTime());
        if (success) successMessages.incrementAndGet();
        else unsuccessMessages.incrementAndGet();
    }

    private long getCurPeriod() {
        return System.currentTimeMillis() / config.getMaxMessageUnitsTimeUnit().toMillis(config.getMaxMessageUnitsTimeQuantity());
    }
}