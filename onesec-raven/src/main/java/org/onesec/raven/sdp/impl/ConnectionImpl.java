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

import java.net.InetAddress;
import org.onesec.raven.sdp.AddrType;
import org.onesec.raven.sdp.Connection;
import org.onesec.raven.sdp.NetType;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectionImpl implements Connection {
    private final NetType netType;
    private final AddrType addrType;
    private final InetAddress address;
    private final short ttl;

    public ConnectionImpl(NetType netType, AddrType addrType, InetAddress address, short ttl) {
        this.netType = netType;
        this.addrType = addrType;
        this.address = address;
        this.ttl = ttl;
    }    

    @Override
    public NetType getNetType() {
        return netType;
    }

    @Override
    public AddrType getAddrType() {
        return addrType;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public short getTTL() {
        return ttl;
    }
    
}
