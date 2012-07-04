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
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.queue.OperatorRegistrator;
import org.onesec.raven.ivr.queue.OperatorDesc;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class OperatorRegistratorNode extends BaseNode implements DataConsumer, OperatorRegistrator {
    
    public final static String NAME = "Operator registrator";
    public final static String OPERATOR_DESC_FIELD = "operatorDesc";
    public final static String OPERATOR_NUMBER_BINDING = "operatorNumber";
    public final static String OPERATOR_CODE_BINDING = "operatorCode";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    private ThreadLocal<AuthInfo> dataStore;
    private BindingSupport bindingSupport;

    public OperatorRegistratorNode() {
        super(NAME);
    }

    @Override
    protected void initFields() {
        super.initFields();
        dataStore = new ThreadLocal<AuthInfo>();
        bindingSupport = new BindingSupportImpl();
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (data==null)
            return;
        //parsing data
        Map<String, String> map = converter.convert(Map.class, data, null);
        AuthInfo authInfo = dataStore.get();
        authInfo.authenticated = true;
        authInfo.operatorDesc.setDesc(map.get(OPERATOR_DESC_FIELD));
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported by this consumer");
    }

    public OperatorDesc getCurrentOperator(String operatorNumber) {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        CallsQueuesNode manager = getCallsQueues();
        CallsQueueOperatorNode operator = manager.getOperatorByPhoneNumber(operatorNumber);
        if (operator==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Not found operator with number ({})", operatorNumber);
            return null;
        }
        if (operator.getPersonId()!=null)
            return new OperatorDescImpl(operator.getPersonId(), operator.getPersonDesc());
        else
            return null;
    }

    public OperatorDesc register(String operatorNumber, String operatorCode) {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        CallsQueuesNode manager = getCallsQueues();
        CallsQueueOperatorNode operator = manager.getOperatorByPhoneNumber(operatorNumber);
        if (operator==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Not found operator with number ({})", operatorNumber);
            return null;
        }
        try {
            dataStore.set(new AuthInfo(operator, new OperatorDescImpl(operatorCode)));
            Bindings bindings = new SimpleBindings();
            bindings.put(OPERATOR_CODE_BINDING, operatorCode);
            bindings.put(OPERATOR_NUMBER_BINDING, operatorNumber);
            DataContextImpl dataContext = new DataContextImpl();
            dataContext.getParameters().putAll(bindings);
            bindingSupport.putAll(bindings);
            dataSource.getDataImmediate(this, dataContext);
            AuthInfo authInfo = dataStore.get();
            if (authInfo.authenticated) {
                operator.setPersonDesc(authInfo.operatorDesc.getDesc());
                operator.setPersonId(authInfo.operatorDesc.getId());
                for (CallsQueueOperatorNode oper: NodeUtils.getChildsOfType(getCallsQueues()
                        .getOperatorsNode(), CallsQueueOperatorNode.class, false)
                ) {
                    if (oper!=operator && operatorCode.equals(oper.getOperatorId())) {
                        oper.setPersonId(null);
                        oper.setPersonDesc(null);
                    }
                }
                return authInfo.operatorDesc;
            } else
                return null;
        } finally {
            bindingSupport.reset();
        }
    }
    
    public void unregister(String operatorNumber) {
        if (!Status.STARTED.equals(getStatus()))
            return;
        CallsQueuesNode manager = (CallsQueuesNode) getParent();
        CallsQueueOperatorNode operator = manager.getOperatorByPhoneNumber(operatorNumber);
        if (operator==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Not found operator with number ({})", operatorNumber);
            return;
        }
        operator.setPersonDesc(null);
        operator.setPersonId(null);
    }
    
    private CallsQueuesNode getCallsQueues() {
        return (CallsQueuesNode) getParent();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private class AuthInfo {
        boolean authenticated = false;
        final CallsQueueOperatorNode operatorNode;
        final OperatorDesc operatorDesc;

        public AuthInfo(CallsQueueOperatorNode operatorNode, OperatorDesc operatorDesc) {
            this.operatorNode = operatorNode;
            this.operatorDesc = operatorDesc;
        }
    }
}
