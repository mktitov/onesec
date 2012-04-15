package org.onesec.raven.sms.queue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onesec.raven.sms.SmCoder;
import com.logica.smpp.pdu.SubmitSM;

public class OutQueue {

    private static Logger log = LoggerFactory.getLogger(OutQueue.class);
    public int mesSequence = 0;
    private Integer mesCount = 0;
    private int maxMesCount = 100;
    /**
     * Максимальное время нахождения сообщения в очереди (мс).
     */
    private long mesLifeTime = 10 * 60 * 1000;
    /**
     * @see OutQueue.factorQF
     */
    private long mesWaitQF = 60 * 1000;
    /**
     * @see OutQueue.factorTH
     */
    private long mesWaitTH = 60 * 1000;
    /**
     * Максимальное время ожидания потверждения на отправленное сообщение (мс).
     */
    private long maxWaitForResp = 60 * 1000;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 14 - QUEUE_FULL (мс). If QUEUE_FULL
     * : waitInterval = mesWaitQF * ( 1+factorQF*attempsCount )
     */
    private int factorQF = 0;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 58 - THROTTLED (мс). If THROTTLED :
     * waitInterval = mesWaitTH * ( 1+factorTH*attempsCount )
     */
    private int factorTH = 0;
    private int maxAttempts = 5;
    private LinkedBlockingQueue<MessageUnit> queue = new LinkedBlockingQueue<MessageUnit>();
    private HashMap<Integer, MessageUnit> sended = new HashMap<Integer, MessageUnit>();
    private HashSet<String> blockedNums = new HashSet<String>();
    private SmCoder coder;

    public OutQueue(SmCoder c) {
        coder = c;
    }

    //public static MessageUnit[] noack
    public int howManyUnconfirmed() {
        return sended.size();
    }

    public boolean addMessage(ShortTextMessage sm) {
        SubmitSM[] ssm = coder.encode(sm);
        if (ssm == null || ssm.length == 0) {
            return false;
        }
        int mesId;
        synchronized (mesCount) {
            if (mesCount + 1 > maxMesCount) {
                return false;
            }
            mesId = mesSequence++;
            for (int i = 0; i < ssm.length; i++) {
                queue.offer(new MessageUnit(ssm[i], mesId, ssm.length, i));
            }
            mesCount++;
        }
        log.info("addMessage text:'{}' id:{} segments:{}", new Object[]{sm.getMessage(), mesId, ssm.length});
        return true;
    }

    public void sended(MessageUnit u) {
        sended(u, System.currentTimeMillis());
    }

    public void sended(MessageUnit u, long time) {
        sended.put(u.getPdu().getSequenceNumber(), u);
        u.sended();
    }

    public void throttled(int sequence, long time) {
        MessageUnit u = sended.get(sequence);
        if (u == null) 
            return;
        long delay = mesWaitTH * (1 + factorTH * u.getAttempts());
        u.waitingFor(delay + time);
        blockedNums.add(u.getDst());
        sended.remove(sequence);
    }

    public void queueIsFull(int sequence, long time) {
        MessageUnit u = sended.get(sequence);
        if (u == null) 
            return;
        long delay = mesWaitQF * (1 + factorQF * u.getAttempts());
        u.waitingFor(delay + time);
        blockedNums.add(u.getDst());
        sended.remove(sequence);
    }

    public void confirmed(int sequence, long time) {
        MessageUnit u = sended.get(sequence);
        if (u == null) {
            return;
        }
        u.confirmed();
        sended.remove(sequence);
    }

    public void failed(int sequence, long time) {
        MessageUnit u = sended.get(sequence);
        if (u == null) {
            return;
        }
        u.fatal();
        sended.remove(sequence);
    }

    public void messageSended(MessageUnit u, boolean ok) {
        synchronized (mesCount) {
            mesCount--;
        }
    }

    public MessageUnit getNext() {
        Iterator<MessageUnit> it = queue.iterator();
        long t = System.currentTimeMillis();
        MessageUnit prev = null;
        boolean noPrev;
        while (it.hasNext()) {
            noPrev = false;
            MessageUnit m = it.next();

            synchronized (m) {
                boolean a = t - m.getFd() > mesLifeTime;
                boolean b = prev != null && prev.getMessageId() == m.getMessageId() && prev.getStatus() == MessageUnit.FATAL;
                if (a || b) {
                    if (m.getStatus() == MessageUnit.WAIT) 
                        blockedNums.remove(m.getDst());
                    m.fatal();
                }

                switch (m.getStatus()) {
                    case MessageUnit.WAIT:
                        if (t > m.getXtime()) {
                            blockedNums.remove(m.getDst());
                            m.ready();
                        }
                        break;
                    case MessageUnit.SENDED:
                        if (t - m.getXtime() > maxWaitForResp) {
                            m.ready();
                        }
                        break;
                    case MessageUnit.CONFIRMED:
                        if (prev == null || (prev.getMessageId() != m.getMessageId())) {
                            it.remove();
                            if (m.isLastSeg()) {
                                messageSended(m, true);
                            }
                            noPrev = true;
                        }
                        break;
                    case MessageUnit.FATAL:
                        it.remove();
                        if (m.isLastSeg()) {
                            messageSended(m, false);
                        }
                        break;
                }
            }
            if (m.getStatus() == MessageUnit.READY) {
                if (m.getAttempts() >= maxAttempts) {
                    m.fatal();
                } else if (!blockedNums.contains(m.getDst())) {
                    return m;
                }
            }
            if (!noPrev) {
                prev = m;
            }
        }
        return null;
    }

    public int getMaxMesCount() {
        return maxMesCount;
    }

    public void setMaxMesCount(int maxMesCount) {
        this.maxMesCount = maxMesCount;
    }

    public long getMesLifeTime() {
        return mesLifeTime;
    }

    public void setMesLifeTime(long mesLifeTime) {
        this.mesLifeTime = mesLifeTime;
    }

    public long getMesWaitQF() {
        return mesWaitQF;
    }

    public void setMesWaitQF(long mesWaitQF) {
        this.mesWaitQF = mesWaitQF;
    }

    public long getMesWaitTH() {
        return mesWaitTH;
    }

    public void setMesWaitTH(long mesWaitTH) {
        this.mesWaitTH = mesWaitTH;
    }

    public long getMaxWaitForResp() {
        return maxWaitForResp;
    }

    public void setMaxWaitForResp(long maxWaitForResp) {
        this.maxWaitForResp = maxWaitForResp;
    }

    public int getFactorQF() {
        return factorQF;
    }

    public void setFactorQF(int factor) {
        if (factor < 0) {
            factor = 0;
        }
        factorQF = factor;
    }

    public int getFactorTH() {
        return factorTH;
    }

    public void setFactorTH(int factor) {
        if (factor < 0) {
            factor = 0;
        }
        factorTH = factor;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
