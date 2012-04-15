package org.onesec.raven.sms;

import org.onesec.raven.sms.queue.ShortTextMessage;

import com.logica.smpp.pdu.SubmitSM;

public interface SmCoder 
{
	public SubmitSM[] encode(ShortTextMessage sm);

}
