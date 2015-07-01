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

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class GenerateLazyCallQueueRequestNode extends AbstractSafeDataPipe {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenario conversationScenario;
    @NotNull @Parameter
    private String abonentNumber;
    @Parameter
    private String callingNumber;
    @NotNull @Parameter(defaultValue="40")
    private Integer inviteTimeout;
    @NotNull @Parameter(defaultValue="5000")
    private Long endpointWaitTimeout;
    @NotNull @Parameter
    private String queueId;
    @NotNull @Parameter
    private Integer priority;

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        try {
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(DATASOURCE_BINDING, dataSource);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            AbonentCommutationManagerImpl req = new AbonentCommutationManagerImpl(abonentNumber
                    , queueId, priority, this, context, endpointPool, conversationScenario
                    , inviteTimeout, endpointWaitTimeout, callingNumber);
            sendDataToConsumers(req, context);
        } finally {
            bindingSupport.reset();
        }
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context
            , BindingSupport bindingSupport) 
    {
    }

    public String getAbonentNumber() {
        return abonentNumber;
    }

    public void setAbonentNumber(String abonentNumber) {
        this.abonentNumber = abonentNumber;
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(String callingNumber) {
        this.callingNumber = callingNumber;
    }

    public IvrConversationScenario getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenario conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public IvrEndpointPool getEndpointPool() {
        return endpointPool;
    }

    public void setEndpointPool(IvrEndpointPool endpointPool) {
        this.endpointPool = endpointPool;
    }

    public Long getEndpointWaitTimeout() {
        return endpointWaitTimeout;
    }

    public void setEndpointWaitTimeout(Long endpointWaitTimeout) {
        this.endpointWaitTimeout = endpointWaitTimeout;
    }

    public Integer getInviteTimeout() {
        return inviteTimeout;
    }

    public void setInviteTimeout(Integer inviteTimeout) {
        this.inviteTimeout = inviteTimeout;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
}
