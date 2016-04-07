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

import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.NetType;
import org.onesec.raven.sdp.SdpParseException;

/**
 *
 * @author Mikhail Titov
 */
public class ParseUtils {

    private ParseUtils() { }
    
    public static NetType parseNetType(final String netType) throws SdpParseException {
        if ("IN".equals(netType))
            return NetType.IN;
        else
            throw new SdpParseException("Invalid NetType field in Origin: "+netType);
    }
    
    public static AddrType parseAddrType(final String addrType) throws SdpParseException {
        switch (addrType) {
            case "IP4": return AddrType.IP4;
            case "IP6": return AddrType.IP6;
            default: throw new SdpParseException("Invalid AddrType field in Origin: "+addrType);
        }
    }    
    
    public static long parseLong(final String num, final String fieldName, final String sdpType) throws SdpParseException {
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw new SdpParseException(String.format("Invalid (%s) field in %s: %s", fieldName, sdpType, num));
        }
    }
    
    public static short parseShort(final String num, final String fieldName, final String sdpType) throws SdpParseException {
        try {
            return Short.parseShort(num);
        } catch (NumberFormatException e) {
            throw new SdpParseException(String.format("Invalid (%s) field in %s: %s", fieldName, sdpType, num));
        }
    }    
}
