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

package org.onesec.raven;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;
import org.onesec.core.services.OnesecCoreModule;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecRavenTestCase extends RavenCoreTestCase
{
    protected LoggerHelper logger;
    
    @Override
    protected void configureRegistry(Set<Class> builder)
    {
        super.configureRegistry(builder);
        builder.add(OnesecCoreModule.class);
        builder.add(OnesecRavenModule.class);
    }

    @Override
    public void initTest() throws Exception {
        super.initTest(); 
        logger = createLogger();
    }

    public InetAddress getInterfaceAddress() throws Exception
    {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements())
        {
            NetworkInterface nif = ifs.nextElement();
            if (nif.isUp() && !nif.isLoopback())
            {
                Enumeration<InetAddress> it = nif.getInetAddresses();
                while (it.hasMoreElements()) {
                    InetAddress addr = it.nextElement();
                    if (addr instanceof Inet4Address) {
                        System.out.println("\n@@@ Interface address is: "+addr.getHostAddress());
                        System.out.println("@@@ Interface hostname is: "+addr.getHostName());
                        System.out.println("@@@ Interface is virtual: "+nif.isVirtual());
                        System.out.println("@@@ Interface is PtP: "+nif.isPointToPoint()+"\n");
                        return addr;
                    }
                } 
            }
        }

        throw new Exception("Interfaces not found");
    }
    
    protected ExecutorServiceNode createExecutor() {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("Executor");
        testsNode.addAndSaveChildren(executor);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        return executor;
    }
    
    protected LoggerHelper createLogger() {
        testsNode.setLogLevel(LogLevel.TRACE);
        return new LoggerHelper(testsNode, null);
    }
}
