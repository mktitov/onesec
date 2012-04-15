package org.onesec.raven.sms.queue;

import com.logica.smpp.pdu.SubmitSM;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.SmCoder;
import org.onesec.raven.sms.SmsTranseiverNode;

public class ShortTextMessage {

    private final String dst;
    private final String message;
    private final Object tag;
    private final SmsTranseiverNode transeiver;
    
    private int unitsCount;

    public ShortTextMessage(String dstAddr, String mes, Object tag, SmsTranseiverNode transeiver) {
        this.dst = dstAddr;
        this.message = mes;
        this.tag = tag;
        this.transeiver = transeiver;
    }
    
    public MessageUnit[] getUnits(SmCoder coder, AtomicInteger counter) {
        SubmitSM[] ssm = coder.encode(this);
        if (ssm == null || ssm.length == 0) 
            return null;
        MessageUnit[] units = new MessageUnit[ssm.length];
        unitsCount = ssm.length;
        for (int i=0; i<unitsCount; ++i)
            units[i] = new MessageUnit(ssm[i], counter.incrementAndGet(), unitsCount, i);
        return units;
    }

    public int getUnitsCount() {
        return unitsCount;
    }

    public String getDst() {
        return dst;
    }

    public String getMessage() {
        return message;
    }
}
