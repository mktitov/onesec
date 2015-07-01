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

package org.onesec.server.service.impl;

import org.apache.catalina.Context;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Embedded;
import org.onesec.core.services.ApplicationHome;
import org.onesec.server.service.TomcatService;
import org.raven.conf.Configurator;

/**
 *
 * @author Mikhail Titov
 */
public class TomcatServiceImpl implements TomcatService
{
    private final Configurator configurator;
    private final ApplicationHome applicationHome;

    private Embedded container;

    public TomcatServiceImpl(Configurator configurator, ApplicationHome applicationHome)
    {
        this.configurator = configurator;
        this.applicationHome = applicationHome;
    }

    public void start()
    {
        container = new Embedded();
        container.setCatalinaHome("target/tomcat");
        container.setRealm(new MemoryRealm());

        WebappLoader loader = new WebappLoader(this.getClass().getClassLoader());

        Context rootContext = container.createContext("", "target/tomcat/webapp");
        rootContext.setLoader(loader);
        rootContext.setReloadable(false);
    }
}
