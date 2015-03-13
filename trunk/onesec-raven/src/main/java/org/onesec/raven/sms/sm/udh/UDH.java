package org.onesec.raven.sms.sm.udh;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onesec.raven.sms.SmeA;
import com.logica.smpp.Data;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import org.onesec.raven.sms.SmsUtil;

public class UDH {
	 private static Logger log = LoggerFactory.getLogger(UDH.class);

	private HashMap<Integer,InfElement> iemap=new HashMap<Integer,InfElement>();
	private boolean valid=false;
	private int udhLength=0; // size of UDH
	private int concatRef;
	private int concatMsgCount;
	private int concatMsgCur;
	private boolean concatenated=false;

	public static final byte CONCAT_8BIT = 0;
	public static final byte SPECIAL_SMI = 1;
	public static final byte APP_PORT_8 = 4;
	public static final byte APP_PORT_16 = 5;
	public static final byte SELECTIVE_STATUS_REPORT = 6;
	public static final byte UDH_SOURCE_INDICATOR = 7;
	public static final byte CONCAT_16BIT = 8;

	public static final int UDH_HEADER_SIZE  = 2;  // 2 octets: tag & length
	public static final int SM_DATA_LENGTH  = 140;  // 2 octets: tag & length

	public UDH() {}
	
	public UDH(ByteBuffer buf) { init(buf); }

	/**
	 * @param buf - буфер для инициализвции UDH
	 * @return 0 - если инициализация прошла успешно
	 */
	public int init(ByteBuffer buf)
	{
		if(buf==null || buf.length()==0) { log.warn("UDH buffer is empty !"); return -1;	}
		valid = false;
		try {
			udhLength = decodeUnsigned(buf.removeByte()) + 1;
			int size = udhLength;
			log.debug("UDH size="+udhLength);
			while(size > 2)
			{
				InfElement ie = new InfElement(buf); 
				putIE(ie);
				size -= ie.getDataLength()+UDH_HEADER_SIZE;
			}
			findConcat();
		} catch(NotEnoughDataInByteBufferException e) { log.warn("Bad UDH data !",e); return -2;}
		valid=true;
		return 0;
	}

	/**
	 * @return Сформированный UDH
	 */
	public ByteBuffer getUDH()
	{
		ByteBuffer buf = new ByteBuffer();
		ByteBuffer ief=null;
		for(InfElement el : iemap.values())
		{
			ief = el.getFullData();
			if(ief!=null) buf.appendBuffer(ief);
		}
		int buflen = buf.length();
		ByteBuffer outbuf = new ByteBuffer();
		outbuf.appendByte((byte)buflen);
		outbuf.appendBuffer(buf);
		return outbuf;
	}
	
	private static String sar_found = "CONCATENATED_SM found";
	/**
	 * Ищет IE, описывающие фрагментацию.
	 */
	private void findConcat()
	{   
		concatenated = false;
		if(containsIE(UDH.CONCAT_8BIT))
		{
			ByteBuffer bfx = iemap.get(new Integer(UDH.CONCAT_8BIT)).getData();
//			byte[] x = bfx.getClone().getBuffer();
                     byte[] x = SmsUtil.cloneBuffer(bfx);
			if(x.length < 3) return;
			concatRef = UDH.decodeUnsigned(x[0]);
			concatMsgCount = UDH.decodeUnsigned(x[1]);
			concatMsgCur = UDH.decodeUnsigned(x[2]);
			concatenated = true;
			log.debug(sar_found+"(8):"+concatRef+"/"+concatMsgCount+"/"+concatMsgCur);
			return;
		}
  		if(containsIE(UDH.CONCAT_16BIT))
		{
			ByteBuffer bfx = iemap.get(new Integer(UDH.CONCAT_16BIT)).getData();
//			ByteBuffer xx = bfx.getClone();
			ByteBuffer xx = SmsUtil.cloneByteBuffer(bfx);
			if(xx.length() < 4) return;
			try {
			concatRef = xx.removeShort(); 
			concatMsgCount = xx.removeByte();
			concatMsgCur = xx.removeByte();
			concatenated = true;
			log.debug(sar_found+"(16):"+concatRef+"/"+concatMsgCount+"/"+concatMsgCur);
			} catch(NotEnoughDataInByteBufferException e) { log.warn("Bad UDH",e);}
		}
	}
	
	/**
	 * Поиск IE в UDH
	 * @param id - идентификатор IE
	 * @return true - если найден
	 */
	public boolean containsIE(int id)
	{
		return iemap.containsKey((Object)new Integer(id));
	}
	
	/**
	 * @return Количество IE в UDH
	 */
	public int getIECount() { return iemap.size(); }
	
	/**
	 * @return Длина всех IE
	 */
	public int getAllIeLen()
	{
		int z=0;
		for(InfElement x : iemap.values())
			z += x.getIELength();
		return z;
	}
	
	/**
	 * Добавляет IE в UDH
	 */
	public void putIE(InfElement ie)
	{
		iemap.put(new Integer(ie.getTag()),ie);
		udhLength = getAllIeLen()+1;
	}

	public boolean isValid() {	return valid; }
	public void setValid(boolean valid) { this.valid = valid; }
	
	public boolean isConcatenated() { return concatenated; }
	public void setConcatenated(boolean concatenated) { this.concatenated = concatenated; }
	
	public int getConcatMsgCount() { return concatMsgCount; }
	public void setConcatMsgCount(int concatMsgCount) { this.concatMsgCount = concatMsgCount; }
	
	public int getConcatMsgCur() { return concatMsgCur; }
	public void setConcatMsgCur(int concatMsgCur) { this.concatMsgCur = concatMsgCur; }
	
	public int getConcatRef() { return concatRef; }
	public void setConcatRef(int concatRef) { this.concatRef = concatRef; }

	public int getUdhLength() { return udhLength; }
	public void setUdhLength(int lengthUDH) { this.udhLength = lengthUDH; }

	/**
	 * convert a single char to corresponding nibble.
	 *
	 * @param c char to convert. must be 0-9 a-f A-F, no
	 * spaces, plus or minus signs.
	 *
	 * @return corresponding integer
	 */
	 private static int charToNibble ( char c )
	    {
	    if ( '0' <= c && c <= '9' ) return c - '0';
	    if ( 'a' <= c && c <= 'f' ) return c - 'a' + 0xa;
	    if ( 'A' <= c && c <= 'F' ) return c - 'A' + 0xa;
	       throw new IllegalArgumentException ( "Invalid hex character: " + c );
	    }

	public static ByteBuffer fromHexString ( String s )
	{
		int stringLength = s.length();
		if ( (stringLength & 0x1) != 0 ) { return null; }
		ByteBuffer b = new ByteBuffer();
		try {
			for ( int i=0,j=0; i<stringLength; i+=2,j++ )
			{
				int high = charToNibble( s.charAt ( i ) );
				int low = charToNibble( s.charAt ( i+1 ) );
				b.appendByte((byte)( ( high << 4 ) | low ));
			}
		} catch(IllegalArgumentException e) { log.error("Bad hex string !",e);} 
		return b;
	}

	public static short getShort(byte[] buf,int index)
	 {
		short result = 0;
		result |= buf[index]&0xff;
		result <<= 8;
		result |= buf[index+1]&0xff;
		return result;
	 }

	public static String getHexDump(byte x)
	{
		return	""+Character.forDigit((x >> 4) & 0x0f, 16)+Character.forDigit(x & 0x0f, 16);	
	}

	public static String getHexDump(byte[] xx)
	{
	    String dump = "";
	    try {
	        int dataLen = xx.length;
	        //byte[] buffer = getBuffer();
	        for (int i=0; i<dataLen; i++) {
	            dump += Character.forDigit((xx[i] >> 4) & 0x0f, 16);
	            dump += Character.forDigit(xx[i] & 0x0f, 16);
	        }
	    } catch (Throwable t) {
	        // catch everything as this is for debug
	        dump = "Throwable caught when dumping = " + t;
	    }
	    return dump;
	}
	
	public static void prn(String s)
	{
		System.out.println(s);		
	};

	public static void main(String[] args)
	{
		String t = "MTS-Bonus"; //"Vam zvonili!";
		byte[] b = t.getBytes();
		byte[] bb = to7bit(b);
		prn(t+" "+getHexDump(b));
		prn("to7bit "+getHexDump(bb));
		prn("back-from7bit " + getHexDump(from7bit(bb)));
		prn("back-from7bitStr " + new String(from7bit(bb)));
		
		byte[] a = new byte[]{(byte)0x53,(byte)0x7B,(byte)0x3E,(byte)0xAC,(byte)0x77,(byte)0xBF,(byte)0xF3,(byte)0x42,(byte)0xF7,(byte)0x1A }; //{(byte)0xD8,(byte)0xDA,(byte)0x90,(byte)0x5D,(byte)0x17,(byte)0x03};
		prn("from7bit " + new String(from7bit(a)));
		prn("from7bit " + getHexDump(from7bit(a)));
		//ByteBuffer z = fromHexString("D8DA905D1703");
		//String res = new String(z.getBuffer());
		//prn("from7bit_ " + res);
		
	}
	
	public static byte[] from7bit(byte[] src)
	    {
	    	double x = java.lang.Math.ceil(((double)8)/7*src.length);
//	    	System.out.println("X="+x);
	    	byte[] dst = new byte[(int)x]; 
//	    	System.out.println("dstsize"+dst.length);
	    	int notEmptyBits=0;
	    	int bc=0;
	    	byte toNext=0,a,b;
	    	for(int i=0;i<src.length;i++)
	    	{
	    		a = src[i];
	//    		if(i<5) { System.out.println("a is "+Character.forDigit((a >> 4) & 0x0f, 16)+Character.forDigit(a & 0x0f, 16));
	//    		System.out.println("toN is "+getHexDump(toNext)+" notEmptyBits="+notEmptyBits);
	//    		}
	    		if(notEmptyBits==0) b = (byte)(a & 0x7f); 
	    		  else { 
	    			b = (byte)((byte)(a << notEmptyBits) & 0x7f);
	    			b |= toNext;
	    		  }
	//    		if(i<5) System.out.println("b is "+getHexDump(b));
	 		    dst[bc++] = b;
	   		    toNext = (byte)(decodeUnsigned(a) >>> (7-notEmptyBits));
	    		//notEmptyBits++;
	    		if(++notEmptyBits==7)
	    		{
	    			notEmptyBits=0;
	     		    dst[bc++] = toNext;
	    		}
	    	}
	    	return dst;
	    }

	public static byte[] to7bit(byte[] src)
	{
		double x = java.lang.Math.ceil(((double)7)/8*src.length);
		byte[] dst = new byte[(int)x]; 
		int emptyBits=0;
		int bc=0;
		for(int i=0;i<src.length;i++)
		{
			byte b = src[i];
			if(emptyBits==0) dst[bc]=b;
			else { 
				dst[bc++] |= (byte)(b << (8-emptyBits));
				if(emptyBits<7) dst[bc] |= (byte)(decodeUnsigned(b) >>> emptyBits);
			}
			emptyBits++;
			if(emptyBits>7) emptyBits=0;
		}
		return dst;
	}

	public static int decodeUnsigned(short signed)
	{
		if (signed>=0) return signed;
	    return (int)(65536+(int)signed);
	}

	public static short decodeUnsigned(byte signed)
	{
	    if (signed>=0) return signed;
	    return (short)(256+(short)signed);
	}
	
	public static String getMesText(byte[] bf,int dcs,String cp)
	{
		if(bf==null) return null;
		if(cp==null) cp = Data.ENC_ASCII;
		String respCP = Data.ENC_ASCII;
		String txt = ""; 
		switch(dcs)
		{
		case 0x08 : respCP = Data.ENC_UTF16_BE; break;
		case 0x07 : respCP = "ISO-8859-8"; break;
		case 0x06 : respCP = "ISO-8859-5"; break;
		case 0x04 : respCP = cp; break;
		case 0x03 : respCP = "ISO-8859-1"; break;
		case 0x02 : respCP = cp; break;
		}
		try { txt = new String(bf,respCP); }
		catch(UnsupportedEncodingException e) { log.error("Xmm..",e);}
		if(dcs==0) txt = SmeA.fromGSM_0338(txt);
		return txt;
	}
	
}
