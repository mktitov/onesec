/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onesec.raven.sms.impl;

import com.logica.smpp.Data;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.util.ByteBuffer;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.onesec.raven.sms.SmsConfig;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.onesec.raven.sms.sm.udh.UDH;
import org.raven.tree.impl.LoggerHelper;
import static java.lang.Math.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.onesec.raven.sms.sm.udh.UDHData;
/**
 *
 * @author Mikhail Titov
 */
public class SmsMessageEncoderImpl implements SmsMessageEncoder {
    private final SmsConfig config;
    private final AtomicInteger sarMsgRefNum = new AtomicInteger();
    private final LoggerHelper logger;

    public SmsMessageEncoderImpl(SmsConfig config, LoggerHelper logger) {
        this.config = config;
        this.logger = new LoggerHelper(logger, "Encoder. ");
    }


    public SubmitSM[] encode(String message, String dstAddr, Address srcAddr, byte dataCoding) throws Exception {
        Address dst = new Address(config.getDstTon(), config.getDstNpi(), dstAddr);
        List<SubmitSM> parts = encodeMessage(message, dst, srcAddr, dataCoding);
        return parts==null || parts.isEmpty()? null : parts.toArray(new SubmitSM[parts.size()]);
    }
    
    private List<SubmitSM> encodeMessage(String message, Address dstAddr, Address srcAddr, byte dataCoding) throws Exception {
        ByteBuffer mesBuf = createMessageBuffer(message, dataCoding);
        if (mesBuf==null)
            return null;
        if (mesBuf.length() <= UDH.SM_DATA_LENGTH) 
            return Arrays.asList(createMessagePart(mesBuf, dstAddr, srcAddr, dataCoding, true));
        switch (config.getLongSmMode()) {
            case 1: return Arrays.asList(createMessagePart(mesBuf, dstAddr, srcAddr, dataCoding, false));
            case 2: return sliceMessage(mesBuf, dstAddr, srcAddr, dataCoding);
            default: return createUDHMessage(mesBuf, dstAddr, srcAddr, dataCoding);
        }
    }
    
    private List<SubmitSM> createUDHMessage(ByteBuffer mesBuf, Address dstAddr, Address srcAddr, byte dataCoding) throws Exception {
        UDHData udhd = new UDHData();
        udhd.setMesData(mesBuf);
        if (config.getLongSmMode() != 2) 
            udhd.setUse16bitRef(true);
        boolean f16 = dataCoding == (byte) 0x08;
        List<ByteBuffer> buffers = udhd.getAllData(f16);
        List<SubmitSM> messParts = new ArrayList<SubmitSM>(buffers.size());
        for (ByteBuffer bb : buffers) {
            SubmitSM req = createMessagePart(bb, dstAddr, srcAddr,  dataCoding, true);
            req.setEsmClass((byte) 0x40);
            messParts.add(req);
        }
        return messParts;
    }
    
    private SubmitSM createMessagePart(ByteBuffer mesBuf, Address dstAddr, Address srcAddr, byte dataCoding, boolean shortMes) 
            throws Exception 
    {
        SubmitSM req = new SubmitSM();
        req.setDestAddr(dstAddr);
        req.setSourceAddr(srcAddr);
        req.setReplaceIfPresentFlag(config.getReplaceIfPresentFlag());
        req.setEsmClass(config.getEsmClass());
        req.setProtocolId(config.getProtocolId());
        req.setPriorityFlag(config.getPriorityFlag());
        req.setRegisteredDelivery(config.getRegisteredDelivery());
        req.setSmDefaultMsgId(config.getSmDefaultMsgId());
//        req.setSequenceNumber(777);
//        req.assignInitialSequenceNumber(777);
        //request.assignSequenceNumber(true);
        req.setDataCoding(dataCoding);
//        req.setRegisteredDelivery((byte)0x01);
        try {
            req.setServiceType(config.getServiceType());
            req.setScheduleDeliveryTime("");
            req.setValidityPeriod("");
        } catch (Exception e) {
            if (logger.isWarnEnabled())
                logger.warn("Problem with setting  some of request parameters", e);
        }
        if (shortMes) req.setShortMessageData(mesBuf);
        else req.setMessagePayload(mesBuf);
        return req;
    }
    
    public ByteBuffer createMessageBuffer(String shortMessage, byte dataCoding) {
        if (shortMessage==null || shortMessage.isEmpty())
            return null;
        try {
            ByteBuffer mesBuf = new ByteBuffer();
            if (dataCoding == 0) {
                byte[] messBytes = shortMessage.getBytes(config.getMessageCP());
                if (config.isUse7bit()) {
                    if (messBytes.length > UDH.SM_DATA_LENGTH && config.getLongSmMode() == 2) {
                        byte[] tmp = new byte[messBytes.length+1];
                        tmp[0] = (byte)0;
                        System.arraycopy(messBytes, 0, tmp, 1, messBytes.length);
                        messBytes = UDH.to7bit(tmp);
                    }
                }
                mesBuf.appendBytes(messBytes);
            } else {
                String respCP = Data.ENC_ASCII;
                switch (dataCoding) {
                    case 0x08: respCP = Data.ENC_UTF16_BE; break;
                    case 0x07: respCP = "ISO-8859-8"; break;
                    case 0x06: respCP = "ISO-8859-5"; break;
                    case 0x03: respCP = "ISO-8859-1"; break;
                    case 0x04: 
                    case 0x02: respCP = config.getMessageCP(); break;
                }
                mesBuf.appendBytes(shortMessage.getBytes(respCP));
            }
            return mesBuf;
        } catch (UnsupportedEncodingException e) {
            logger.error("Converting message to bytes error", e);
            return null;
        }
    }
    
    private short getNextSarMessageRefNum() {
        int next = sarMsgRefNum.incrementAndGet();
        if (next<=Short.MAX_VALUE) return (short)next;
        else {
            synchronized(sarMsgRefNum) {
                next = sarMsgRefNum.get();
                if (next<=Short.MAX_VALUE) return (short)next;
                else {
                    sarMsgRefNum.set(1);
                    return 1;
                }
            }
        }
    }

    private List<SubmitSM> sliceMessage(ByteBuffer mesBuf, Address dstAddr, Address srcAddr, byte dataCoding) throws Exception {
        List<ByteBuffer> frags = getFragments(mesBuf);
        int segCnt = frags.size();
        int segCur = 1;
        short seqNum = getNextSarMessageRefNum();
        List<SubmitSM> messParts = new ArrayList<SubmitSM>(segCnt);
        for (ByteBuffer bb : frags) {
            SubmitSM req = createMessagePart(bb, dstAddr, srcAddr, dataCoding, true);
            req.setSarMsgRefNum(seqNum);
            req.setSarTotalSegments((short) segCnt);
            req.setSarSegmentSeqnum((short) segCur);
//            req.setShortMessageData(bb);
            req.setMessagePayload(bb);
            messParts.add(req);
            segCur++;
        }
        return messParts;
    }
    
    public List<ByteBuffer> getFragments(ByteBuffer message) throws Exception {
        int maxSize = UDH.SM_DATA_LENGTH - 8;
        int segCnt = (int) ceil(((double)message.length()) / maxSize);
        ArrayList<ByteBuffer> fragments = new ArrayList<ByteBuffer>(segCnt);
        for (int i=0; i < segCnt; i++) 
            fragments.add(message.removeBuffer(min(maxSize, message.length())));
        return fragments;
    }
}
