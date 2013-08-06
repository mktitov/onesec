package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.ShortMessageListener;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.raven.tree.impl.LoggerHelper;

public class ShortTextMessageImpl {

    private final String dst;
    private final String message;
    private final Object tag;
    private final ShortMessageListener listener;
    private final long id;
    private final MessageUnitImpl[] units;
    private final LoggerHelper logger;
    
    private final AtomicInteger unitsCount = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

    public ShortTextMessageImpl(String dstAddr, String mes, Object tag, long id, ShortMessageListener listener, 
            SmsMessageEncoder encoder, LoggerHelper logger) 
        throws Exception
    {
        this.dst = dstAddr;
        this.message = mes;
        this.tag = tag;
        this.listener = listener;
        this.id = id;
        this.logger = new LoggerHelper(logger, "Message ["+id+"]. ");
        SubmitSM[] frags = encoder.encode(mes, dstAddr);
        if (frags==null || frags.length==0)
            throw new Exception("Message encoding error. May be message is empty? Message: "+message);
        MessageUnitImpl[] _units = new MessageUnitImpl[frags.length];
        unitsCount.set(frags.length);
        for (int i=0; i<frags.length; ++i)
            _units[i] = new MessageUnitImpl(frags[i], this, i==frags.length-1, new LoggerHelper(logger, "Unit ["+i+"]. "));
        this.units = _units;
    }
    
    public MessageUnitImpl[] getUnits() {
        return units;
    }
    
    void unitHandled(boolean success) {
        int count = unitsCount.decrementAndGet();
        boolean stat = this.success.compareAndSet(true, success);
        if (count<=0) {
            if (logger.isDebugEnabled())
                logger.debug("Handled, success = "+stat);
            listener.messageHandled(stat, tag);
        }
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

    @Override
    public String toString() {
        return "id: "+id+"; message: "+message;
    }
}
