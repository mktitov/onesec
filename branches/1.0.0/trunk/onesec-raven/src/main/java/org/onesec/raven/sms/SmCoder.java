package org.onesec.raven.sms;

import org.onesec.raven.sms.queue.ShortTextMessageImpl;

import com.logica.smpp.pdu.SubmitSM;

public interface SmCoder 
{
	public SubmitSM[] encode(ShortTextMessageImpl sm);

}
