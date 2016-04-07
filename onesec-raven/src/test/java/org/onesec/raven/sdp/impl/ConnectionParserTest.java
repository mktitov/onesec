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
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.Connection;
import org.onesec.raven.sdp.NetType;
import org.onesec.raven.sdp.SdpParseException;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectionParserTest extends Assert {
    private AppendableCharSequence seq = new AppendableCharSequence(1024);
    
    @Test(expected = SdpParseException.class)
    public void invalidConnectionString() throws SdpParseException {
        seq.append("c=");
        ConnectionParser.parse(seq);
    }
    
    @Test(expected = SdpParseException.class)
    public void invalidConnectionString2() throws SdpParseException {
        seq.append("c=IN");
        ConnectionParser.parse(seq);
    }
    
    @Test(expected = SdpParseException.class)
    public void invalidConnectionString3() throws SdpParseException {
        seq.append("c=IN IP4");
        ConnectionParser.parse(seq);
    }
    
    @Test(expected = SdpParseException.class)
    public void invalidConnectionString4() throws SdpParseException {
        seq.append("c=IN IP4 224.2.36.42 1");
        ConnectionParser.parse(seq);
    }
    
    //IN IP4 224.2.36.42/127
    @Test
    public void ip4WithoutTtl() throws SdpParseException, UnknownHostException {
        seq.append("c=IN IP4 224.2.36.42");
        List<Connection> connections = ConnectionParser.parse(seq);
        checkConnections(new ConnectionImpl[]{
                new ConnectionImpl(NetType.IN, AddrType.IP4, InetAddress.getByName("224.2.36.42"), (short)-1)
            }, 
            connections);
    }    
    
    @Test
    public void ip6WithoutTtl() throws SdpParseException, UnknownHostException {
        seq.append("c=IN IP6 FF15::101");
        List<Connection> connections = ConnectionParser.parse(seq);
        checkConnections(new ConnectionImpl[]{
                new ConnectionImpl(NetType.IN, AddrType.IP6, InetAddress.getByName("FF15::101"), (short)-1)
            }, 
            connections);        
    }
    
    @Test
    public void ip4WithTtl() throws SdpParseException, UnknownHostException {
        seq.append("c=IN IP4 224.2.36.42/123");
        List<Connection> connections = ConnectionParser.parse(seq);
        checkConnections(new ConnectionImpl[]{
                new ConnectionImpl(NetType.IN, AddrType.IP4, InetAddress.getByName("224.2.36.42"), (short)123)
            }, 
            connections);
        
    }
    
//    @Test
//    public void ip6WithTtl() throws UnknownHostException, SdpParseException {
//        seq.append("c=IN IP6 FF15::101");
//        List<Connection> connections = ConnectionParser.parse(seq);
//        checkConnections(new ConnectionImpl[]{
//                new ConnectionImpl(NetType.IN, AddrType.IP6, InetAddress.getByName("FF15::101"), (short)321)
//            }, 
//            connections);                
//    }
    
    @Test
    public void multiplyIP4Connections() throws SdpParseException, UnknownHostException {
        seq.append("c=IN IP4 224.2.36.42/123/2");
        List<Connection> connections = ConnectionParser.parse(seq);
        checkConnections(new ConnectionImpl[]{
                new ConnectionImpl(NetType.IN, AddrType.IP4, InetAddress.getByName("224.2.36.42"), (short)123),
                new ConnectionImpl(NetType.IN, AddrType.IP4, InetAddress.getByName("224.2.36.43"), (short)123)
            }, 
            connections);
    }
    
    @Test
    public void multiplyIP6Connections() throws SdpParseException, UnknownHostException {
        seq.append("c=IN IP6 FF15::101/2");
        List<Connection> connections = ConnectionParser.parse(seq);
        checkConnections(new ConnectionImpl[]{
                new ConnectionImpl(NetType.IN, AddrType.IP6, InetAddress.getByName("FF15::101"), (short)-1),
                new ConnectionImpl(NetType.IN, AddrType.IP6, InetAddress.getByName("FF15::102"), (short)-1)
            }, 
            connections);                
    }    
    
    private void checkConnections(ConnectionImpl[] expect, List<Connection> actual) {
        assertEquals(expect.length, actual.size());
        for (int i = 0; i<expect.length; i++) {
            checkConnection(actual.get(i), expect[i]);
        }
    }
    
    private void checkConnection(Connection expect, Connection actual) {
        assertEquals(expect.getNetType(), actual.getNetType());
        assertEquals(expect.getAddrType(), actual.getAddrType());
        assertEquals(expect.getAddress(), actual.getAddress());
        assertEquals(expect.getTTL(), actual.getTTL());
    }
}
