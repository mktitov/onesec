/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.sms.queue;

import com.logica.smpp.Data;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.DataSM;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.ValueNotSetException;
import com.logica.smpp.pdu.tlv.TLVShort;
import com.logica.smpp.pdu.tlv.TLVUByte;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.onesec.raven.sms.impl.SmsIncomingMessageChannel;
import org.onesec.raven.sms.sm.udh.UDH;
import org.onesec.raven.sms.sm.udh.UDHData;
import org.raven.RavenRuntimeException;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.AbstractDataProcessorWithLogger;
import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
public class InQueue extends AbstractDataProcessorWithLogger {
    public final static String CHECK_MESSAGE_RECEIVE_TIMEOUT = "CHECK_MESSAGE_RECEIVE_TIMEOUT";
    public final static String GET_RECEIVED_PACKETS = "GET_RECEIVED_PACKETS";
    public final static String GET_RECEIVED_MESSAGES = "GET_RECEIVED_MESSAGES";
    public final static String GET_RECEIVED_DATASM_PACKETS = "GET_RECEIVED_DATASM_PACKETS";
    public final static String GET_RECEIVED_DELIVERYSM_PACKETS = "GET_RECEIVED_DELIVERYSM_PACKETS";
    public final static String GET_RECEIVED_UDH_PACKETS = "GET_RECEIVED_UDH_PACKETS";
    public final static String GET_RECEIVED_SAR_PACKETS = "GET_RECEIVED_SAR_PACKETS";
    public final static String GET_PROCESSING_ERRORS = "GET_PROCESSING_ERRORS";
    
    private final Map<String, Message> messages = new HashMap<String, Message>();
    private final String defaultCp;
    private final SmsIncomingMessageChannel messageChannel;
    private final long messageReceiveTimeout;
    private long receivedPackets;
    private long receivedMessages;
    private long dataSMpackets;
    private long deliverySMpackets;
    private long udhPackets;
    private long sarPackets;
    private long processingErrors;

    public InQueue(final String defaultCp, final SmsIncomingMessageChannel messageChannel, final long messageReceiveTimeout) 
    {
        this.defaultCp = defaultCp;
        this.messageChannel = messageChannel;
        this.messageReceiveTimeout = messageReceiveTimeout;
    }

    @Override
    public void init(DataProcessorFacade facade, DataProcessorContext context) {
        super.init(facade, context); 
        try {
            facade.sendRepeatedly(messageReceiveTimeout, messageReceiveTimeout, 0, CHECK_MESSAGE_RECEIVE_TIMEOUT);
        } catch (ExecutorServiceException ex) {
            if (logger.isErrorEnabled())
                logger.error("Error initializing inbound queue", ex);
            throw new RavenRuntimeException("Error initializing inbound queue", ex);
        }
    }

    public Object processData(Object message) {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Processing message: "+message);
            if (message instanceof Request)
                processRequest((Request) message);
            else if (message==CHECK_MESSAGE_RECEIVE_TIMEOUT)
                checkMessagesTimeout();
            else if (message==GET_RECEIVED_MESSAGES)
                return receivedMessages;
            else if (message==GET_RECEIVED_PACKETS)
                return receivedPackets;
            else if (message==GET_RECEIVED_DATASM_PACKETS)
                return dataSMpackets;
            else if (message==GET_RECEIVED_DELIVERYSM_PACKETS)
                return deliverySMpackets;
            else if (message==GET_PROCESSING_ERRORS)
                return processingErrors;
            else if (message==GET_RECEIVED_UDH_PACKETS)
                return udhPackets;
            else if (message==GET_RECEIVED_SAR_PACKETS)
                return sarPackets;
            else {
                if (logger.isDebugEnabled())
                    logger.warn("Received UNRECOGNIZED message. Ignoring...");
            }
        } catch (Exception e) {
            processingErrors++;
            if (logger.isErrorEnabled())
                logger.error("Error processing message: "+message, e);
        }
        return VOID;
    }
    
    private void checkMessagesTimeout() {
        if (messages.isEmpty())
            return;
        final Iterator<Map.Entry<String, Message>> it = messages.entrySet().iterator();
        final long curTime = System.currentTimeMillis();
        while (it.hasNext()) {
            final Map.Entry<String, Message> entry = it.next();
            if (entry.getValue().created+messageReceiveTimeout < curTime) {
                if (logger.isWarnEnabled())
                    logger.warn("Reached CONCATENATED_MESSAGE_RECEIVE_TIMEOUT({}ms) for message: {}"
                            , messageReceiveTimeout, entry.getValue());
                it.remove();
            }
        }
    }
    
    private void processRequest(Request request) throws Exception {
        if (!(request instanceof DataSM) && !(request instanceof DeliverSM)) {
            if (logger.isWarnEnabled())
                logger.warn("Unknown pdu type: "+request.getClass().getName());
            return;
        }
        if (request instanceof DeliverSM)
            deliverySMpackets++;
        else
            dataSMpackets++;
        receivedPackets++;
        MessagePart messagePart = request instanceof DeliverSM?
                MessagePart.decode((DeliverSM) request, defaultCp) :
                MessagePart.decode((DataSM) request, defaultCp);
        if (messagePart.sarPacket)
            sarPackets++;
        else if (messagePart.udhPacket)
            udhPackets++;
        if (messagePart.isConcatenated()) {
            final String key = messagePart.getKey();
            Message message = messages.get(key);
            if (message==null) 
                messages.put(key, new Message(messagePart));
            else {
                MessagePart readyMessage = message.addMessagePart(messagePart);
                if (readyMessage!=null) {
                    messages.remove(key);
                    receivedMessages++;
                    messageChannel.sendMessage(readyMessage);
                }
            }
        } else {
            receivedMessages++;
            messageChannel.sendMessage(messagePart);
        }
    }
    
    private static class Message {
        private final List<MessagePart> parts = new LinkedList<MessagePart>();
        private final int expectedPartsCount;
        private final long created;

        public Message(MessagePart messagePart) {
            this.expectedPartsCount = messagePart.getMessageSegCount();
            this.created = messagePart.receiveTs;
            parts.add(messagePart);
        }
        
        public MessagePart addMessagePart(MessagePart messagePart) {
            parts.add(messagePart);
            if (expectedPartsCount==parts.size()) {
                Collections.sort(parts);
                final StringBuilder buf = new StringBuilder();
                for (MessagePart part: parts)
                    buf.append(part.message);
                messagePart.message = buf.toString();
                return messagePart;
            } 
            return null;
        }

        @Override
        public String toString() {
            final MessagePart mess = parts.get(0);
            return String.format(
                    "Concatenated message: id=%s, srcAddr=%s, dstAddr=%s, expectedPartsCount=%s, receivedPartsCount=%s "
                    , mess.messageId, mess.srcAddress, mess.dstAddress, expectedPartsCount, parts.size());
        }                
    }
    
    public static class MessagePart implements Comparable<MessagePart> {
        private final long receiveTs = System.currentTimeMillis();
        
        private Integer messageId;
        private short messageSegNum = 0;
        private short messageSegCount = 0;
        private int sequenceNumber;
        
        private short esmClass;
        private short dataCoding;
        
        private String srcAddress;
        private byte srcNpi;
        private byte srcTon;
        
        private String dstAddress;
        private byte dstNpi;
        private byte dstTon;
        private boolean sarPacket = false;
        private boolean udhPacket = false;
        
        private String message;
        
        public static MessagePart decode(DeliverSM pdu, String defaultCp) throws Exception {
            MessagePart part = new MessagePart();
            ByteBuffer buf = null;
            try {
                buf = pdu.getBShortMessage().getClone();
                if(buf==null) 
                    buf = pdu.getMessagePayload().getClone();
            } catch (ValueNotSetException e) {}
            buf = checkByteBuffer(buf);
            
            part.esmClass = UDH.decodeUnsigned(pdu.getEsmClass());
            part.dataCoding = UDH.decodeUnsigned(pdu.getDataCoding());
            
            part.sequenceNumber = pdu.getSequenceNumber();
            part.srcAddress = pdu.getSourceAddr().getAddress();
            part.srcNpi = pdu.getSourceAddr().getNpi();
            part.srcTon = pdu.getSourceAddr().getTon();
            
            part.dstAddress = pdu.getDestAddr().getAddress();
            part.dstNpi = pdu.getDestAddr().getNpi();
            part.dstTon = pdu.getDestAddr().getTon();
            
            try {
                part.messageId = (int)pdu.getSarMsgRefNum();
            } catch (ValueNotSetException e) {}
            if (part.messageId!=null) {
                part.messageSegNum = pdu.getSarSegmentSeqnum();
                part.messageSegCount = pdu.getSarTotalSegments();
                part.sarPacket=true;
            } else {
                decodeUdh(part.esmClass, part, buf);
            }
            part.message = UDH.getMesText(buf.getBuffer(), part.dataCoding, defaultCp);
            return part;
        }
        
        public static MessagePart decode(DataSM pdu, String defaultCp) throws Exception {
            MessagePart part = new MessagePart();
            ByteBuffer buf = null;
            try {
                buf = pdu.getMessagePayload().getClone();
            } catch (ValueNotSetException e) {}
            buf = checkByteBuffer(buf);
            
            part.esmClass = UDH.decodeUnsigned(pdu.getEsmClass());
            part.dataCoding = UDH.decodeUnsigned(pdu.getDataCoding());
            
            part.sequenceNumber = pdu.getSequenceNumber();
            part.srcAddress = pdu.getSourceAddr().getAddress();
            part.srcNpi = pdu.getSourceAddr().getNpi();
            part.srcTon = pdu.getSourceAddr().getTon();
            
            part.dstAddress = pdu.getDestAddr().getAddress();
            part.dstNpi = pdu.getDestAddr().getNpi();
            part.dstTon = pdu.getDestAddr().getTon();
            
            part.messageId = getSarMessageId(pdu);
            if (part.messageId!=null) {
                part.messageSegNum = getSarMessageSegNum(pdu);
                part.messageSegCount = getSarMessageSegCount(pdu);
            } else {
                decodeUdh(part.esmClass, part, buf);
                part.message = UDH.getMesText(buf.getBuffer(), part.dataCoding, defaultCp);
            }
            return part;
        }
        
        private static void decodeUdh(short esmClass, MessagePart part, ByteBuffer buf) throws Exception {
            if ( (esmClass & 0x40) != 0) {
                UDHData udhd = new UDHData(buf);
                if (udhd.isValid()) {
                    if (udhd.isConcatenated()) {
                        part.messageId = udhd.getConcatRef();
                        part.messageSegCount = (short) udhd.getConcatMsgCount();
                        part.messageSegNum = (short) udhd.getConcatMsgCur();
                    }
                    buf.removeBuffer(udhd.getUdhLength());
                    part.udhPacket = true;
                } else {
                    throw new Exception("Invalid UDH");
                }
            }
        }
        
        private static ByteBuffer checkByteBuffer(ByteBuffer buf) throws NotEnoughDataInByteBufferException {
            if (buf==null)
                buf = new ByteBuffer();
            if (buf.length()>512)
                buf.removeBuffer(512);
            return buf;
        }
        
        private static Integer getSarMessageId(Request pdu) throws ValueNotSetException {
            TLVShort sarMsgId = (TLVShort) pdu.getExtraOptional(Data.OPT_PAR_SAR_MSG_REF_NUM);
            return sarMsgId!=null && sarMsgId.hasValue()? -1*sarMsgId.getValue() : null;
        }
        
        private static short getSarMessageSegCount(Request pdu) throws ValueNotSetException {
            TLVUByte segCount = (TLVUByte) pdu.getExtraOptional(Data.OPT_PAR_SAR_TOT_SEG);
            return segCount.getValue();
        }
        
        private static short getSarMessageSegNum(Request pdu) throws ValueNotSetException {
            TLVUByte segNum = (TLVUByte) pdu.getExtraOptional(Data.OPT_PAR_SAR_SEG_SNUM);
            return segNum.getValue();
        }
        
        private boolean isConcatenated() {
            return messageId!=null && messageSegCount>1;
        }
        
        private String getKey() {
            return srcAddress+"-"+dstAddress+"-"+messageId;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public short getEsmClass() {
            return esmClass;
        }

        public short getDataCoding() {
            return dataCoding;
        }

        public String getSrcAddress() {
            return srcAddress;
        }

        public byte getSrcNpi() {
            return srcNpi;
        }

        public byte getSrcTon() {
            return srcTon;
        }

        public String getDstAddress() {
            return dstAddress;
        }

        public byte getDstNpi() {
            return dstNpi;
        }

        public byte getDstTon() {
            return dstTon;
        }

        public String getMessage() {
            return message;
        }

        public Integer getMessageId() {
            return messageId;
        }

        public short getMessageSegCount() {
            return messageSegCount;
        }

        public long getReceiveTs() {
            return receiveTs;
        }        

        public int compareTo(MessagePart t) {
            return Integer.compare(messageSegNum, t.messageSegNum);
        }
    }
}
