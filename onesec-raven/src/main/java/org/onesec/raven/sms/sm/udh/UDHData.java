package org.onesec.raven.sms.sm.udh;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;

public class UDHData extends UDH {
	private static Logger log = LoggerFactory.getLogger(UDHData.class);
	private static int refNum=256;
	private boolean use16bitRef = false;
	ByteBuffer mesData = new ByteBuffer();
	
	public void setMesData(byte[] b) { setMesData(new ByteBuffer(b)); }

	public void setMesData(ByteBuffer b) { mesData = b.getClone(); }
	
	public ByteBuffer getMesData() { return mesData.getClone(); }

	public UDHData() { }
	
	public UDHData(ByteBuffer b)
	{
		ByteBuffer bf = b.getClone(); 
        super.init(bf);
        mesData = bf;
	}
	
	public boolean isLongMessage()
	{
		if(super.getUdhLength() + mesData.length() <= UDH.SM_DATA_LENGTH) return false;  
		return true;
	}
 
	public ArrayList<ByteBuffer> getAllData(boolean fit16)
	{
		ArrayList<ByteBuffer> al = new ArrayList<ByteBuffer>();
		int ludh = super.getUdhLength();
		int lmes = mesData.length();
		if(ludh+lmes <= UDH.SM_DATA_LENGTH) { 
			ByteBuffer bf = new ByteBuffer();
			if(ludh>0) bf.appendBuffer(super.getUDH()); 
			bf.appendBuffer(mesData);
			al.add(bf);
			if(log.isDebugEnabled()) log.debug("add segment: " + bf.getHexDump());
			return al;
		}
		
		if(ludh==0) ludh=1;
		int iesegsz = IEC8.IEC8_SIZE;
		if(isUse16bitRef()) iesegsz = IEC16.IEC16_SIZE;
		ludh += iesegsz;
		if(fit16 && ludh%2!=0) ludh++;
		int segCount = (int) Math.ceil(((double)lmes)/(UDH.SM_DATA_LENGTH-ludh));
		InfElement ie = null;
		if(!isUse16bitRef()) ie = new IEC8(refNum++,segCount,1);
		  else ie = new IEC16(refNum++,segCount,1);
		super.putIE(ie);
		ludh = super.getUdhLength();
		if(fit16 && ludh%2!=0) ludh++;
		ByteBuffer bx = new ByteBuffer();
		bx.appendBuffer(mesData);
		for(int i=1;i<=segCount;i++)
		{
			int sl = Math.min(UDH.SM_DATA_LENGTH-ludh,bx.length());
			log.info("i="+i);
			log.info("sl="+sl);
			ByteBuffer rb = new ByteBuffer();
			ie.setSegNum(i);
			rb.appendBuffer(super.getUDH());
			try { rb.appendBuffer(bx.removeBuffer(sl)); } 
			 catch(NotEnoughDataInByteBufferException e) {log.error("Make UDH:",e);}
			al.add(rb);
//			if(log.isDebugEnabled())
				log.info("add "+ i +" segment: " + rb.getHexDump());
		}
		return al;
	}

	public boolean isUse16bitRef() { return use16bitRef; }
	public void setUse16bitRef(boolean use16bitRef) { this.use16bitRef = use16bitRef; }
}
