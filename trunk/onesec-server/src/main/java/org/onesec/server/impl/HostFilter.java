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

import java.io.IOException;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.onesec.server.helpers.HostResolver;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class HostFilter extends HandlerWrapper {
    
    private final Set<String> hosts;
    private final Logger log;

    public HostFilter(Server server, Set<String> hosts, Logger log) {
        setServer(server);
        this.hosts = hosts;
        this.log = log;
        
    }

    @Override
    public void handle(
            String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                throws IOException, ServletException 
    {
        if (log.isDebugEnabled())
            log.debug("Check request");
        if (!isAllowRequestFromHost(request)){
            response.setStatus(Response.SC_FORBIDDEN);
            ((Request)request).setHandled(true);
        }else
            super.handle(target, request, response, dispatch);
    }

    private boolean isAllowRequestFromHost(HttpServletRequest request) throws ServletException {
        try {
            if (hosts == null || hosts.size() == 0) {
                return true;
            }
            if (log.isDebugEnabled())
                log.debug("Check request");
            String ip = request.getRemoteAddr();
            String host = HostResolver.getHost(ip);
            if (   hosts.contains(ip) 
                || hosts.contains(host)) 
            {
                return true;
            }
            log.warn(String.format(
                    "attempt to make request from not authorized host. ip (%s), host (%s)", 
                    ip, host));
            return false;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
    
    
    
}
