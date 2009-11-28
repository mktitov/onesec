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

import java.net.URL;
import javax.media.Manager;
import javax.media.Player;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImplTest extends OnesecRavenTestCase
{
    private RtpStreamManagerNode manager;
    private ExecutorServiceNode executor;
    private AudioStream audioStream;

    @Before
    public void prepare() throws Exception
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtp manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setMaxStreamCount(10);

        RtpAddressNode address1 = createAddress("localhost", 3000);
        assertTrue(manager.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

    }

    @Test
    public void test() throws Exception
    {
        InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source2 = new TestInputStreamSource("src/test/wav/test.wav");
        InputStreamSource source3 = new TestInputStreamSource("src/test/wav/test.wav");

        ConcatDataSource audioSource =
                new ConcatDataSource(FileTypeDescriptor.WAVE, executor, 160, manager);

        OutgoingRtpStream sendStream = manager.getOutgoingRtpStream(manager);
        Player player = Manager.createPlayer(new URL("rtp://localhost:"+sendStream.getPort()+"/1"));
        player.start();
        try
        {
            sendStream.open("localhost", remotePort, audioStream);
        }
        finally
        {
            player.stop();
            sendStream.release();
        }
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