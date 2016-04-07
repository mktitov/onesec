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

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.AppendableCharSequence;
import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.NetType;
import org.onesec.raven.sdp.Origin;
import org.onesec.raven.sdp.SdpParseException;
import static org.onesec.raven.sdp.impl.ParseUtils.*;
import org.onesec.raven.sip.ByteBufWriteable;
import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public class OriginImpl implements Origin, ByteBufWriteable {
    private final String username;
    private final String sessId;
    private final String sessVersion;
    private final NetType netType;
    private final AddrType addrType;
    private final String unicastAddress;

    public OriginImpl(String username, String sessId, String sessVersion, NetType netType, AddrType addrType, String unicastAddress) {
        this.username = username;
        this.sessId = sessId;
        this.sessVersion = sessVersion;
        this.netType = netType;
        this.addrType = addrType;
        this.unicastAddress = unicastAddress;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getSessId() {
        return sessId;
    }

    @Override
    public String getSessVersion() {
        return sessVersion;
    }

    @Override
    public NetType getNetType() {
        return netType;
    }

    @Override
    public AddrType getAddrType() {
        return addrType;
    }

    @Override
    public String getUnicastAddress() {
        return unicastAddress;
    }

    @Override
    public ByteBuf writeTo(ByteBuf buf) {
        buf.writeByte('o').writeByte('=');
        buf.writeBytes(SipUtils.toBytes(username)).writeByte(' ');
        buf.writeBytes(SipUtils.toBytes(sessId)).writeByte(' ');
        buf.writeBytes(SipUtils.toBytes(sessVersion)).writeByte(' ');
        buf.writeBytes(netType.getBytes()).writeByte(' ');
        buf.writeBytes(addrType.getBytes()).writeByte(' ');
        buf.writeBytes(SipUtils.toBytes(unicastAddress));
        return buf;
    }
    
    public static Origin parse(AppendableCharSequence seq) throws SdpParseException {
        String[] parts = new String[6];
        if (SipUtils.splitExactByWs(parts, seq, 2)) {
            return new OriginImpl(parts[0], parts[1], parts[2], 
                    parseNetType(parts[3]), parseAddrType(parts[4]), parts[5]);            
        } else {
            throw new SdpParseException("Expected 6 tokens in Origin but it is not true: "+seq);                    
        }
    }
    
    private static long parseLong(final String num, final String fieldName) throws SdpParseException {
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw new SdpParseException(String.format("Invalid (%s) field in Origin: %s", fieldName, num));
        }
    }
        
//    public static Origin parse(Byte)
}
