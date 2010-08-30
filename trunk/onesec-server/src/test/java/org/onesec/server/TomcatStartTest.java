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

package org.onesec.server;

import java.io.File;
import java.net.InetAddress;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class TomcatStartTest
{
    private Embedded container;

    @Test
    public void test() throws Exception
    {
        FileUtils.forceMkdir(new File("target/tomcat/webapp"));
        container = new Embedded();
        container.setCatalinaHome("target/tomcat");
//        container.setRealm(new MemoryRealm());

        WebappLoader loader = new WebappLoader(this.getClass().getClassLoader());

        Context rootContext = container.createContext("test", "webapp");
        rootContext.setLoader(loader);
        rootContext.setReloadable(true);
        // create host
        // String appBase = new File(catalinaHome, "webapps").getAbsolutePath();
        Host localHost = container.createHost("localHost", new File("target/tomcat").getAbsolutePath());
        localHost.addChild(rootContext);

        // create engine
        Engine engine = container.createEngine();
        engine.setName("localEngine");
        engine.addChild(localHost);
        engine.setDefaultHost(localHost.getName());
        container.addEngine(engine);

        // create http connector
        Connector httpConnector = container.createConnector((InetAddress) null, 8080, false);
        container.addConnector(httpConnector);

        container.setAwait(true);

        // start server
        container.start();

        Thread.sleep(60000);
    }
}
