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

package org.onesec.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecServerTestCase extends RavenCoreTestCase
{
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        super.configureRegistry(builder);
        builder.add(OnesecServerModule.class);
//        System.setProperty(null, null)
    }

    public InetAddress getInterfaceAddress() throws Exception
    {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements())
        {
            NetworkInterface nif = ifs.nextElement();
            if (nif.isUp() && !nif.isLoopback())
            {
                InetAddress addr = nif.getInetAddresses().nextElement();
                System.out.println("\n@@@ Interface address is: "+addr.getHostAddress()+"\n");
                return nif.getInetAddresses().nextElement();
            }
        }

        throw new Exception("Interfaces not found");
    }
}
