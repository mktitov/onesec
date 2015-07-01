/*
 * Copyright 2014 Mikhail Titov.
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
package org.onesec.raven.rtp;

import java.net.InetAddress;
import java.util.Set;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.net.impl.NettyNioEventLoopGroupNode;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class NettyRtpManagerConfiguratorTest extends OnesecRavenTestCase {
    private NettyRtpManagerConfigurator rtpConf;
    private LoggerHelper logger;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        super.configureRegistry(builder);
    }      
    
    @Before
    public void prepare() {
        NettyNioEventLoopGroupNode eventLoop = new NettyNioEventLoopGroupNode();
        eventLoop.setName("netty event loop");        
        testsNode.addAndSaveChildren(eventLoop);
        assertTrue(eventLoop.start());
        
        rtpConf = new NettyRtpManagerConfigurator();
        rtpConf.setName("Netty rtp configurator");
        testsNode.addAndSaveChildren(rtpConf);
        rtpConf.setEventLoopGroupProvider(eventLoop);
        assertTrue(rtpConf.start());
        
        logger = new LoggerHelper(rtpConf, "Streams. ");
    }
    
    @Test
    public void inOutInOnePortTest() throws Exception {
        int port = 17777;
        RTPManager inRtp = rtpConf.configureInboundManager(InetAddress.getLocalHost(), port, InetAddress.getLocalHost(), 
                SessionAddress.ANY_PORT, logger);
        RTPManager outRtp = rtpConf.configureOutboundManager(InetAddress.getLocalHost(), port, InetAddress.getLocalHost(), 
                1234, logger);
    }
}
