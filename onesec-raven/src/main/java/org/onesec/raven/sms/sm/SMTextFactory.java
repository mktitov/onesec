package org.onesec.raven.sms.sm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import com.logica.smpp.Data;
import com.logica.smpp.SmppException;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import org.onesec.raven.sms.ISmeConfig;
import org.onesec.raven.sms.SmCoder;
import org.onesec.raven.sms.queue.ShortTextMessage;
import org.onesec.raven.sms.sm.udh.UDH;
import org.onesec.raven.sms.sm.udh.UDHData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMTextFactory extends SMFactory implements SmCoder
{
	 private static Logger log = LoggerFactory.getLogger(SMTextFactory.class);
	 /**
	  * Режим передачи длинных сообщений с использованием : 0 - SAR, 1 - message payload, 2 - UDH(8-bit reference), 3 - UDH(16-bit reference)  
	  */
	 private int longSmMode = 0;

	//   SMTextFactory() { super(); }
	   public SMTextFactory(ISmeConfig sp) { 
		   super(sp); 
		   longSmMode = sp.getLongSmMode();
	   }
	 
/*	   
	public int getPDU(SmeParams sp,OutMes om,ArrayList al)
	{
		setParams(sp);
		longSmMode = sp.getLongSmMode();
		if(sp.getDataCoding()==0)
		{
			log.info("DC="+sp.getDataCoding());
			om.setSmdata(SmeA.toGSM_0338(om.getSmdata()));
		}	
		try {
	       srcAddr = new Address(sp.getSrcTon(),sp.getSrcNpi(),om.getService()); 	
	       dstAddr = new Address(sp.getDstTon(),sp.getDstNpi(),om.getSubscriber()); 	
		   } catch(WrongLengthOfStringException e) {return -1;}
		   try { return submit(om.getSmdata(),al);
		} catch(SmppException e) {log.error("Xmm...",e); return -2;}
	}
	*/   
/**
 */

private ByteBuffer getMessageData(String shortMessage)
{
	ByteBuffer bmes = new ByteBuffer();
	if(shortMessage==null || shortMessage.length()==0) return bmes;
	try{
		  if(dataCoding==0) //
		  {
			  byte[] bm = shortMessage.getBytes(super.messageCP);
			  if(use7bit)
			  {
				  if(bm.length > UDH.SM_DATA_LENGTH && longSmMode==2)
                   {
		    	    ByteBuffer tmp = new ByteBuffer();
		    	    for(int i=0;i<7;i++) tmp.appendByte((byte)0);
		    	    tmp.appendBytes(bm);
		    	    try { tmp.removeBytes(6); } catch(NotEnoughDataInByteBufferException e) {}
		    	    bm = tmp.getBuffer();
		           }
				  bmes.appendBytes(UDH.to7bit(bm));
			  } else bmes.appendBytes(bm);
		  }
		  else
		  {	String respCP = Data.ENC_ASCII;
		  	switch(dataCoding)
		  	{
		  	case 0x08 : respCP = Data.ENC_UTF16_BE; break;
		  	case 0x07 : respCP = "ISO-8859-8"; break;
		  	case 0x06 : respCP = "ISO-8859-5"; break;
		  	case 0x03 : respCP = "ISO-8859-1"; break;
		  	case 0x04 : respCP = super.messageCP; break;
		  	case 0x02 : respCP = super.messageCP; break;
		  	}
		  	bmes.appendBytes(shortMessage.getBytes(respCP));
		  } 
	} catch(UnsupportedEncodingException e) { log.error("Xmm...",e); return null;}

	return bmes;
}

public int submit(String shortMessage,ArrayList<SubmitSM> al) throws SmppException
{
	SubmitSM request = new SubmitSM();
	ByteBuffer bmes = getMessageData(shortMessage);
	if(bmes==null) return -1;
	
	if(bmes.length() <= UDH.SM_DATA_LENGTH)
	{
		setRequestParams(request);
		request.setShortMessageData(bmes);
		al.add(request);
		return 0;
	}
	if(longSmMode==1)
	{
		al.add(submitLongPayload(bmes));
		return 0;
	}
	
	if(longSmMode==0) return submitSar(bmes,al);
	
	
	UDHData udhd = new UDHData();
	udhd.setMesData(bmes);
	if(longSmMode!=2) udhd.setUse16bitRef(true);
	boolean f16 = false;
	if(super.dataCoding==(byte)0x08) f16 = true;
	
	//Iterator it = udhd.getAllData(f16).iterator();
	for(ByteBuffer bb : udhd.getAllData(f16))
	{
		SubmitSM req = new SubmitSM();
		setRequestParams(req);
		req.setEsmClass((byte)0x40);
//		req.setDataCoding((byte)0xf5);
		req.setShortMessageData(bb);
		if(log.isDebugEnabled())
			 log.debug("TXT: "+req.debugString());
		al.add(req);
	}
	
 return 0;
}

private SubmitSM submitLongPayload(ByteBuffer message) throws SmppException
{
	SubmitSM req = new SubmitSM(); 
	setRequestParams(req);
	req.setMessagePayload(message); 
 return req;
}

 private static int segSequence=0;
 private int submitSar(ByteBuffer bmes,ArrayList<SubmitSM> al) throws SmppException
  {
	ArrayList<ByteBuffer> res = getFragments(bmes);
	if(res==null) return -1;
	segSequence++;
	int segCnt = res.size();
	int segCur=1;
	//Iterator it = res.iterator();
	for( ByteBuffer bb : res )
	{
		SubmitSM req = new SubmitSM();
		setRequestParams(req);
		req.setSarMsgRefNum((short)segSequence);
		req.setSarTotalSegments((short)segCnt);
		req.setSarSegmentSeqnum((short)segCur);
		req.setShortMessageData(bb);
		//if(log.isDebugEnabled())
			 log.info("SAR("+segSequence+"/"+segCnt+"/"+segCur+") "+req.debugString());
		al.add(req);
		segCur++;
	}
	return 0;
  }
  
  private ArrayList<ByteBuffer> getFragments(ByteBuffer message)
	{
		ArrayList<ByteBuffer> x = new ArrayList<ByteBuffer>();
		int segCnt,curSeg;
		int maxSize = UDH.SM_DATA_LENGTH-8;
					   
		segCnt = (int) Math.ceil( ((double)message.length()) / maxSize);
		for(curSeg=1;curSeg<=segCnt; curSeg++)
		{
			ByteBuffer bb = new ByteBuffer();
			int xl = Math.min(maxSize,message.length());
			try { bb.appendBuffer(message.removeBuffer(xl));} 
			catch (NotEnoughDataInByteBufferException e) {log.error("Bad data !!!",e);}
			x.add(bb);
		}	  
	return x;
	}

	public SubmitSM[] encode(ShortTextMessage sm) 
	{
		try {
			dstAddr = new Address((byte)config.getDstTon(), (byte)config.getDstNpi(), sm.getDst());
			ArrayList<SubmitSM> z = new ArrayList<SubmitSM>();
			int r = submit(sm.getMessage(),z);
			if(r==0)
				return z.toArray(new SubmitSM[]{});
		} catch (SmppException e) {
			log.error("encode:", e);
		}
		return null;
	}

}
