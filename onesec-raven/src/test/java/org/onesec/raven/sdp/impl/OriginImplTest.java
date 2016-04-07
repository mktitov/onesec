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
package org.onesec.raven.sdp.impl;

import io.netty.buffer.Unpooled;
import io.netty.util.internal.AppendableCharSequence;
import java.nio.charset.Charset;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.NetType;
import org.onesec.raven.sdp.Origin;
import org.onesec.raven.sdp.SdpParseException;

/**
 *
 * @author Mikhail Titov
 */
public class OriginImplTest {
    
    @Test
    public void parseTest() throws SdpParseException {
        String s = "jdoe 2890844526 2890842807 IN IP4 10.47.16.5";
        AppendableCharSequence seq = new AppendableCharSequence(512);
        seq.append("o=jdoe 2890844526 2890842807 IN IP4 10.47.16.5");
        Origin origin = OriginImpl.parse(seq);
        assertNotNull(origin);
        assertEquals("jdoe", origin.getUsername());
        assertEquals("2890844526", origin.getSessId());
        assertEquals("2890842807", origin.getSessVersion());
        assertEquals(NetType.IN, origin.getNetType());
        assertEquals(AddrType.IP4, origin.getAddrType());
        assertEquals("10.47.16.5", origin.getUnicastAddress());
    }
    
    @Test
    public void writeToTest() {
        OriginImpl origin = new OriginImpl("user", "123", "321", NetType.IN, AddrType.IP4, "1.2.3.4");
        String str = origin.writeTo(Unpooled.buffer()).toString(Charset.forName("utf-8"));
        assertEquals("o=user 123 321 IN IP4 1.2.3.4", str);
    }
}
