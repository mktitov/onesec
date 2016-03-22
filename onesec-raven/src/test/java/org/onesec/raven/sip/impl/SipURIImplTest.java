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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipURI;

/**
 *
 * @author Mikhail Titov
 */
public class SipURIImplTest {
    
    @Test
    public void extractSchemeTest() throws Exception {
        SipURIImpl.PosHolder holder = new SipURIImpl.PosHolder();
        assertEquals(SipURI.Scheme.SIP, SipURIImpl.extractScheme("sip:123", holder));
        assertEquals(3, holder.pos);
        assertEquals(SipURI.Scheme.SIPS, SipURIImpl.extractScheme("sips:123", holder));
        assertEquals(4, holder.pos);
    }
    
    @Test(expected = URISyntaxException.class)
    public void extractSchemeTest2() throws Exception {
        SipURIImpl.PosHolder holder = new SipURIImpl.PosHolder();
        SipURIImpl.extractScheme("http:123", holder);
    }
    
    @Test(expected = URISyntaxException.class)
    public void extractSchemeTest3() throws Exception {
        SipURIImpl.PosHolder holder = new SipURIImpl.PosHolder();
        SipURIImpl.extractScheme("sip", holder);
    }
    
    @Test
    public void extractUserInfoTest() throws Exception {
        SipURIImpl.PosHolder holder = new SipURIImpl.PosHolder();
        String[] arr = new String[3];
        
        String strUri = "sip:user:pass:pass@host.name";
        holder.pos = 3;
        int cnt = SipURIImpl.extractUserInfo(strUri, holder, arr);
        assertEquals(2, cnt);
        assertEquals(strUri.indexOf('@'), holder.pos);
        assertArrayEquals(new String[]{"user", "pass:pass", null}, arr);
        
        arr = new String[3];
        strUri = "sip:user@host.name";
        holder.pos = 3;
        cnt = SipURIImpl.extractUserInfo(strUri, holder, arr);
        assertEquals(1, cnt);
        assertEquals(strUri.indexOf('@'), holder.pos);
        assertEquals("user", arr[0]);
    }
    
    @Test
    public void userInfoTest() throws Exception {
        String strUri = "sip:user:pass:pass@host.name";
        SipURI uri = new SipURIImpl(strUri);
        assertEquals("user", uri.getUser());
        assertEquals("pass:pass", uri.getPassword());        
        
        uri = new SipURIImpl("sip:user@host.name");
        assertEquals("user", uri.getUser());
        assertNull(uri.getPassword());
        
        uri = new SipURIImpl("sip:host.name");
        assertNull(uri.getUser());
        assertNull(uri.getPassword());
        
        uri = new SipURIImpl("sip:"+URLEncoder.encode("Ашот", "utf-8")+":"+URLEncoder.encode("тест", "utf-8")+"@host.name");
        assertEquals("Ашот", uri.getUser());
        assertEquals("тест", uri.getPassword());
    }    
    
    @Test(expected = URISyntaxException.class)
    public void userInfoTest2() throws Exception {
        String strUri = "sip:user:@host.name";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test(expected = URISyntaxException.class)
    public void userInfoTest3() throws Exception {
        String strUri = "sip::pass@host.name";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test(expected = URISyntaxException.class)
    public void userInfoTest4() throws Exception {
        String strUri = "sip:@host.name";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test
    public void extractHostTest() throws Exception {
        SipURIImpl.PosHolder holder = new SipURIImpl.PosHolder();
        String[] arr;
        int cnt;
        String strUri;
        
        arr = new String[3];
        strUri = "sip:host.name";
        holder.pos=3;        
        cnt = SipURIImpl.extractHost(strUri, holder, arr);
        assertArrayEquals(new String[]{"host.name", null, null}, arr);
        assertEquals(strUri.length(), holder.pos);
        
        arr = new String[3];
        strUri = "sip:host.name:123";
        holder.pos=3;        
        cnt = SipURIImpl.extractHost(strUri, holder, arr);
        assertArrayEquals(new String[]{"host.name", "123", null}, arr);
        assertEquals(strUri.length(), holder.pos);
        
        arr = new String[3];
        strUri = "sip:host.name;param1=value";
        holder.pos=3;        
        cnt = SipURIImpl.extractHost(strUri, holder, arr);
        assertArrayEquals(new String[]{"host.name", null, null}, arr);
        assertEquals(strUri.indexOf(';'), holder.pos);
        
        arr = new String[3];
        strUri = "sip:host.name:123;param1=value";
        holder.pos=3;        
        cnt = SipURIImpl.extractHost(strUri, holder, arr);
        assertArrayEquals(new String[]{"host.name", "123", null}, arr);
        assertEquals(strUri.indexOf(';'), holder.pos);
        
        arr = new String[3];
        strUri = "sip:host.name?q1=1;&";
        holder.pos=3;        
        cnt = SipURIImpl.extractHost(strUri, holder, arr);
        assertArrayEquals(new String[]{"host.name", null, null}, arr);
        assertEquals(strUri.indexOf('?'), holder.pos);
        
//        strUri = "sip:host.name;param1=value";
//        holder.pos=3;        
//        assertEquals("host.name", SipURIImpl.extractHost(strUri, holder));
//        assertEquals(strUri.indexOf(';'), holder.pos);
//        
//        strUri = "sip:host.name;param1=value?q1=1&";
//        holder.pos=3;        
//        assertEquals("host.name", SipURIImpl.extractHost(strUri, holder));
//        assertEquals(strUri.indexOf(';'), holder.pos);
//        
//        strUri = "sip:host.name?q1=1;&";
//        holder.pos=3;        
//        assertEquals("host.name", SipURIImpl.extractHost(strUri, holder));
//        assertEquals(strUri.indexOf('?'), holder.pos);
    }
    
    @Test 
    public void hostTest() throws Exception {
        String strUri = "sip:host.name";
        SipURI uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(SipURI.Scheme.SIP.getDefaultPort(), uri.getPort());
        
        strUri = "sips:host.name";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(SipURI.Scheme.SIPS.getDefaultPort(), uri.getPort());
        
        strUri = "sips:host.name:123";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(123, uri.getPort());
        
        strUri = "sip:user@host.name";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        
        strUri = "sip:user@host.name;p1=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(SipURI.Scheme.SIP.getDefaultPort(), uri.getPort());
        
        strUri = "sip:user@host.name:123;p1=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(123, uri.getPort());
        
        strUri = "sip:user@host.name;p1=2?q1=12&q2=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        
        strUri = "sip:user@host.name?q1=;12&q2=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        
        strUri = "sip:user@host.name?q1=;12&q2=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        
        strUri = "sip:user@host.name:123?q1=;12&q2=2";
        uri = new SipURIImpl(strUri);
        assertEquals("host.name", uri.getHost());        
        assertEquals(123, uri.getPort());
    }
    
    @Test(expected = URISyntaxException.class)
    public void invalidHostTest() throws Exception {
        String strUri = "sip:";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test(expected = URISyntaxException.class)
    public void invalidHostTest1() throws Exception {
        String strUri = "sip:user@";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test(expected = URISyntaxException.class)
    public void invalidHostTest2() throws Exception {
        String strUri = "sip:;p=1";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test(expected = URISyntaxException.class)
    public void invalidHostTest3() throws Exception {
        String strUri = "sip:?p=1";
        SipURI uri = new SipURIImpl(strUri);
    }
    
    @Test
    public void paramsTest() throws Exception {
        String strUri = "sip:host.name";
        SipURI uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getParams());
        assertEquals(SipURI.Transport.UDP, uri.getTransport());
        assertNull(uri.getMAddr());
        assertNull(uri.getTtl());
        
        strUri = "sip:host.name;";
        uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getParams());
        
        strUri = "sip:host.name;p1=1";
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]{new Object[]{"p1","1"}}, uri.getParams());
        
        strUri = String.format("sip:host.name;p1=1;%s=%s", escape("параметр"), escape("значение"));
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]{
                new Object[]{"p1","1"},
                new Object[]{"параметр","значение"}
            }, uri.getParams());
        
        strUri = "sip:host.name;p1=1?q1=3";
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]{new Object[]{"p1","1"}}, uri.getParams());
        
        strUri = "sip:host.name;transport=tcp";
        uri = new SipURIImpl(strUri);
        assertEquals(SipURI.Transport.TCP, uri.getTransport());
        
        strUri = "sip:host.name;maddr=10.50.1.1";
        uri = new SipURIImpl(strUri);
        assertEquals(InetAddress.getByName("10.50.1.1"), uri.getMAddr());
        
        strUri = "sip:host.name;ttl=100";
        uri = new SipURIImpl(strUri);
        assertEquals(new Integer(100), uri.getTtl());
    }
    
    @Test(expected = URISyntaxException.class)
    public void invalidParamsTest() throws Exception {
        String strUri = "sip:host.name;p";
        SipURI uri = new SipURIImpl(strUri);
        
    }        
    
    @Test(expected = URISyntaxException.class)
    public void invalidTransportTest() throws Exception {
        String strUri = "sip:host.name;transport=upd";
        SipURI uri = new SipURIImpl(strUri);        
    }        
    
    @Test(expected = URISyntaxException.class)
    public void invalidMAddrTest() throws Exception {
        String strUri = "sip:host.name;maddr=1.1.1.1.1";
        SipURI uri = new SipURIImpl(strUri);        
    }        
    
    @Test(expected = URISyntaxException.class)
    public void invalidTtlTest() throws Exception {
        String strUri = "sip:host.name;ttl=-1";
        SipURI uri = new SipURIImpl(strUri);        
    }        
    
    @Test
    public void queryParamsTest() throws Exception {
        String strUri = "sip:host.name";
        SipURI uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getQueryParams());
        
        strUri = "sip:host.name;";
        uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getQueryParams());
        
        strUri = "sip:host.name?";
        uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getQueryParams());
        
        strUri = "sip:host.name;p=1?";
        uri = new SipURIImpl(strUri);
        assertEquals(Collections.EMPTY_MAP, uri.getQueryParams());
        
        strUri = "sip:host.name?q1=1&q2=2";
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]
            {
                new Object[]{"q1", "1"},
                new Object[]{"q2", "2"}
            }, 
            uri.getQueryParams());
        
        strUri = "sip:host.name;p1=p2?q1=1&q2=2";
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]
            {
                new Object[]{"q1", "1"},
                new Object[]{"q2", "2"}
            }, 
            uri.getQueryParams());
        
        strUri = String.format("sip:host.name;p1=p2?q1=1&%s=%s", escape("привет"), escape("мир"));
        uri = new SipURIImpl(strUri);
        assertMapEquals(new Object[][]
            {
                new Object[]{"q1", "1"},
                new Object[]{"привет", "мир"}
            }, 
            uri.getQueryParams());
    }
    
    @Test
    public void toStringTest() throws Exception {        
        assertEquals("sip:user:pass@host;p1=pv1?q1=qv1", new SipURIImpl("sip:user:pass@host;p1=pv1?q1=qv1").toString());        
        assertEquals("sip:user:pass@host?q1=qv1", new SipURIImpl("sip:user:pass@host:5060;transport=udp?q1=qv1").toString());        
        assertEquals("sip:user:pass@host;transport=tcp", new SipURIImpl("sip:user:pass@host:5060;transport=tcp").toString());        
        assertEquals("sips:user:pass@host", new SipURIImpl("sips:user:pass@host:5061;transport=tls").toString());        
        assertEquals("sips:user@host", new SipURIImpl("sips:user@host").toString());        
        assertEquals("sips:user@host:7070", new SipURIImpl("sips:user@host:7070").toString());        
    }
    
    private void assertMapEquals(Object[][] expect, Map testMap) {
        Map expectMap = new HashMap();
        for (Object[] entry: expect) {
            expectMap.put(entry[0], entry[1]);
        }
        assertTrue(expectMap.equals(testMap));
    }
    
    private String escape(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, SipConstants.DEFAULT_ESCAPE_ENCODING);
    }
}
