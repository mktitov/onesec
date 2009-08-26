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

package org.onesec.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.onesec.core.services.ApplicationHome;
import org.onesec.server.services.JettyServer;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class JettyServerImpl implements JettyServer {
    
    public final static String CONFIG_FILE_NAME = "onesec.cfg";
    
    private Server server;
    private int serverPort;
    private boolean logRequests = false;
    private final Set<String> allowRequestsFromHosts = new HashSet<String>();
    
    private final Logger log;

    public JettyServerImpl(
            ApplicationHome applicationHome, Collection<Handler> handlers, Logger log) 
                throws Exception
    {
        log.info("Starting Jetty HTTP server");
        this.log = log;
        
        readConfig(applicationHome.getHome());
        
        startServer(handlers);
        
        log.info("Jetty HTTP server started at port "+serverPort);
    }

    public Server getServer() {
        return server;
    }
    
    public void registryDidShutdown() {
        try {
            if (server != null) {
                server.stop();
                while(!server.isStopped())
                    TimeUnit.MILLISECONDS.sleep(10);
                log.info("Jetty HTTP server stoped");
            }
        } catch (Exception ex) {
            log.error("Error while stopping Jetty HTTP server", ex);
        }
    }

    private void readConfig(File home) throws FileNotFoundException, IOException {
        Properties conf = new Properties();
        FileInputStream is = 
                new FileInputStream(home.getAbsolutePath()+File.separator+CONFIG_FILE_NAME);
        try{
            conf.load(is);
            String port=getNotNullProperty(conf, "http-server-port");
            try{
                serverPort=Integer.parseInt(port);
            }catch(NumberFormatException e){
                throw new RuntimeException ("Value for property http-server-port must " +
                                     "be a positive integer");
            }
            logRequests = conf.getProperty("log-requests", "false").equals("true");
            String hosts=conf.getProperty("allow-from");
            if (hosts!=null && hosts.trim().length()>0)
                allowRequestsFromHosts.addAll(Arrays.asList(hosts.split("\\s+")));        
        }finally{
            is.close();
        }
    }

    private String getNotNullProperty(Properties conf, String propName) {
        String value=conf.getProperty(propName);
        if (value==null)
            throw new RuntimeException(
                    String.format(
                        "The value for property %s must be setted in the configuration file %s"
                                , propName, CONFIG_FILE_NAME));
        return value;
    }

    private void startServer(Collection<Handler> handlers) throws Exception 
    {
        server = new Server();
        
        Connector connector = new SocketConnector();
        connector.setPort(serverPort);
        server.setConnectors(new Connector[]{connector});
        NCSARequestLog requestLog = null;
        if (logRequests){
            requestLog = new NCSARequestLog("log/jetty-yyyy_mm_dd.request.log");
            requestLog.setRetainDays(30);
            requestLog.setAppend(true);
            requestLog.setExtended(true);
            requestLog.setLogTimeZone("GMT");        
        }
        for (Handler handler: handlers) {
            RequestLogHandler logHandler = null;
            if (logRequests) {
                logHandler = new RequestLogHandler();
                logHandler.setRequestLog(requestLog);
            }
            HostFilter hostFilter = new HostFilter(server, allowRequestsFromHosts, log);
            hostFilter.setServer(server);
            hostFilter.setHandler(handler);
            if (logRequests) {
                logHandler.setHandler(hostFilter);
                logHandler.setServer(server);
                server.addHandler(logHandler);
            }else
                server.addHandler(hostFilter);
        }
        
        server.start();
    }

}
