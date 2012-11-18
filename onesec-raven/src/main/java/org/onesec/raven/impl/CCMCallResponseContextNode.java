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

import java.util.Map;
import org.onesec.core.services.Operator;
import org.raven.annotations.NodeClass;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.impl.AbstractNetworkResponseContext;
import org.raven.net.impl.NetworkResponseServiceNode;
import org.raven.net.impl.ParameterNode;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class)
public class CCMCallResponseContextNode
        extends AbstractNetworkResponseContext implements NetworkResponseContext
{
    public static final String NUMA_PARAMETER = "num_a";
    public static final String NUMB_PARAMETER = "num_b";

    @Service
    private static Operator operator;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        createParameters();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        createParameters();
    }

    public Object doGetResponse(String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        String numA = (String)params.get(NUMA_PARAMETER);
        String numB = (String)params.get(NUMB_PARAMETER);

        operator.call(numA, numB);
        
        return "OK";
    }

    private void createParameters()
    {
        createParameter(NUMA_PARAMETER);
        createParameter(NUMB_PARAMETER);
    }

    private void createParameter(String paramName)
    {
        ParameterNode paramNode = (ParameterNode) getParametersNode().getChildren(paramName);
        if (paramNode==null)
        {
            paramNode = new ParameterNode();
            paramNode.setName(paramName);
            getParametersNode().addAndSaveChildren(paramNode);
            paramNode.setParameterType(String.class);
            paramNode.setRequired(Boolean.TRUE);
            paramNode.start();
        }
    }
}
