/*
 *  Copyright 2010 Mikhail Titov.
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

package org.onesec.raven;

import java.net.InetAddress;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.Before;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.onesec.raven.ivr.impl.ConcatDataSource;
import org.onesec.raven.ivr.impl.RtpAddressNode;
import org.onesec.raven.ivr.impl.RtpStreamManagerNode;
import org.onesec.raven.ivr.impl.TestInputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.onesec.raven.JMFHelper.*;


/**
 *
 * @author Mikhail Titov
 */
public class RtpManagerTestCase extends OnesecRavenTestCase
{
    protected final static Logger logger = LoggerFactory.getLogger(RtpManagerTestCase.class);

    protected RtpStreamManagerNode manager;
    protected ExecutorServiceNode executor;
    protected InetAddress localAddress;

    /**
     * Here you cat init you nodes. This method executes after rtp manager node and executor nodes.
     */
    public void initNodes()
    {
    }

    @Before
    public void createRtpManager() throws Exception
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtp manager");
        tree.getRootNode().addAndSaveChildren(manager);
        manager.setLogLevel(LogLevel.DEBUG);
        manager.setMaxStreamCount(10);

        localAddress  = getInterfaceAddress();
        RtpAddressNode address1 = createAddress(localAddress.getHostAddress(), 5004);
        assertTrue(manager.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(20);
        assertTrue(executor.start());

        initNodes();
    }

    protected OperationState sendOverRtp(String filename, Codec codec, final String host, final int port)
            throws Exception
    {
        logger.debug("Sending RTP stream to the ({}:{}) ", host, port);

        final ConcatDataSource audioSource =
                new ConcatDataSource(FileTypeDescriptor.WAVE, executor, codec, 240, 5, 5, manager);
        final OutgoingRtpStream sendStream = manager.getOutgoingRtpStream(manager);
        sendStream.open(host, port, audioSource);
        Thread runner = new Thread(){
            @Override
            public void run() {
                try {
                    InputStreamSource source1 = new TestInputStreamSource("src/test/wav/test.wav");
                    Thread.sleep(100);
                    sendStream.start();
                    audioSource.addSource(source1);
                    Thread.sleep(100);
                    while (audioSource.isPlaying()) 
                        Thread.sleep(100);
                    Thread.sleep(1000);
                    audioSource.close();
                    Thread.sleep(100);
                    sendStream.release();
                } catch (Exception e) {
                    logger.error("Send over rtp process was interuppted", e);
                }
            }
        };
        runner.start();
        return new OperationState(runner);
    }

    protected RtpAddressNode createAddress(String ip, int startingPort)
    {
        RtpAddressNode addr = new RtpAddressNode();
        addr.setName(ip);
        manager.addAndSaveChildren(addr);
        addr.setStartingPort(startingPort);
        assertTrue(addr.start());

        return addr;
    }

    public static class OperationState
    {
        private final Thread executor;

        public OperationState(Thread executor) {
            this.executor = executor;
        }

        public boolean isExecuting() {
            return executor.isAlive();
        }

        public void join() throws InterruptedException
        {
            executor.join();
        }
    }
}
