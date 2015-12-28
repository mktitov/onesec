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
package org.onesec.raven.ivr.impl;

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class SipTransparentProxyNodeIT extends OnesecRavenTestCase {
    private SipTransparentProxyNode proxy;
    
    @Before
    public void prepare() throws Exception {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumQueueSize(5);
        executor.setMaximumPoolSize(30);
        assertTrue(executor.start());                
        
        proxy = new SipTransparentProxyNode();
        proxy.setName("sip proxy");
        testsNode.addAndSaveChildren(proxy);
        proxy.setEnableTcp(Boolean.TRUE);
        proxy.setEnableUdp(Boolean.TRUE);
        proxy.setIp(getInterfaceAddress().getHostAddress());
        proxy.setProxyIp("192.168.2.200");
        proxy.setExecutor(executor);
        proxy.setLogLevel(LogLevel.TRACE);
    }
    
//    @Test 
    public void startTest() throws InterruptedException {
        assertTrue(proxy.start());
        Thread.sleep(1000);
        proxy.stop();
    }
    
    @Test
    public void registerTest() throws InterruptedException, Exception {
        assertTrue(proxy.start());
//        SipTerminalImpl term = new SipTerminalImpl(getInterfaceAddress().getHostAddress(), proxy, null, "UDP");
        SipTerminalImpl term = new SipTerminalImpl("1000", proxy, null, "UDP");
        proxy.register(term);
        Thread.sleep(10000);
    }
}