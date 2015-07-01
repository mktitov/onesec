package org.onesec.raven.sms.sm.udh;


import com.logica.smpp.util.ByteBuffer;
//import com.logica.smpp.util.NotEnoughDataInByteBufferException;

public class IEC8 extends InfElement {
//	private int refNum,segCount,curSeg;
	
	public static int IEC8_SIZE = 5;

	public IEC8(int ref,int all,int cur) 
	{
		super(UDH.CONCAT_8BIT);
		ByteBuffer bf = new ByteBuffer();
		bf.appendByte((byte)(ref&0xff));
		bf.appendByte((byte)(all&0xff));
		bf.appendByte((byte)(cur&0xff));
		super.appendBytes(bf);
	}

	public void setRefNum(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[0] = (byte)(n&0xff);
	}

	public void setSegCount(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[1] = (byte)(n&0xff);
	}

	public void setSegNum(int n)
	{
		byte[] x = super.getData().getBuffer();
		x[2] = (byte)(n&0xff);
	}

}
