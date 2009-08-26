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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.ContextHandler;
import org.onesec.core.services.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class OperatorHandler extends ContextHandler {
    
    private final static String[] NUMBER_PARAMETERS = new String[]{"num_a", "num_b"};
    
    private final static Logger log = LoggerFactory.getLogger(OperatorHandler.class);
    
    private final static String CONTEXT_PATH = "/dialer";
    private final static String TARGET_PATH = CONTEXT_PATH+"/make-call";
    
    private final Operator operator;

    public OperatorHandler(Operator operator) {
        this.operator = operator;
        setContextPath(CONTEXT_PATH);        
    }

    @Override
    public void handle(
            String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                throws IOException, ServletException 
    {
        try {
            ((Request)request).setHandled(true);
            if (!TARGET_PATH.equals(target)) 
                throw new Exception(String.format("Invalid path (%s) in dialer context", target));
            String[] nums = new String[2];
            int i=0;
            for (String param: NUMBER_PARAMETERS){
                nums[i]=request.getParameter(param);
                if (nums[i]==null || nums[i].trim().length()==0)
                    throw new Exception(String.format("Parameter (%s) can not be empty", param));
                ++i;
            }
            log.info(String.format(
                    "Dispatching call information (numA=%s, numB=%s) to operator"
                    , nums[0], nums[1]));
            operator.call(nums[0], nums[1]);
            response.setStatus(Response.SC_OK);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            response.setStatus(Response.SC_BAD_REQUEST);
        }
    }
    

}
