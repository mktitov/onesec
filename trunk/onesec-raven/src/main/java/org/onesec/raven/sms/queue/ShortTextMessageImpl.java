package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.onesec.raven.sms.impl.SmsRecordSchemaNode;
import org.onesec.raven.sms.impl.SmsTransceiverNode;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.tree.impl.LoggerHelper;

public class ShortTextMessageImpl implements ShortTextMessage, MessageUnitListener {

    private final String dst;
    private final String message;
//    private final Object tag;
    private final List<ShortMessageListener> listeners = new ArrayList<ShortMessageListener>(2);
    private final long id;
    private final MessageUnit[] units;
    private final LoggerHelper logger;
    private final long created = System.currentTimeMillis();
    private final SmsTransceiverNode.RecordHolder originalMessage;
    private volatile long handled = 0l;
    
    private final AtomicInteger unitsCount = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

//    public ShortTextMessageImpl(String dstAddr, Address srcAddr, String mes, Record originalMessage, long id, 
//            SmsMessageEncoder encoder, SmsConfig config, LoggerHelper logger) 
    public ShortTextMessageImpl(SmsTransceiverNode.RecordHolder originalMessage,
            SmsMessageEncoder encoder, SmsConfig config, LoggerHelper logger) 
        throws Exception
    {
        this.originalMessage = originalMessage;
        Record rec = originalMessage.getRecord();
        this.message = (String) rec.getValue(SmsRecordSchemaNode.MESSAGE);
        this.dst = (String) rec.getValue(SmsRecordSchemaNode.ADDRESS);
        Address srcAddr = config.getSrcAddr();
        String fromAddr = (String) rec.getValue(SmsRecordSchemaNode.FROM_ADDRESS);
        if (fromAddr!=null) {
            Byte srcTon = (Byte) rec.getValue(SmsRecordSchemaNode.FROM_ADDRESS_TON);
            srcTon = srcTon==null? config.getSrcAddr().getTon() : srcTon;
            Byte srcNpi = (Byte) rec.getValue(SmsRecordSchemaNode.FROM_ADDRESS_NPI);
            srcNpi = srcNpi==null? config.getSrcAddr().getNpi() : srcNpi;
            srcAddr = new Address(srcTon, srcNpi, fromAddr);
        }
        Byte dataCoding = (Byte) rec.getValue(SmsRecordSchemaNode.DATA_CODING);
        if (dataCoding==null)
            dataCoding = config.getDataCoding();
        this.id = (Long) rec.getValue(SmsRecordSchemaNode.ID);
        this.logger = new LoggerHelper(logger, "Message ["+id+"]. ");
        
        SubmitSM[] frags = encoder.encode(message, dst, srcAddr, dataCoding);
        if (frags==null || frags.length==0)
            throw new Exception("Message encoding error. May be message is empty? Message: "+message);
        MessageUnit[] _units = new MessageUnitImpl[frags.length];
        unitsCount.set(frags.length);
        Boolean registeredDelivery = (Boolean) rec.getValue(SmsRecordSchemaNode.NEED_DELIVERY_RECEIPT);
        SimpleDateFormat parser = new SimpleDateFormat("dd:HH:mm");
        parser.setLenient(false);
        SimpleDateFormat formatter = new SimpleDateFormat("0000ddHHmm00000");
        for (int i=0; i<frags.length; ++i) {
            //setting up registered_delivery flag
            if (registeredDelivery!=null && registeredDelivery) 
                frags[i].setRegisteredDelivery((byte)0x01);
            if (registeredDelivery==null)
                frags[i].setRegisteredDelivery(config.getRegisteredDelivery());
            //setting up expiration date
            String expirePeriod = (String) rec.getValue(SmsRecordSchemaNode.MESSAGE_EXPIRE_PERIOD);
            if (expirePeriod==null)
                expirePeriod = config.getMessageExpireTime();
            frags[i].setValidityPeriod(String.format("0000%s00000R", expirePeriod.replaceAll(":","")));
//            Date parsedDate = parser.parse(expirePeriod);
//            frags[i].setValidityPeriod(formatter.format(parsedDate)+"R");
            _units[i] = new MessageUnitImpl(this, frags[i], config, new LoggerHelper(this.logger, "Unit ["+i+"]. "))
                        .addListener(this);
        }
        this.units = _units;
    }
    
    public MessageUnit[] getUnits() {
        return units;
    }

    public void setMessageId(String messageId) {
        try {
            originalMessage.getRecord().setValue(SmsRecordSchemaNode.MESSAGE_ID, messageId);
        } catch (RecordException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error setting message_id for sms message record", ex);
        }
    }

    public void statusChanged(MessageUnit unit, MessageUnitStatus oldStatus, MessageUnitStatus newStatus) {
        if (newStatus==CONFIRMED || newStatus==FATAL) {
            boolean stat = this.success.compareAndSet(true, newStatus==CONFIRMED);
            if (unitsCount.decrementAndGet()==0) {
                handled = System.currentTimeMillis();
                if (logger.isDebugEnabled())
                    logger.debug("Handled, success = "+stat);
                for (ShortMessageListener listener: listeners)
                    listener.messageHandled(this, stat, originalMessage);                
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

    public SmsTransceiverNode.RecordHolder getOriginalMessage() {
        return originalMessage;
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
