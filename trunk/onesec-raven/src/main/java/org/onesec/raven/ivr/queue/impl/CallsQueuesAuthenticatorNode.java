/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.queue.impl;

import java.util.Collection;
import java.util.Map;
import org.onesec.raven.ivr.queue.CallsQueuesAuthenticator;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class CallsQueuesAuthenticatorNode extends BaseNode implements DataConsumer, CallsQueuesAuthenticator {
    
    public final static String NAME = "Authenticator";
    public final static String OPERATOR_DESC_FIELD = "operatorDesc";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    private ThreadLocal<AuthInfo> dataStore;

    public CallsQueuesAuthenticatorNode() {
        super(NAME);
    }

    @Override
    protected void initFields() {
        super.initFields();
        dataStore = new ThreadLocal<AuthInfo>();
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (data==null)
            return;
        //parsing data
        Map<String, String> map = converter.convert(Map.class, data, null);
        AuthInfo authInfo = dataStore.get();
        authInfo.authenticated = true;
        dataStore.get().operatorDesc.setOperatorDesc(map.get(OPERATOR_DESC_FIELD));
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported by this consumer");
    }

    public OperatorDesc authenticate(String operatorNumber, String operatorCode) {
        CallsQueuesNode manager = (CallsQueuesNode) getParent();
        CallsQueueOperatorNode operator = manager.getOperatorByPhoneNumber(operatorNumber);
        if (operator==null)
            return null;
        dataStore.set(new AuthInfo(new OperatorDescImpl(operatorCode)));
        dataSource.getDataImmediate(this, new DataContextImpl());
        AuthInfo authInfo = dataStore.get();
        if (authInfo.authenticated) {
            operator.setOperatorDesc(authInfo.operatorDesc.getOperatorDesc());
            operator.setOperatorId(authInfo.operatorDesc.getOperatorId());
            return authInfo.operatorDesc;
        } else
            return null;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private class AuthInfo {
        boolean authenticated = false;
        final OperatorDesc operatorDesc;

        public AuthInfo(OperatorDesc operatorDesc) {
            this.operatorDesc = operatorDesc;
        }
    }
}
