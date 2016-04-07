/*
 * Copyright 2016 Mikhail Titov.
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
package org.onesec.raven.sip.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;
import java.util.List;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipMessage;
import org.onesec.raven.sip.SipMessageDecoderException;
import org.onesec.raven.sip.SipMessageDecoderState;
import org.raven.tree.impl.LoggerHelper;
import static org.onesec.raven.sip.impl.SipUtils.*;

/**
 *
 * @author Mikhail Titov
 */
public class SipMessageDecoder implements SipConstants {
    public final static int MAX_INITIAL_LINE_SIZE = 256;
    public final static int MAX_HEADER_LINE_SIZE = 1024;
    
    public final static SipMessageDecoderException decoderException = new SipMessageDecoderException();
    private final LoggerHelper logger;
    private final AppendableCharSequence line = new AppendableCharSequence(128);
    private final String[] messageTypeLine = new String[3];
    private SipMessage message;
    private final HeadersParser headersParser = new HeadersParser();
    private String currentHeaderName;
    private String currentHeaderValue;

    public SipMessageDecoder(final LoggerHelper logger) {
        this.logger = logger;
    }
    
    protected SipMessageDecoderState decode(SipMessageDecoderState state, ChannelHandlerContext ctx, ByteBuf in, List<Object> out) 
            throws Exception 
    {
        switch(state) {
            case INIT: 
                if (!skipControlCharacters(in))
                    return SipMessageDecoderState.INIT;
//                state = SipMessageDecoderState.DETECT_MESSAGE_TYPE;
            case DETECT_MESSAGE_TYPE: 
                if (!readLine(in, MAX_INITIAL_LINE_SIZE))
                    return SipMessageDecoderState.DETECT_MESSAGE_TYPE;
                boolean res = splitExactByWs(messageTypeLine, line, 0);
                if (!res) {
                    if (logger.isErrorEnabled())
                        logger.error("Error decoding initial line: "+line);
                    throw decoderException;
                }
                if (SIP_2_0.equals(messageTypeLine[0])) {
                    //decoding response
                } else {
                    //decoding request
                    message = new SipRequestImpl(messageTypeLine[0], messageTypeLine[1], messageTypeLine[2]);
                }
                line.reset();
                state = SipMessageDecoderState.READING_HEADERS;
            case READING_HEADERS: 
                if (!readHeaders(in))
                    return SipMessageDecoderState.READING_HEADERS;
                state = SipMessageDecoderState.READING_CONTENT;
            case READING_CONTENT:
                if (!readContent(in))
                    return SipMessageDecoderState.READING_CONTENT;
                
                break;            
        }
        return state;
    }
    
    private boolean readHeaders(final ByteBuf buf) throws SipMessageDecoderException {
        for (;;) {
            if (!readLine(buf, MAX_HEADER_LINE_SIZE)) 
                return false;
            if (line.length()==0) {
                if (currentHeaderName!=null)
                    headersParser.add(currentHeaderName, currentHeaderValue);
                headersParser.addHeadersToMessage(message);
                return true;
            } if (Character.isWhitespace(line.charAt(0))) {
                //appending string to current header;
                if (currentHeaderName==null) {
                    if (logger.isErrorEnabled())
                        logger.error("Invalid start of header: "+line);
                    throw decoderException;
                } else {
                    currentHeaderValue += " "+line;
                }
                line.reset();
            } else {
                //creating new header
                if (currentHeaderName!=null) 
                    headersParser.add(currentHeaderName, currentHeaderValue);
                parseHeader(line);
                line.reset();
            }
        } 
    }
    
    private boolean readContent(final ByteBuf buf) throws SipMessageDecoderException {
        final Integer contentLength = message.getContentLength();
        if (contentLength==null || contentLength<0) {
            if (logger.isErrorEnabled())
                logger.error("SIP message must have valid Content-Length header");
            throw decoderException;
        }
        if (contentLength==0)
            return true;
        final String contentType = message.getContentType();
        if (contentType==null || contentType.isEmpty()) {
            if (logger.isErrorEnabled())
                logger.error("SIP message with not empty body must have Content-Type header");
            throw decoderException;
        }
        if (buf.readableBytes()<contentLength)
            return false;        
//        parseContent(buf);
        return true;
    }
    
    private void parseContent(final String contentType, final ByteBuf buf) {
        
    }
    
    private void parseHeader(AppendableCharSequence headerLine) throws SipMessageDecoderException {
        int pos = indexOf(':', headerLine);
        if (pos<1) {
            if (logger.isErrorEnabled())
                logger.error("Invalid header: "+headerLine);
            throw decoderException;        
        }
        currentHeaderName = headerLine.substring(0, pos);
        currentHeaderValue = pos+1<headerLine.length()? headerLine.substring(pos+1, headerLine.length()) : "";
    }
    
    private boolean readLine(final ByteBuf buf, final int maxSize) throws SipMessageDecoderException {
        char prevChar = line.length()==0? 0 : line.charAt(line.length()-1);
        for (int i=0; i<buf.readableBytes(); ++i) {
            char ch = (char)buf.readByte();
            if (prevChar==HttpConstants.CR && ch==HttpConstants.LF) 
                return true;
            else {
                prevChar = ch;
                if (line.length()>=maxSize) {
                    if (logger.isErrorEnabled())
                        logger.error("SIP header or initial line is lager than "+maxSize+" bytes.");
                    throw decoderException;
                }
                if (ch!=HttpConstants.CR && ch!=HttpConstants.LF) {
                    line.append(ch);                
                }
            }
        }
        return false;
    }
    
    static boolean skipControlCharacters(ByteBuf buf) {       
        int ch;
        for (int i=0; i<buf.readableBytes(); i++) {
            ch = buf.readUnsignedByte();
            if (!Character.isISOControl(ch) && !Character.isWhitespace(ch)) {
                buf.readerIndex(buf.readerIndex()-1);
                return true;
            }
        }
        return false;
    }
    
    private void reset() {
        message = null;
        headersParser.reset();
        currentHeaderName = null;
        currentHeaderValue = null;
        line.reset();
        for (int i=0; i<messageTypeLine.length; ++i) 
            messageTypeLine[i] = null;
    }
    
    public SipMessage getMessage() {
        return message;
    }
}
