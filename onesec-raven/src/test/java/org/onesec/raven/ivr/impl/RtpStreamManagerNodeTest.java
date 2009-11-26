/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.RtpStream;

/**
 *
 * @author Mikhail Titov
 */
public class RtpStreamManagerNodeTest extends OnesecRavenTestCase
{
    private RtpStreamManagerNode manager;
    private RtpAddressNode address1;
    private RtpAddressNode address2;


    @Before
    public void prepare()
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtp manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setMaxStreamCount(10);

        address1 = createAddress("localhost", 3000);
    }

    @Test()
    public void maxStreamCountTest()
    {
        manager.setMaxStreamCount(0);
        assertTrue(manager.start());
        assertNull(manager.getIncomingRtpStream());
        assertNull(manager.getOutgoingRtpStream("localhost", 1234));
    }

    @Test()
    public void oneAddressTest() throws UnknownHostException
    {
        assertTrue(manager.start());
        InetAddress addr = InetAddress.getByName("localhost");
        RtpStream stream1 = createStream(addr, 3000);

        assertEquals(new Integer(1), manager.getStreamsCount());
        assertEquals(1, manager.getStreams().size());
        assertSame(stream1, manager.getStreams().get(addr).get(3000));

        stream1.release();
        assertEquals(new Integer(0), manager.getStreamsCount());
        assertEquals(1, manager.getStreams().size());
        assertEquals(0, manager.getStreams().get(addr).size());

        stream1 = createStream(addr, 3000);
        RtpStream stream2 = createStream(addr, 3002);
        stream2.release();
        stream2 = createStream(addr, 3002);
        stream1.release();
        stream1 = createStream(addr, 3000);
    }

    @Test
    public void twoAddressTest() throws Exception
    {
        InetAddress localhost = InetAddress.getByName("localhost");
        InetAddress second = InetAddress.getByName("192.168.0.20");
        address2 = createAddress("192.168.0.20", 4000);
        assertTrue(manager.start());

        assertEquals(0, manager.getStreams().size());
        assertEquals(new Integer(0), manager.getStreamsCount());
        RtpStream s1, s2, s3, s4;
        s1 = createStream(null, null);
        assertEquals(new Integer(1), manager.getStreamsCount());
        assertEquals(1, manager.getStreams().size());
        s2 = createStream(null, null);
        assertEquals(new Integer(2), manager.getStreamsCount());
        assertEquals(2, manager.getStreams().size());
        assertFalse(s1.getAddress().equals(s2.getAddress()));

        s2.release();
        assertEquals(new Integer(1), manager.getStreamsCount());
        s2 = createStream(null, null);
        assertFalse(s1.getAddress().equals(s2.getAddress()));
        assertEquals(new Integer(2), manager.getStreamsCount());
        assertEquals(1, manager.getStreams().get(localhost).size());
        assertEquals(1, manager.getStreams().get(second).size());

        s3 = createStream(null, null);
        s4 = createStream(null, null);
        assertEquals(new Integer(4), manager.getStreamsCount());
        assertEquals(2, manager.getStreams().get(localhost).size());
        assertEquals(2, manager.getStreams().get(second).size());
    }

    private RtpStream createStream(InetAddress address, Integer port)
    {
        IncomingRtpStream iStream = manager.getIncomingRtpStream();
        assertNotNull(iStream);
        if (address==null)
            assertNotNull(iStream.getAddress());
        else
            assertEquals(address, iStream.getAddress());
        if (port==null)
            assertNotNull(iStream.getPort());
        else
            assertEquals(port.intValue(), iStream.getPort());

        return iStream;
    }

    private RtpAddressNode createAddress(String ip, int startingPort)
    {
        RtpAddressNode addr = new RtpAddressNode();
        addr.setName(ip);
        manager.addAndSaveChildren(addr);
        addr.setStartingPort(startingPort);
        assertTrue(addr.start());

        return addr;
    }
}