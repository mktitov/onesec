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
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.sip.SipMessageDecoderState;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SipMessageDecoderTest extends Assert {
    private final Logger logger = LoggerFactory.getLogger(SipMessageDecoderTest.class.getName());
    private final LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Decoder", "Decoder. ", logger);
    private final ByteBuf buf = Unpooled.buffer();
    
    @Test
    public void decodeRequestTest() throws Exception {
//        byte[] crlf = SipUtils.toBytes("\r\n");
        String req = "INVITE sip:1001@192.168.1.2:5061 SIP/2.0\r\n" +
            "Via: SIP/2.0/UDP 192.168.1.2:5060;branch=z9hG4bK.A8taTbnGQ;rport\r\n" +
            "From: <sip:linphone@192.168.1.2>;tag=1gYNrJbv-\r\n" +
            "To: sip:1001@192.168.1.2\r\n" +
            "CSeq: 20 INVITE\r\n" +
            "Call-ID: AVrq~wU7aZ\r\n" +
            "Max-Forwards: 70\r\n" +
            "Supported: outbound\r\n" +
            "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, SUBSCRIBE, INFO, UPDATE\r\n" +
            "Content-Type: application/sdp\r\n" +
            "Content-Length: 584\r\n" +
            "Contact: <sip:linphone@192.168.1.2>;+sip.instance=\"<urn:uuid:e23e352a-1c52-475c-93c0-8f70f3097725>\"\r\n" +
            "User-Agent: Linphone/3.9.1 (belle-sip/1.4.2)\r\n" +
            "\r\n" +
            "v=0\r\n" +
            "o=linphone 3390 358 IN IP4 192.168.1.2\r\n" +
            "s=Talk\r\n" +
            "c=IN IP4 192.168.1.2\r\n" +
            "t=0 0\r\n" +
            "a=rtcp-xr:rcvr-rtt=all:10000 stat-summary=loss,dup,jitt,TTL voip-metrics\r\n" +
            "m=audio 7078 RTP/AVP 96 97 98 99 0 8 101 100 102\r\n" +
            "a=rtpmap:96 opus/48000/2\r\n" +
            "a=fmtp:96 useinbandfec=1\r\n" +
            "a=rtpmap:97 SILK/16000\r\n" +
            "a=rtpmap:98 speex/16000\r\n" +
            "a=fmtp:98 vbr=on\r\n" +
            "a=rtpmap:99 speex/8000\r\n" +
            "a=fmtp:99 vbr=on\r\n" +
            "a=rtpmap:101 telephone-event/48000\r\n" +
            "a=rtpmap:100 telephone-event/16000\r\n" +
            "a=rtpmap:102 telephone-event/8000\r\n" +
            "m=video 9078 RTP/AVP 96 97\r\n" +
            "a=rtpmap:96 VP8/90000\r\n" +
            "a=rtpmap:97 H264/90000\r\n" +
            "a=fmtp:97 profile-level-id=42801F";
        buf.writeBytes(SipUtils.toBytes(req));
        SipMessageDecoder decoder = new SipMessageDecoder(loggerHelper);
        List<Object> res = new ArrayList<>();
        SipMessageDecoderState state = decoder.decode(SipMessageDecoderState.INIT, null, buf, res);
        System.out.println("Decoder state: "+state);
        System.out.println("MESSAGE:\r\n"+decoder.getMessage().toString());
    }
}
