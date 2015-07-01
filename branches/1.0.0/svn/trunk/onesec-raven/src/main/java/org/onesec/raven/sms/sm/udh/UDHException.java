package org.onesec.raven.sms.sm.udh;

import com.logica.smpp.pdu.PDUException;
public class UDHException extends PDUException {
	static final long serialVersionUID = 0;
	    public UDHException()
	    {
	        super();
	    }
	    
	    public UDHException(String s)
	    {
	        super(s);
	    }
	
}
