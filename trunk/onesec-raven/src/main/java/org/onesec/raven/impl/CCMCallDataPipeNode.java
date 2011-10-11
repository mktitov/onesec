/*
 *  Copyright 2011 Mikhail Titov.
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
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.BindingSupport;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CCMCallDataPipeNode extends AbstractSafeDataPipe
{
    @Service
    private static Operator operator;

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception 
    {
        if (data!=null)
        {
            Map params = converter.convert(Map.class, data, null);
            String num_a = getParam("num_a", params);
            String num_b = getParam("num_b", params);

            operator.call(num_a, num_b);
        }
        DataSourceHelper.sendDataToConsumers(this, data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context
            , BindingSupport bindingSupport)
    {
    }

    private String getParam(String name, Map params) throws Exception
    {
        String param = converter.convert(String.class, params.get(name), null);
        if (param==null)
            throw new Exception(String.format("Parameter (%s) does not exists or null", name));
        return param;
    }

}
