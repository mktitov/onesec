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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.onesec.raven.net.sip.SipConstants;
import org.onesec.raven.net.sip.SipHeaderValue;
import org.onesec.raven.net.sip.SipViaHeader;
import org.raven.RavenUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SipViaHeaderImpl extends SipHeaderImpl implements SipViaHeader, SipConstants {
    private final String transportProtocol;
    private final static Set<String> VALID_PROTOCOLS = 
            new HashSet<String>(Arrays.asList("UDP", "TCP", "TLS", "SCTP"));

    public SipViaHeaderImpl(String name, String values) throws Exception {
        super(name, values);
        try {
            SipHeaderValue value = getValue();
            if (value==null)
                throw new Exception("Empty value");
            String[] toks = RavenUtils.split(" ");
            if (toks.length!=2)
                throw new Exception("Invalid elements count in the value");
            if (!toks[0].startsWith(SIP_VERSION))
                throw new Exception("Invalid SIP version");
            String[] protoToks = RavenUtils.split("/");
            if (protoToks.length!=3 || !VALID_PROTOCOLS.contains(protoToks[2].toUpperCase()))
                throw new Exception("Invalid protocol");
            transportProtocol = protoToks[3].toUpperCase();
            
        } catch (Exception e) {
            throw new Exception(String.format("Invalid VIA header (%s: %s). %s", name, values, e.getMessage()));
        }
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public String getClientHost() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getClientPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMaddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTtl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getReceived() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getBranch() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
