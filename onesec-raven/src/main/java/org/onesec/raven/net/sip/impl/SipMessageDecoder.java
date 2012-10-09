/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.net.sip.impl;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import org.onesec.raven.net.sip.SipConstants;
import org.onesec.raven.net.sip.SipMessage;
import org.onesec.raven.net.sip.SipRequest;
import org.raven.RavenUtils;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class SipMessageDecoder implements SipConstants {
    private final byte[] decodeBuffer;
    private final Logger logger;
    private SipMessage message;
    private String contentEncoding = HEAD_ENCODING;
    private boolean decodingContent;
    private int contentLength;
    private StringBuilder content;
    private final LinkedList<Pair> headers = new LinkedList<Pair>();
    private Pair lastHeader;

    public SipMessageDecoder(Logger logger, byte[] decodeBuffer) {
        this.logger = logger;
        this.decodeBuffer = decodeBuffer;
    }

    public SipMessage decode(ByteBuffer buffer) throws Exception {
        if (!decodingContent)
            decodeHeaders(buffer);
        if (decodingContent && contentLength>=0)
            if (decodeContent(buffer))
                return message;
        return null;
    }

    private void decodeHeaders(ByteBuffer buffer) throws Exception {
        int pos = buffer.position();
        while (buffer.hasRemaining() && !decodingContent) {
            if (buffer.get() == 13 && buffer.hasRemaining() && buffer.get()==10) {
                int len = buffer.position()-pos-2;
                buffer.position(pos);
                buffer.get(decodeBuffer, 0, len);
                decodeHeaderOrMessageTypeFromLine(new String(decodeBuffer, 0, len, HEAD_ENCODING));
                buffer.get(); buffer.get();
                pos = buffer.position();
            }
        }
        if (pos!=buffer.position())
            buffer.position(pos);
    }

    private void decodeHeaderOrMessageTypeFromLine(String line) throws Exception {
        if (line.isEmpty()) {
            if (message==null)
                return;
            decodingContent = true;
            if (lastHeader!=null)
                headers.add(lastHeader);
            for (Pair pair: headers)
                createAndAddHeader(pair);
        } else {
            if (message == null) 
                createMessage(line);
            else 
                decodeHeader(line);
        }
    }
    
    private void decodeHeader(String line) throws Exception {
        if (Character.isWhitespace(line.charAt(0))) {
            if (lastHeader==null)
                throw new Exception("Invalid header format ("+line+")");
            lastHeader.value += " "+toLowerCase(line.trim());
        } else {
            if (lastHeader!=null)
                headers.add(lastHeader);
            line = toLowerCase(line);
            int colonPos = line.indexOf(':');
            if (colonPos==-1)
                throw new Exception("Invalid header format ("+line+")");
            String name = line.substring(0, colonPos).trim();
            String value = line.substring(colonPos+1).trim();
            boolean alreadyExists = false;
            if (!NON_COMBINABLE_HEADERS.contains(name))
                for (Pair header: headers)
                    if (header.name.equals(name)) {
                        header.value += ", "+value;
                        alreadyExists = true;
                    }
            if (!alreadyExists)
                lastHeader = new Pair(name, value);
        }
    }
    
    private String toLowerCase(String str) {
        boolean inQuotas = false;
        StringBuilder newStr = new StringBuilder();
        for (int i=0; i<str.length(); ++i) {
            char c = str.charAt(i);
            if (c=='"') inQuotas = !inQuotas;
            else newStr.append(!inQuotas? Character.toLowerCase(c) : c);
        }
        return newStr.toString();
    }

    private void createMessage(String line) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Handling new SIP message: {}", line);
        String[] toks = RavenUtils.split(line, " ");
        if (toks.length!=3) 
            throw new Exception(String.format(
                    "Error processing start line (%s). Invalid number of elements expected %s but was %s."
                    , line, 3, toks.length));
        if (SIP_VERSION.equals(toks[0]))
            message = new SipResponseImpl(Integer.parseInt(toks[1]), toks[2]);
        else 
            message = new SipRequestImpl(toks[0], toks[1]);
        if (logger.isDebugEnabled())
            logger.debug("Created new {}", message instanceof SipRequest? "REQUEST" : "RESPONSE");
    }

    private void createAndAddHeader(Pair header) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Adding header to message: (%s)");
        message.addHeader(new SipHeaderImpl(header.name, header.value));
    }

    private boolean decodeContent(ByteBuffer buffer) throws Exception {
        int bytesToRead = buffer.remaining()>contentLength? contentLength : buffer.remaining();
        if (bytesToRead>0) {
            buffer.get(decodeBuffer, 0, bytesToRead);
            if (content==null)
                content = new StringBuilder();
            content.append(new String(decodeBuffer, 0, bytesToRead, contentEncoding));
            contentLength -= bytesToRead;
        }
        if (contentLength==0) {
            String contentStr = content==null? null : content.toString();
            if (logger.isTraceEnabled())
                logger.trace("Adding content to message: "+contentStr);
            message.setContent(contentStr);
        }
        return contentLength==0;
    }
    
    private class Pair {
        private final String name;
        private String value;

        public Pair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name+": "+value;
        }
    }
}
