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

package org.onesec.raven.impl;

import org.onesec.core.provider.ProviderConfiguration;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=ProvidersNode.class)
public class ProviderNode extends BaseNode implements ProviderConfiguration
{
    @Parameter @NotNull
    private Integer fromNumber;

    @Parameter @NotNull
    private Integer toNumber;

    @Parameter @NotNull
    private String user;

    @Parameter @NotNull
    private String password;
    
    @Parameter @NotNull
    private String host;

    public void setFromNumber(Integer fromNumber) {
        this.fromNumber = fromNumber;
    }
    
    public Integer getFromNumber() {
        return fromNumber;
    }

    public void setToNumber(Integer toNumber) {
        this.toNumber = toNumber;
    }

    public Integer getToNumber() {
        return toNumber;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }
}
