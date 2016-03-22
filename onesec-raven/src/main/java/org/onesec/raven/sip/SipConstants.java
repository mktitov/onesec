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

import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public interface SipConstants {
    public final static byte CR = 13;
    public final static byte LF = 10;
    public final static byte[] CRLF = new byte[]{CR, LF};
    public final static int DEFAULT_SIP_PORT = 5060;
    public final static String DEFAULT_ESCAPE_ENCODING = "utf-8";
    public final static String TRANSPORT_PARAM = "transport";
    public final static byte[] TRANSPORT_PARAM_BYTES = SipUtils.toBytes(TRANSPORT_PARAM);
    public final static String MADDR_PARAM = "maddr";
    public final static String TTL_PARAM = "ttl";
    public final static String SIP_2_0 = "SIP/2.0";
    public final static byte[] SIP_2_0_BYTES = SipUtils.toBytes(SIP_2_0);
    public final static String SIP_SCHEME = "sip";
    public final static String SIPS_SCHEME = "sips";    
    //
    public final static String SDP_CONTENT_TYPE = "application/sdp";
}
