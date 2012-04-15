package org.onesec.raven.sms.sm.udh;


import com.logica.smpp.util.ByteBuffer;
//import com.logica.smpp.util.NotEnoughDataInByteBufferException;

public class IEC16 extends InfElement {

	public static int IEC16_SIZE = 6;
	public IEC16(int ref,int all,int cur) 
	{
		super(UDH.CONCAT_16BIT);
		ByteBuffer bf = new ByteBuffer();
		bf.appendShort((short)ref);
		bf.appendByte((byte)(all&0xff));
		bf.appendByte((byte)(cur&0xff));
		super.appendBytes(bf);
	}
	
	public void setRefNum(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[0] = (byte)((n>>>8) & 0xff);
		x[1] = (byte)(n & 0xff);
	}

	public void setSegCount(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[2] = (byte)(n&0xff);
	}

	public void setSegNum(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[3] = (byte)(n&0xff);
	}


}
