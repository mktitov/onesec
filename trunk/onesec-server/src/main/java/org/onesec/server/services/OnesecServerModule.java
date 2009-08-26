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

package org.onesec.server.services;

import java.util.Collection;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.mortbay.jetty.Handler;
import org.onesec.core.services.ApplicationHome;
import org.onesec.core.services.Operator;
import org.onesec.server.impl.JettyServerImpl;
import org.onesec.server.impl.OperatorHandler;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecServerModule {
    
    @EagerLoad
    public JettyServer buildJettyServer(
            ApplicationHome home, Collection<Handler> handlers, Logger log
            , RegistryShutdownHub registryShutdownHub) 
        throws Exception
    {
        JettyServer server = new JettyServerImpl(home, handlers, log);
        registryShutdownHub.addRegistryShutdownListener(server);
        return server;
    }
    
    public void contributeJettyServer(Configuration<Handler> handlers, Operator operator){
        handlers.add(new OperatorHandler(operator));
    }
    
}
