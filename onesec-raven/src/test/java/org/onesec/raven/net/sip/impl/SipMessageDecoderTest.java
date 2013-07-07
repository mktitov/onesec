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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.net.sip.SipMessage;
import org.onesec.raven.net.sip.SipRequest;
import org.onesec.raven.net.sip.SipResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SipMessageDecoderTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger("SipMessageDecoder");
    private final static byte[] decodeBuffer = new byte[1024];
    private ByteBuffer buf;
    private String crlf = ""+(char)13+(char)10;
    private SipMessageDecoderImpl decoder;
    
    @Before
    public void prepare() {
        buf = ByteBuffer.allocate(1024);
        decoder = new SipMessageDecoderImpl(logger, decodeBuffer);
        assertNotNull(decoder);
    }
    
    @Test
    public void simpleRequestDecode() throws Exception {
        buf.put(("INVITE sip:request/uri SIP/2.0"+crlf+crlf).getBytes("utf-8"));
        buf.flip();
        SipMessage mess = decoder.decode(buf);
        assertNotNull(mess);
        assertTrue(mess instanceof SipRequest);
    }
    
    @Test
    public void simpleRequestDecodeWithEmptyLineBeforeMessage() throws Exception {
        buf.put((crlf+crlf+"INVITE sip:request/uri SIP/2.0"+crlf+crlf).getBytes("utf-8"));
        buf.flip();
        SipMessage mess = decoder.decode(buf);
        assertNotNull(mess);
        assertTrue(mess instanceof SipRequest);
    }
    
    @Test
    public void simpleResponseDecode() throws Exception {
        buf.put(("SIP/2.0 200 TEST"+crlf+crlf).getBytes("utf-8"));
        buf.flip();
        SipMessage mess = decoder.decode(buf);
        assertNotNull(mess);
        assertTrue(mess instanceof SipResponse);
    }
    
}
