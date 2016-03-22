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
import io.netty.buffer.Unpooled;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipHeaders;
import org.onesec.raven.sip.SipMessageException;
import org.onesec.raven.sip.SipRequest;
import org.onesec.raven.sip.SipURI;

/**
 *
 * @author Mikhail Titov
 */
public class SipRequestImpl extends AbstractSipMessage implements SipRequest, SipConstants {
    private final String method;
    private final Method knownMethod;
    private final SipURI requestUri;
    private final SipHeadersImpl headers = new SipHeadersImpl();

    public SipRequestImpl(String method, String requestUri, String version) throws Exception {
        this.method = method;
        this.knownMethod = parseMethod(method);
        this.requestUri = new SipURIImpl(requestUri);
        if (!SIP_2_0.equalsIgnoreCase(version))
            throw new SipMessageException("Invalid SIP version: ("+version+")");
    }    

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public Method getKnownMethod() {
        return knownMethod;
    }

    @Override
    public SipURI getRequestURI() {
        return requestUri;
    }

    @Override
    public String getVersion() {
        return SIP_2_0;
    }

    @Override
    public SipHeaders headers() {
        return headers;
    }

    @Override
    public ByteBuf writeTo(ByteBuf buf) throws UnsupportedEncodingException {
        if (knownMethod != Method.UNKNOWN)
            buf.writeBytes(knownMethod.getCharBytes());
        else
            buf.writeBytes(SipUtils.toBytes(method));
        buf.writeByte(' ');
        requestUri.writeTo(buf);
        buf.writeByte(' ');
        buf.writeBytes(SIP_2_0_BYTES);
        buf.writeBytes(CRLF);
        headers.writeTo(buf);
        buf.writeBytes(CRLF);
        return buf;
    }

    @Override
    public String toString() {
        try {
            return writeTo(Unpooled.buffer(64)).toString(StandardCharsets.ISO_8859_1);
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    @Override
    public void setContent(Object body) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
