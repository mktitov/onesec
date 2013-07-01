package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.SmCoder;
import org.onesec.raven.sms.SmsTransceiverNode;
import org.raven.tree.impl.LoggerHelper;

public class ShortTextMessage {

    private final String dst;
    private final String message;
    private final Object tag;
    private final SmsTransceiverNode transeiver;
    private final long id;
    private final LoggerHelper logger;
    
    private final AtomicInteger unitsCount = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

    public ShortTextMessage(String dstAddr, String mes, Object tag, SmsTransceiverNode transeiver, 
            long id, LoggerHelper logger) 
    {
        this.dst = dstAddr;
        this.message = mes;
        this.tag = tag;
        this.transeiver = transeiver;
        this.id = id;
        this.logger = logger;
    }
    
    public MessageUnit[] getUnits(SmCoder coder) {
        SubmitSM[] ssm = coder.encode(this);
        if (ssm == null || ssm.length == 0) {
            transeiver.messageHandled(this, false);
            return null;
        }
        MessageUnit[] units = new MessageUnit[ssm.length];
        unitsCount.set(ssm.length);
        for (int i=0; i<ssm.length; ++i)
            units[i] = new MessageUnit(ssm[i], this, i==ssm.length-1, new LoggerHelper(logger, "["+i+"] "));
        return units;
    }
    
    void unitHandled(boolean success) {
        int count = unitsCount.decrementAndGet();
        boolean stat = this.success.compareAndSet(true, success);
        if (count<=0)
            transeiver.messageHandled(this, stat);
    }

    public long getId() {
        return id;
    }

    public Object getTag() {
        return tag;
    }

    public int getUnitsCount() {
        return unitsCount.get();
    }

    public String getDst() {
        return dst;
    }

    public String getMessage() {
        return message;
    }
}
