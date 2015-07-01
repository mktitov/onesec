package org.onesec.raven.sms.sm.udh;

public class UDHWrongLengthException extends UDHException {
	static final long serialVersionUID = 0;

    public UDHWrongLengthException()
    {
        super("The TLV is shorter or longer than allowed.");
    }
    
    public UDHWrongLengthException(int min, int max, int actual)
    {
        super("The UDH is shorter or longer than allowed: min=" + min + " max=" + max + " actual=" + actual+ ".");
    }

}
