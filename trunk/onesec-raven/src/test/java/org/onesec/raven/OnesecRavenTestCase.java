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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;
import org.onesec.core.services.OnesecCoreModule;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecRavenTestCase extends RavenCoreTestCase
{
    @Override
    protected void configureRegistry(Set<Class> builder)
    {
        super.configureRegistry(builder);
        builder.add(OnesecCoreModule.class);
        builder.add(OnesecRavenModule.class);
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
