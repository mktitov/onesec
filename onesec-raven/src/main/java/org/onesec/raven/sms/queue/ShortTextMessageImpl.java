package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.MessageUnit;
import org.onesec.raven.sms.MessageUnitListener;
import org.onesec.raven.sms.MessageUnitStatus;
import static org.onesec.raven.sms.MessageUnitStatus.*;
import org.onesec.raven.sms.ShortMessageListener;
import org.onesec.raven.sms.ShortTextMessage;
import org.onesec.raven.sms.SmsConfig;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.raven.tree.impl.LoggerHelper;

public class ShortTextMessageImpl implements ShortTextMessage, MessageUnitListener {

    private final String dst;
    private final String message;
    private final Object tag;
    private final List<ShortMessageListener> listeners = new ArrayList<ShortMessageListener>(2);
    private final long id;
    private final MessageUnit[] units;
    private final LoggerHelper logger;
    private final long created = System.currentTimeMillis();
    private volatile long handled = 0l;
    
    private final AtomicInteger unitsCount = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

    public ShortTextMessageImpl(String dstAddr, String mes, Object tag, long id, 
            SmsMessageEncoder encoder, SmsConfig config, LoggerHelper logger) 
        throws Exception
    {
        this.dst = dstAddr;
        this.message = mes;
        this.tag = tag;
        this.id = id;
        this.logger = new LoggerHelper(logger, "Message ["+id+"]. ");
        SubmitSM[] frags = encoder.encode(mes, dstAddr);
        if (frags==null || frags.length==0)
            throw new Exception("Message encoding error. May be message is empty? Message: "+message);
        MessageUnit[] _units = new MessageUnitImpl[frags.length];
        unitsCount.set(frags.length);
        for (int i=0; i<frags.length; ++i)
            _units[i] = new MessageUnitImpl(frags[i], config, new LoggerHelper(this.logger, "Unit ["+i+"]. "))
                        .addListener(this);
        this.units = _units;
    }
    
    public MessageUnit[] getUnits() {
        return units;
    }

    public void statusChanged(MessageUnit unit, MessageUnitStatus oldStatus, MessageUnitStatus newStatus) {
        if (newStatus==CONFIRMED || newStatus==FATAL) {
            boolean stat = this.success.compareAndSet(true, newStatus==CONFIRMED);
            if (unitsCount.decrementAndGet()==0) {
                handled = System.currentTimeMillis();
                if (logger.isDebugEnabled())
                    logger.debug("Handled, success = "+stat);
                for (ShortMessageListener listener: listeners)
                    listener.messageHandled(this, stat, tag);                
            } else if (!stat)
                for (MessageUnit u: units)
                    u.fatal();
        }
    }
    
    public long getHandledTime() {
        return handled - created;
    }
    
    public void addListener(ShortMessageListener listener) {
        listeners.add(listener);
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
