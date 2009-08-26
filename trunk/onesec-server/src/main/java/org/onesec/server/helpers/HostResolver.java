/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.server.helpers;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Mikhail Titov
 */
public class HostResolver {
    private static Map<String, String> cache = new ConcurrentHashMap<String, String>();
    
    public static String getHost(String ip) throws Exception {
        String host=cache.get(ip);
        if (host==null){
            byte[] ipArr=new byte[4];
            int i=0;
            for (String token: ip.split("\\."))
                ipArr[i++]=Byte.parseByte(token);
            InetAddress addr = InetAddress.getByAddress(ipArr);
            host=addr.getHostName();
            cache.put(ip, host);
        }
        return host;
    }
}
