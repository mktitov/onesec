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

package org.onesec.core.provider.impl;

import org.onesec.core.provider.ProviderConfiguration;

/**
 *
 * @author Mikhail Titov
 */
public class ProviderConfigurationImpl implements ProviderConfiguration 
{    
    private int id;
    private String name;
    private Integer idObj;
    private int fromNumber;
    private int toNumber;
    private String user;
    private String password;
    private String host;
        
    public ProviderConfigurationImpl(
            int id, String name, int fromNumber, int toNumber, String user
            , String password, String host)
    {
        this.id = id;
        this.name = name;
        this.idObj = id;
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.user = user;
        this.password = password;
        this.host = host;
    }
    
    public String getName() {
        return name;
    }
    
    public int getId() {
        return id;
    }

    public Integer getFromNumber() {
        return fromNumber;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public Integer getToNumber() {
        return toNumber;
    }

    public String getUser() {
        return user;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProviderConfiguration) {
            ProviderConfiguration conf = (ProviderConfiguration)obj;
            return id==conf.getId() && fromNumber==conf.getFromNumber() 
                    && toNumber==conf.getToNumber() && user.equals(conf.getUser()) 
                    && password.equals(conf.getPassword()) && host.equals(conf.getHost());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return idObj.hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                "Provider config: id (%d); name (%s); numbers block [%d, %d]; user (%s); host (%s). "
                , id, name, fromNumber, toNumber, user, host);
    }
}
