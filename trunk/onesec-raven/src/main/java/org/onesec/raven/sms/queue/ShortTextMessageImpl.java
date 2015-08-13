package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.tlv.TLV;
import com.logica.smpp.pdu.tlv.TLVByte;
import com.logica.smpp.pdu.tlv.TLVEmpty;
import com.logica.smpp.pdu.tlv.TLVInt;
import com.logica.smpp.pdu.tlv.TLVOctets;
import com.logica.smpp.pdu.tlv.TLVShort;
import com.logica.smpp.pdu.tlv.TLVString;
import com.logica.smpp.util.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
        final Collection<TLV> optParams = decodeOptionalParams(rec);
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
            //adding optional parameters
            if (optParams!=null)
                for (TLV optParam: optParams)
                    frags[i].setExtraOptional(optParam);
            _units[i] = new MessageUnitImpl(this, frags[i], config, new LoggerHelper(this.logger, "Unit ["+i+"]. "))
                        .addListener(this);
        }
        this.units = _units;
    }
    
    private Collection<TLV> decodeOptionalParams(final Record rec) throws Exception {
        Map<Object, Object> optionalParams = (Map) rec.getTag(SmsRecordSchemaNode.OPTIONAL_PARAMETERS_TAG);
        if (optionalParams==null)
            return null;
        List<TLV> tlvs = new ArrayList<>(optionalParams.size());
        try {
            for (Map.Entry<Object, Object> pair: optionalParams.entrySet()) 
                tlvs.add(decodeTLV(decodeTag(pair.getKey()), pair.getValue()));
        } catch (Exception e) {
            throw new Exception("Error decoding optional parameters for message: "+message, e);
        }
        return tlvs;
    }
    
    private short decodeTag(Object tagObj) throws Exception {
        try {
            return Short.decode(tagObj.toString());
        } catch (NumberFormatException e) {
            throw new Exception(String.format("TLV tag (%s) decoding error", tagObj), e);
        }
    }
    
    private TLV decodeTLV(short tag, Object value) throws Exception {
        try {
            if (value==null)
                return new TLVEmpty(tag, true);
            else if (value instanceof Byte)
                return new TLVByte(tag, (Byte)value);
            else if (value instanceof Integer)
                return new TLVInt(tag, (Integer)value);
            else if (value instanceof Short)
                return new TLVShort(tag, (Short)value);
            else if (value instanceof byte[])
                return new TLVOctets(tag, new ByteBuffer((byte[])value));
            else if (value instanceof String)
                return new TLVString(tag, (String)value);
            throw new Exception("Can't detect type of TLV value");
        } catch (Exception e) {
            throw new Exception(String.format("Error decoding TLV value (%s) for tag (%s)", value, tag), e);
        }        
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
