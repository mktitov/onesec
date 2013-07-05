package org.onesec.raven.sms.sm.udh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;

public class InfElement  
{
	private static Logger log = LoggerFactory.getLogger(InfElement.class);

    private short tag = 0;
    private int dataLength = 0;
    private boolean valueInited = false; // must be set by setValueData()
    private ByteBuffer data = new ByteBuffer();

    /**
     * For checking the min/max length limits that the particular limit
     * <i>shouldn't</i> be checked.
     */
    private static int DONT_CHECK_LIMIT = -1;

    /**
     * The minimal length of the data. If no min length is required,
     * then set to <code>DONT_CHECK_LIMIT</code>.
     */
    private int minLength = DONT_CHECK_LIMIT;

    /**
     * The maximal length of the data. If no max length is required,
     * then set to <code>DONT_CHECK_LIMIT</code>.
     */
    private int maxLength = DONT_CHECK_LIMIT;

    public InfElement(int tag) 
    {
    	this.tag = (short)tag;
    	this.dataLength = 0;
    	valueInited = true;
		if(log.isDebugEnabled()) log.debug("set: "+debugString());
    }
    
    public InfElement(int tag, ByteBuffer dt) 
    {
    	this.tag = (short)tag;
//    	this.data.setBuffer(dt.getClone().getBuffer());
    	this.dataLength = data.length();
    	valueInited = true;
		if(log.isDebugEnabled()) log.debug("set: "+debugString());
    }

    public InfElement(int tag, byte data) 
    {
    	this.tag = (short)tag;
    	this.data = new ByteBuffer();
    	this.data.appendByte(data);
    	this.dataLength = 1;
    	valueInited = true;
		if(log.isDebugEnabled()) log.debug("set: "+debugString());
    }
    
    public void appendBytes(ByteBuffer bf)
    {
    	this.data.appendBuffer(bf);
    	this.dataLength = this.data.length();
    }

    private int xxx;	
    public void setSegNum(int x)
    {
    	
    }
    
    public void appendByte(byte bf)
    {
    	this.data.appendByte(bf);
    	this.dataLength = this.data.length();
    }
    
    public InfElement(ByteBuffer dt) throws NotEnoughDataInByteBufferException
    {
//    	try {
    	tag = UDH.decodeUnsigned(dt.removeByte());
    	dataLength = UDH.decodeUnsigned(dt.removeByte());
    	data = dt.removeBytes(dataLength);
    	valueInited = true;
    	if(log.isDebugEnabled()) log.debug("parse: "+debugString());
    	//} catch(NotEnoughDataInByteBufferException e) {}
    }

    /**
     * Compares this IE to another IE. IE are equal if their tags
     * are equal.
     */
    public boolean equals(Object obj)
    {
        if ((obj != null) && (obj instanceof InfElement)) return getTag() == ((InfElement)obj).getTag();
        return false;
    }

    /**
     * Throws exception if the <code>length</code> provided isn't between
     * provided <code>min</code> and <code>max</code> inclusive.
     */
    protected static void checkLength(int min, int max, int length)
    throws UDHWrongLengthException
    {
    	if ((length < min) || (length > max)) 
        	throw new UDHWrongLengthException(min, max, length);
    }

    /**
     * Throws exception if the length provided isn't between min and max
     * lengths of this TLV.
     */
    protected void checkLength(int length) throws UDHWrongLengthException
    {
    	int min = 0;
        int max = 0;
        if (minLength != DONT_CHECK_LIMIT) min = minLength;
        	else min = 0;
        if (maxLength != DONT_CHECK_LIMIT) max = maxLength;
         	else max = Integer.MAX_VALUE;
        checkLength(min, max, length);
    }

    /**
     * Throws exception if the length of the buffer provided isn't between
     * min and max lengths provided.
     */
    protected static void checkLength(int min, int max, ByteBuffer buffer)
    throws UDHWrongLengthException
    {
        int length=0;
        if (buffer != null) length = buffer.length();
        checkLength(min, max, length);
    }
    
    protected void checkLength(ByteBuffer buffer)
    throws UDHWrongLengthException
    {
        int length=0;
        if (buffer != null) length = buffer.length();
        checkLength(length);
    }

    public String debugString()
    {    
    	 if(data==null) return "Data is NULL !";
		 return "IE id="+UDH.getHexDump((byte)tag)+" len="+UDH.getHexDump((byte)dataLength)+" data="+data.getHexDump();
    }

	public ByteBuffer getFullData() 
	{
		if(!isValueInited()) return null;
		ByteBuffer bf = new ByteBuffer();
		bf.appendByte((byte)tag);
		bf.appendByte((byte)dataLength);
		bf.appendBuffer(data);
		return bf; 
	}
    

	public ByteBuffer getData() { return data; }
	public void setData(ByteBuffer data) { this.data = data; }

	public int getDataLength() { return dataLength; }
	public void setDataLength(int len) { this.dataLength = len; }

	public int getIELength() { return getDataLength()+2; }
	
	public int getTag() { return tag; }
	public void setTag(byte tag) { this.tag = tag; }
	public void setTag(int tag) { this.tag = (byte)(tag&0xff); }

	public boolean isValueInited() { return valueInited; }
	public void setValueInited(boolean valueIsSet) { this.valueInited = valueIsSet; }
    

}
