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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipURI;
import static org.onesec.raven.sip.impl.SipUtils.*;

/**
 *
 * @author Mikhail Titov
 */
public class SipURIImpl implements SipURI, SipConstants {
    private static final String INVALID_PORT_NUMBER_MESS = "Invalid port number";
        
    private final Scheme scheme;
    private final String user;
    private final String password;
    private final String host;
    private final int port;
    private final Map<String, String> params;
    private final Map<String, String> queryParams;
    private final Transport transport;
    private final InetAddress maddr; 
    private final Integer ttl;
//    private params;
//    private headers;

    public SipURIImpl(final String uri) throws Exception {
        if (uri==null)
            throw new NullPointerException("uri");
        final PosHolder holder = new PosHolder();
        scheme = extractScheme(uri, holder);
        //
        int cnt;
        final String[] arr = new String[16];
        //
        cnt = extractUserInfo(uri, holder, arr);
        user = cnt>=1? arr[0] : null;
        password = cnt==2? arr[1]: null;
        //
        cnt = extractHost(uri, holder, arr);
        host = arr[0];
        port = cnt==2? parsePort(arr[1]) : scheme.getDefaultPort();
        //
        params = extractParams(uri, holder, arr, ';', ';');
        transport = configureTransport(uri);
        maddr = configureMAddr(uri);
        ttl = configureTtl(uri);
        //
        queryParams = extractParams(uri, holder, arr, '?', '&');
    }

    @Override
    public Scheme getScheme() {
        return scheme;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public InetAddress getMAddr() {
        return maddr;
    }

    @Override
    public Integer getTtl() {
        return ttl;
    }
    
    @Override
    public ByteBuf writeTo(final ByteBuf buf) throws UnsupportedEncodingException {
        buf.writeBytes(scheme.getCharBytes());
        buf.writeByte(':');
        if (user!=null) {
            buf.writeBytes(toBytes(escape(user)));
            if (password!=null)
                buf.writeByte(':').writeBytes(toBytes(escape(password)));
            buf.writeByte('@');
        }
        buf.writeBytes(toBytes(host));
        if (port!=scheme.getDefaultPort()) 
            buf.writeByte(':').writeBytes(toBytes(Integer.toString(port)));
        if (!params.isEmpty()) {
            for (Map.Entry<String, String> param: params.entrySet()) {
                if (TRANSPORT_PARAM.equals(param.getKey())) {
                    if (transport==scheme.getDefaultTransport())
                        continue;
                    else {
                        buf.writeByte(';').writeBytes(TRANSPORT_PARAM_BYTES);
                        buf.writeByte('=');
                        buf.writeBytes(transport.getCharBytes());
                    }
                } else {
                    buf.writeByte(';');
                    buf.writeBytes(toBytes(escape(param.getKey())));
                    buf.writeByte('=');
                    buf.writeBytes(toBytes(escape(param.getValue())));
                }
            }
        }
        if (!queryParams.isEmpty()) {
            buf.writeByte('?');
            boolean first=true;
            for (Map.Entry<String, String> param: queryParams.entrySet()) {
                if (!first)
                    buf.writeByte('&');
                else
                    first = false;
                buf.writeBytes(toBytes(escape(param.getKey())));
                buf.writeByte('=');
                buf.writeBytes(toBytes(escape(param.getValue())));
            }
        }
        return buf;
    }
    
//    public StringBuilder writeTo(final StringBuilder buf) throws UnsupportedEncodingException {
//        final ByteBuf byteBuf = Unpooled.buffer(64);
//        writeTo(byteBuf);        
//        return buf.append(byteBuf.toString(StandardCharsets.ISO_8859_1));
//    }
    
    @Override
    public String toString() {
        try {
            final ByteBuf byteBuf = Unpooled.buffer(64);
            writeTo(byteBuf);        
            return byteBuf.toString(StandardCharsets.ISO_8859_1);
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }
    
    private Transport configureTransport(final String uri) throws URISyntaxException {
        String _transport = params.get(TRANSPORT_PARAM);
        try {
            return _transport!=null? Transport.valueOf(_transport.toUpperCase()) : scheme.getDefaultTransport();
        } catch (IllegalArgumentException e) {
            throw new URISyntaxException(uri, "Invalid transport: ("+_transport+")");
        }            
    }
    
    private InetAddress configureMAddr(final String uri) throws URISyntaxException {
        String _maddr = params.get(MADDR_PARAM);
        try {
            return _maddr==null? null : InetAddress.getByName(_maddr);
        } catch (UnknownHostException ex) {
            throw new URISyntaxException(uri, "Invalid maddr parameter: ("+_maddr+")");
        }
    }
    
    private Integer configureTtl(final String uri) throws URISyntaxException {
        String _ttl = params.get(TTL_PARAM);
        if (_ttl==null)
            return null;
        try {
            int ttl = Integer.parseInt(_ttl);
            if (ttl<0)
                throw new URISyntaxException(uri, "Invalid ttl param: ("+_ttl+")");
            return ttl;
        } catch (NumberFormatException e) {
            throw new URISyntaxException(uri, "Invalid ttl param: ("+_ttl+")");
        }
    }

    final static int parsePort(String port) throws URISyntaxException {
        try {
            int p = Integer.parseInt(port);
            if (p<0)
                throw new URISyntaxException(port, INVALID_PORT_NUMBER_MESS);
            return p;
        } catch (NumberFormatException e) {
            throw new URISyntaxException(port, INVALID_PORT_NUMBER_MESS);
        }
    }
    
    final static Scheme extractScheme(final String uri, final PosHolder holder) throws URISyntaxException {
        holder.pos = uri.indexOf(':');
        if (holder.pos>=0) {
            final String _scheme = uri.substring(0, holder.pos);
            if (SIP_SCHEME.equalsIgnoreCase(_scheme))
                return Scheme.SIP;
            else if (SIPS_SCHEME.equalsIgnoreCase(_scheme))
                return Scheme.SIPS;
            else
                throw new URISyntaxException(uri, "Invalid scheme");
        } else 
            throw new URISyntaxException(uri, "Invalid scheme");
    }
    
    final static int extractUserInfo(final String uri, final PosHolder holder, final String[] arr) throws Exception {
        int cnt = split(uri, '@', holder.nextPos(), 2, false, arr);
        if (cnt!=2)
            return 0;
        holder.pos+=calcLength(cnt-1, arr)+1;
        cnt = split(arr[0], ':', 0, 2, true, arr);
        for (int i=0; i<cnt; i++) {
            if (arr[i].isEmpty())
                throw new URISyntaxException(uri, "Invalid user name or password");
            else 
                arr[i] = unescape(arr[i]);
        }
        return cnt;
    }
    
    final static int extractHost(final String uri, final PosHolder holder, final String[] arr) throws URISyntaxException {
        holder.pos++;
        int pos1 = uri.indexOf(';', holder.pos);
        int pos2 = uri.indexOf('?', holder.pos);
        int end = uri.length();
        if (pos1>=0 && (pos2==-1 || pos1<pos2)) {
            end = pos1;
        } else if (pos2>=0) {
            end = pos2;
        }
        String hostAndPort = uri.substring(holder.pos, end);
        if (hostAndPort.isEmpty())
            throw new URISyntaxException(uri, "Invalid host name");
        holder.pos = end;
        return split(hostAndPort, ':', 0, 2, false, arr);
    }
    
    final static Map<String, String> extractParams(final String uri, final PosHolder holder, final String[] arr, 
                final char paramsDelim, final char pairDelim) 
            throws Exception 
    {
        if (holder.pos+1>=uri.length() || uri.charAt(holder.pos)!=paramsDelim)
            return Collections.EMPTY_MAP;
        holder.pos++;
        int end;
        if (paramsDelim=='?')
            end = uri.length();
        else { 
            end = uri.indexOf('?', holder.pos);
            if (end==-1)
                end = uri.length();
        }
        int cnt = split(uri.substring(holder.pos, end), pairDelim, 0, -1, false, arr);
        final String[] pair = new String[2];
        int pairCnt;
        Map<String, String> params = new HashMap<>();
        for (int i=0; i<cnt; i++) {
            pairCnt = split(arr[i], '=', 0, 2, false, pair);
            if (pairCnt!=2)
                throw new URISyntaxException(uri, "Invalid parameter syntax: "+arr[i]);
            params.put(unescape(pair[0]), unescape(pair[1]));
        }
        holder.pos = end;
        return Collections.unmodifiableMap(params);
    }
    
    final static int calcLength(final int cnt, final String[] arr) {
        int len=0;
        for (int i=0; i<cnt; ++i) 
            len+=arr[i].length();
        return len;
    }
    
//    final static int split(final String str, final char ch, final int startFrom, final int max, final boolean expandLast, final String[] arr) {
//        if (arr==null || arr.length==0)
//            return 0;
//        final int strlen = str.length();
//        if (strlen==0) {
//            arr[0] = str;
//            return 1;
//        }
//        int start = startFrom;
//        int ind=0;
//        int pos;
//        while ( (pos=str.indexOf(ch, start))>=0 ) {
//            if (ind+1>=arr.length || (max!=-1 && ind+1>=max)) {
//                arr[ind++] = expandLast? str.substring(start) : str.substring(start, pos);
//                return ind;
//            } 
//            arr[ind++] = str.substring(start, pos);
//            start = pos+1;            
//        }
//        arr[ind++] = start==strlen? "" : str.substring(start, strlen);
//        return ind;
//    }
    
    private static String unescape(final String str) throws UnsupportedEncodingException {
        return str.indexOf('%')>=0? URLDecoder.decode(str, DEFAULT_ESCAPE_ENCODING) : str;
    }
    
    private static String escape(final String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, DEFAULT_ESCAPE_ENCODING);        
    }
    
    private static byte[] toBytes(final String str) {
        return str.getBytes(StandardCharsets.ISO_8859_1);
    }

    static final class PosHolder {
        public int pos;
        public final int nextPos() {
            return pos+1;
        }
    }
}
