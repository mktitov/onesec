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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.easymock.IArgumentMatcher;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.onesec.core.services.ApplicationHome;
import org.onesec.core.services.Operator;
import org.onesec.server.services.JettyServer;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
@Test(sequential=true)
public class JettyServerImplTest extends Assert{
    
    @Test(timeOut=5000L, enabled=true)
    public void test_startAndStop() throws Exception 
    {
        copyConfigFile_WithNoHostRestrictions();
        
        ApplicationHome home = createAndTrainApplicationHomeMock();
        
        JettyServer server = new JettyServerImpl(
                home, new ArrayList<Handler>(), LoggerFactory.getLogger(JettyServer.class));
        
        assertNotNull(server.getServer());
        assertTrue(server.getServer().isRunning());
        assertEquals(server.getServer().getConnectors()[0].getPort(), 8080);
        
        stopServer(server);
        
        verify(home);
    }
    
    @Test(timeOut=5000L, enabled=true)
    public void test_handler() throws Exception 
    {
        copyConfigFile_WithNoHostRestrictions();
        
        ApplicationHome home = createAndTrainApplicationHomeMock();
        Handler handler = createAndTrainHandlerMock();
        
        JettyServer server = new JettyServerImpl(
                home, Arrays.asList(handler), LoggerFactory.getLogger(JettyServer.class));
        assertTrue(server.getServer().isRunning());
        
        InputStream is = new URL("http://localhost:8080").openStream();
        assertNotNull(is);
        is.close();
        
        stopServer(server);
        
        verify(home, handler);
    }
    
    @Test(timeOut=5000L, enabled=true)
    public void test_hostFilter_restrict() throws Exception 
    {
        Handler handler = createAndTrainHandler2Mock();
        check_hostFilter("onesec_withHostRestriction.cfg", handler, 403, "");
    }
    
    @Test(timeOut=5000L)
    public void test_hostFilter_allow() throws Exception 
    {
        Handler handler = createAndTrainHandlerMock();
        check_hostFilter("onesec_withHostRestriction2.cfg", handler, 200, "");
    }
    
    @Test(timeOut=5000L)
    public void test_operatorHandler() throws Exception
    {
        Operator operator = createMock(Operator.class);
        expect(operator.call("1234", "4321")).andReturn(null);
        replay(operator);
    
        OperatorHandler handler = new OperatorHandler(operator);
        
        check_hostFilter("onesec_withNoHostRestriction.cfg", handler, 400, "/dialer/invalid_path");
        check_hostFilter(
                "onesec_withNoHostRestriction.cfg", handler, 400
                , "/dialer/make-call?num_a=&num_b=4321");
        check_hostFilter(
                "onesec_withNoHostRestriction.cfg", handler, 400
                , "/dialer/make-call?num_a=1234&num_b=");
        check_hostFilter(
                "onesec_withNoHostRestriction.cfg", handler, 200
                , "/dialer/make-call?num_a=1234&num_b=4321");
        
        verify(operator);
    }
    
    private void check_hostFilter(String configFile, Handler handler, int responseCode, String path) 
            throws Exception 
    {
        copyFile(configFile, "onesec.cfg");
        
        ApplicationHome home = createAndTrainApplicationHomeMock();
        
        JettyServer server = new JettyServerImpl(
                home, Arrays.asList(handler), LoggerFactory.getLogger(JettyServer.class));
        assertTrue(server.getServer().isRunning());
        try{
            HttpURLConnection connection = 
                    (HttpURLConnection)new URL("http://localhost:8080"+path).openConnection();
            assertEquals(connection.getResponseCode(), responseCode);
            connection.disconnect();
        }finally{
            stopServer(server);
        }
                
        if (path.length()==0)
            verify(home, handler);
        else
            verify(home);
        
    }

    private void copyConfigFile_WithNoHostRestrictions() throws IOException {
        copyFile("onesec_withNoHostRestriction.cfg", "onesec.cfg");
    }
    
    private void copyFile(String sourceFile, String targetFile) throws IOException {
        String home = System.getProperty("user.home")+"/.onesec/";
        FileUtils.copyFile(new File(home+sourceFile), new File(home+targetFile));
    }

    private ApplicationHome createAndTrainApplicationHomeMock() {
        ApplicationHome home = createMock(ApplicationHome.class);
        expect(home.getHome()).andReturn(new File(System.getProperty("user.home")+"/.onesec"));
        replay(home);
        
        return home;
    }

    private Handler createAndTrainHandlerMock() throws IOException, ServletException, Exception {
        Handler handler = createStrictMock(Handler.class);
        handler.setServer((Server)anyObject());
        expectLastCall().times(2);
        handler.start();
        handler.handle(
                (String)anyObject(), createRequest()
                , createResponse(), eq(1));
        handler.stop();
        
        replay(handler);
        
        return handler;
    }
    
    private Handler createAndTrainHandler2Mock() throws Exception{
        Handler handler = createStrictMock(Handler.class);
        handler.setServer((Server)anyObject());
        expectLastCall().times(2);
        handler.start();
        handler.stop();
        
        replay(handler);
        
        return handler;
    }
    
    private static HttpServletResponse createResponse() {
        reportMatcher(new IArgumentMatcher() {

            public boolean matches(Object obj) {
                try {
                    HttpServletResponse response = (HttpServletResponse) obj;
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println("<h1>Hello</h1>");
                } catch (IOException ex) {
                }
                return true;
            }

            public void appendTo(StringBuffer arg0) {
            }
        });
        return null;
    }
    
    private static HttpServletRequest createRequest() 
    {
        reportMatcher(new IArgumentMatcher() {

            public boolean matches(Object obj) {
                Request request = (Request)obj;
                request.setHandled(true);
                return true;
            }

            public void appendTo(StringBuffer arg0) {
            }
        });
        return null;
    }

    @SuppressWarnings("static-access")
    private void stopServer(JettyServer server) throws InterruptedException {

        server.registryDidShutdown();

        while (true) {
            if (server.getServer().isStopped()) {
                break;
            }
            Thread.currentThread().sleep(10);
        }
    }

}
