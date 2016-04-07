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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.onesec.raven.sdp.Connection;
import org.onesec.raven.sdp.MediaDescription;
import org.onesec.raven.sdp.Origin;
import org.onesec.raven.sdp.SdpParseException;
import org.onesec.raven.sip.SipMessageDecoderException;
import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SdpContentParser {
    public final static int MAX_SIZE = 1024;
    //session description
    public final static char SESS_V = 'v';
    public final static char SESS_O = 'o';
    public final static char SESS_S = 's';
    public final static char SESS_I = 'i';
    public final static char SESS_U = 'u';
    public final static char SESS_E = 'e';
    public final static char SESS_P = 'p';
    public final static char SESS_C = 'с';
    public final static char SESS_B = 'b';
    public final static char TIME_T = 't';
    public final static char TIME_R = 'r';
    public final static char SESS_Z = 'z';
    public final static char SESS_K = 'k';
    public final static char SESS_A = 'a';
    public final static char MEDIA_M = 'm';
    public final static char MEDIA_I = 'i';
    public final static char MEDIA_C = 'c';
    public final static char MEDIA_B = 'b';
    public final static char MEDIA_K = 'k';
    public final static char MEDIA_A = 'a';
    public final static char[] SESS_FIRST_EXPECT = new char[]{'v'};
    public final static char[] SESS_V_EXPECT = new char[]{'o'};
    public final static char[] SESS_O_EXPECT = new char[]{'s'};
    public final static char[] SESS_S_EXPECT = new char[]{'i','u','e','p','c','b','t'};
    public final static char[] SESS_I_EXPECT = new char[]{'u','e','p','c','b','t'};
    public final static char[] SESS_U_EXPECT = new char[]{'e','p','c','b','t'};
    public final static char[] SESS_E_EXPECT = new char[]{'p','c','b','t',};
    public final static char[] SESS_P_EXPECT = new char[]{'c','b','t',};
    public final static char[] SESS_C_EXPECT = new char[]{'b','t'};
    public final static char[] SESS_B_EXPECT = new char[]{'b','t'};
    //time description
    public final static char[] TIME_T_EXPECT = new char[]{'r','z','k','a','m'};
    public final static char[] TIME_R_EXPECT = new char[]{'r','z','k','a','m'};
    //
    public final static char[] SESS_Z_EXPECT = new char[]{'k','a', 'm'};
    public final static char[] SESS_K_EXPECT = new char[]{'a', 'm'};
    public final static char[] SESS_A_EXPECT = new char[]{'a', 'm'};
    //media description
    public final static char[] MEDIA_M_EXPECT = new char[]{'i','c','b','k','a','m'};
    public final static char[] MEDIA_I_EXPECT = new char[]{'c','b','k','a','m'};
    public final static char[] MEDIA_C_EXPECT = new char[]{'b','k','a','m'};
    public final static char[] MEDIA_B_EXPECT = new char[]{'b','k','a','m'};
    public final static char[] MEDIA_K_EXPECT = new char[]{'a','m'};
    public final static char[] MEDIA_A_EXPECT = new char[]{'a','m'};
    
    private final AppendableCharSequence seq;
    
    public enum SdpType {
        v(false), o(false), s(false), i, u, e, p, c, b, z, k, a, //session description
        t(false), r, //time description
        m(false); //media description

        private final boolean optional;

        private SdpType() {
            this(true);
        }

        private SdpType(boolean optional) {
            this.optional = optional;
        }        
    }
    
    public final static EnumMap<SdpType, EnumSet<SdpType>> sdpTypeDep;
    static {
        sdpTypeDep = new EnumMap<>(SdpType.class);
        sdpTypeDep.put(SdpType.v, EnumSet.of(SdpType.o));
        sdpTypeDep.put(SdpType.o, EnumSet.of(SdpType.s));
        sdpTypeDep.put(SdpType.s, EnumSet.of(SdpType.i, SdpType.t, SdpType.m));
        sdpTypeDep.put(SdpType.i, EnumSet.of(SdpType.i, SdpType.t, SdpType.m));
        
    }
    

    public SdpContentParser(final AppendableCharSequence seq) {
        seq.reset();
        this.seq = seq;
    }
    
    public SdpContentImpl parse(final ByteBuf buf) throws SdpParseException {
        return null;
    }
    
    private SdpContentImpl parseSessDesc(final ByteBuf buf, final AppendableCharSequence seq) throws SdpParseException {
        char[] expectation = SESS_FIRST_EXPECT;
        char prevType = '-';
        Origin origin;
        String sessionName;
        String sessionInformation;
        List<Connection> connections=null;
        List<MediaDescription> mediaDescriptions = null;
        for (;;) {
            boolean res = readLine(buf, seq, true);
            if (res)
                switch (seq.charAt(0)) {
                    case SESS_V: 
                        checkExpectation(prevType, SESS_V, expectation);
                        if (seq.charAt(2)!='0')
                            throw new SdpParseException(String.format("Invalid SDP version (%s). Expected (0)", seq.charAt(2)));
                        prevType = SESS_V;
                        expectation = SESS_V_EXPECT;
                        break;
                    case SESS_O:
                        checkExpectation(prevType, SESS_O, expectation);
                        origin = OriginImpl.parse(seq);
                        expectation = SESS_O_EXPECT;
                        break;
                    case SESS_S:
                        sessionName = seq.substring(2, seq.length());
                        expectation = SESS_S_EXPECT;
                        break;
                    case SESS_I:
                        sessionInformation = seq.substring(2, seq.length());
                        expectation = SESS_I_EXPECT;
                        break;
                    case SESS_U:
                        expectation = SESS_U_EXPECT;
                        break;
                    case SESS_E: 
                        expectation = SESS_E_EXPECT;
                        break;
                    case SESS_P:
                        expectation = SESS_P_EXPECT;
                        break;
                    case SESS_C:
                        expectation = SESS_C_EXPECT;
                        List<Connection> conns = ConnectionParser.parse(seq);
                        if (connections==null)
                            connections = conns;
                        else
                            connections.addAll(conns);
                        break;
                    case SESS_B:
                        expectation = SESS_B_EXPECT;
                        break;
                    case TIME_T:
                        expectation = TIME_T_EXPECT;
                        break;
                    case TIME_R:
                        expectation = TIME_R_EXPECT;
                        break;
                    case SESS_Z:
                        expectation = SESS_Z_EXPECT;
                        break;
                    case SESS_K:
                        expectation = SESS_K_EXPECT;
                        break;
                    case SESS_A:
                        expectation = SESS_A_EXPECT;
                        break;
                    case MEDIA_M: 
                        mediaDescriptions = new ArrayList<>(1);
                        parseMediaDesc(buf, seq, mediaDescriptions);
                        break;
                    default:
                        throw new SdpParseException("Invalid SDP media type or invalid position: "+seq.charAt(0));                        
                }
            else {
                //проверяем и создаем sess desc
            }
        }        
    }
    
    private void parseMediaDesc(final ByteBuf buf, final AppendableCharSequence seq, List<MediaDescription> mediaDescs) throws SdpParseException {
        boolean initialCycle = true;
        boolean res = true;
        for (;;) {
            if (initialCycle) 
                initialCycle = false;
            else
                res = readLine(buf, seq, true);
            if (res) {
                switch(seq.charAt(0)) {
                    case MEDIA_M: 
                        break;
                    case MEDIA_I:
                        break;
                    case MEDIA_C:
                        break;
                    case MEDIA_B:
                        break;
                    case MEDIA_K:
                        break;
                    case MEDIA_A:
                        break;
                }
            }
        }
    }
    
    private void checkExpectation(final char prevType, final char test, final char[] chars) throws SdpParseException{
        for (char c: chars)
            if (test==c)
                return;
        if (prevType=='-')
            throw new SdpParseException(String.format("Invalid SDP type (%s). The first type in SDP content must be 'v'", test, chars));
        else    
            throw new SdpParseException(String.format("Invalid SDP type (%s). After type (%s) expected one of: (%s)", test, prevType, chars));
    }
    
    private boolean readLine(final ByteBuf buf, final AppendableCharSequence seq, final boolean check) throws SdpParseException {
        try {
            seq.reset();
            SipUtils.readLine(buf, seq, MAX_SIZE, true, null);
            if (seq.length()==0)
                return false;            
            if (check && (seq.length()<2 || seq.charAt(1)!='='))
                throw new SdpParseException("Invalid SDP line format: "+seq);
            return true;
        } catch (SipMessageDecoderException ex) {
            throw new SdpParseException("SDP line size is large than "+MAX_SIZE);
        }
    }
}
