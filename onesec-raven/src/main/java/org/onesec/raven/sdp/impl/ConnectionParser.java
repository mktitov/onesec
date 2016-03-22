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

import io.netty.util.internal.AppendableCharSequence;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.Connection;
import org.onesec.raven.sdp.NetType;
import org.onesec.raven.sdp.SdpParseException;
import static org.onesec.raven.sdp.impl.ParseUtils.*;
import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectionParser {

    private ConnectionParser() {}
    
    public final static List<Connection> parse(final AppendableCharSequence seq) throws SdpParseException {
        final String[] parts = new String[3];
        if (SipUtils.splitExactByWs(parts, seq, 2)) {
            final NetType netType = parseNetType(parts[0]);
            final AddrType addrType = parseAddrType(parts[1]);
            final int cnt = SipUtils.split(parts[2], '/', 0, -1, true, parts);
            final String addr = parts[0];
            short ttl = -1;
            short addrCnt = 1;
            if (cnt>1) {
                if (addrType==AddrType.IP4)
                    ttl = parseShort(parts[1], "ttl", "Connection");
                else
                    addrCnt = parseShort(parts[1], "numberOfAddresses", "Connection");
            }
            if (cnt==3 && addrType==AddrType.IP4) 
                addrCnt = parseShort(parts[2], "numberOfAddresses", "Connection");
            final List<Connection> connections = new ArrayList<>(addrCnt);
            try {
                InetAddress inetAddr = InetAddress.getByName(addr);
                while (addrCnt-- > 0) {
                    connections.add(new ConnectionImpl(netType, addrType, inetAddr, ttl));
                    if (addrCnt>0) {
                        byte[] bytes = inetAddr.getAddress();
                        bytes[bytes.length-1] = (byte) (bytes[bytes.length-1]+1);
                        inetAddr = InetAddress.getByAddress(bytes);
                    }
                }
                return connections;
            } catch (UnknownHostException ex) {
                throw new SdpParseException("Invalid address in Connection", ex);
            }
        } else {
            throw new SdpParseException("Expected 3 tokens in Connection but it is not true: "+seq);                    
        }
    }
}
