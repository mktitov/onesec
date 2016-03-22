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
package org.onesec.raven.sip;

import io.netty.buffer.ByteBuf;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Map;
import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public interface SipURI {
    public enum Transport {
        UDP, TCP, TLS, SCTP;
        
        private final byte[] charBytes;

        private Transport() {
            this.charBytes = SipUtils.toBytes(name().toLowerCase());
        }

        public byte[] getCharBytes() {
            return charBytes;
        }
    }
    
    public enum Scheme {
        SIP(5060, Transport.UDP, SipUtils.toBytes("sip")), SIPS(5061, Transport.TLS, SipUtils.toBytes("sips"));
        
        private final int defaultPort;
        private final Transport defaultTransport;
        private final byte[] charBytes;

        private Scheme(int defaultPort, Transport transport, byte[] charBytes) {
            this.defaultPort = defaultPort;
            this.defaultTransport = transport;
            this.charBytes = charBytes;
        }

        public int getDefaultPort() {
            return defaultPort;
        }        

        public Transport getDefaultTransport() {
            return defaultTransport;
        }        

        public byte[] getCharBytes() {
            return charBytes;
        }
    }
    
    public Scheme getScheme();
    public String getUser();
    public String getPassword();
    public String getHost();
    public int getPort();
    public Transport getTransport();
    public InetAddress getMAddr();
    public Integer getTtl();
    public Map<String, String> getParams();
    public Map<String, String> getQueryParams();
    
    public ByteBuf writeTo(ByteBuf buf) throws UnsupportedEncodingException;
}
