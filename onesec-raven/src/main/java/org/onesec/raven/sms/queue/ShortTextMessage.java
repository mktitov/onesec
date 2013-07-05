package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.ShortMessageListener;
import org.onesec.raven.sms.SmCoder;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;

public class ShortTextMessage {

    private final String dst;
    private final String message;
    private final Object tag;
    private final ShortMessageListener listener;
    private final long id;
    private final MessageUnit[] units;
    
    private final AtomicInteger unitsCount = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

    public ShortTextMessage(String dstAddr, String mes, Object tag, long id, ShortMessageListener listener, 
            SmsMessageEncoder encoder, LoggerHelper logger) 
        throws Exception
    {
        this.dst = dstAddr;
        this.message = mes;
        this.tag = tag;
        this.listener = listener;
        this.id = id;
        SubmitSM[] frags = encoder.encode(mes, dstAddr);
        if (frags==null || frags.length==0)
            throw new Exception("Message encoding error. May be message is empty? Message: "+message);
        MessageUnit[] _units = new MessageUnit[frags.length];
        unitsCount.set(frags.length);
        for (int i=0; i<frags.length; ++i)
            _units[i] = new MessageUnit(frags[i], this, i==frags.length-1, new LoggerHelper(logger, "["+i+"] "));
        this.units = _units;
    }
    
    public MessageUnit[] getUnits() {
        return units;
    }
    
    void unitHandled(boolean success) {
        int count = unitsCount.decrementAndGet();
        boolean stat = this.success.compareAndSet(true, success);
        if (count<=0)
            listener.messageHandled(stat, tag);
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
