/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import javax.script.Bindings;
import org.onesec.raven.ivr.CallRouteRule;
import org.onesec.raven.ivr.CallRouteRuleProvider;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.ConversationScenario;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CiscoCallsRouterNode.class)
public class EndpointPoolRouteRule extends BaseNode implements CallRouteRuleProvider {
    
    public final static String ACCEPT_ATTR = "accept";
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean accept;
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrEndpointPool endpointPool;
    
    @NotNull @Parameter(defaultValue="1000")
    private Long endpointReserveTimeout;
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenario conversationScenario;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    public CallRouteRule getCallRouteRule() {
        return new RouteRule((CiscoCallsRouterNode)getParent(), endpointPool, endpointReserveTimeout, 
                conversationScenario);
    }

    public Boolean getAccept() {
        return accept;
    }

    public void setAccept(Boolean accept) {
        this.accept = accept;
    }

    public IvrEndpointPool getEndpointPool() {
        return endpointPool;
    }

    public void setEndpointPool(IvrEndpointPool endpointPool) {
        this.endpointPool = endpointPool;
    }

    public Long getEndpointReserveTimeout() {
        return endpointReserveTimeout;
    }

    public void setEndpointReserveTimeout(Long endpointReserveTimeout) {
        this.endpointReserveTimeout = endpointReserveTimeout;
    }

    public IvrConversationScenario getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenario conversationScenario) {
        this.conversationScenario = conversationScenario;
    }
    
    private class RouteRule implements CallRouteRule {
        private final String ruleKey;
        private final CiscoCallsRouterNode owner;
        private final IvrEndpointPool pool;
        private final long reserveTimeout;
        private final ConversationScenario conversationScenario;

        public RouteRule(CiscoCallsRouterNode owner, IvrEndpointPool pool, long reserveTimeout, 
                ConversationScenario conversationScenario) 
        {
            this.ruleKey = owner.formRuleKey(EndpointPoolRouteRule.this);
            this.pool = pool;
            this.owner = owner;
            this.reserveTimeout = reserveTimeout;
            this.conversationScenario = conversationScenario;
        }

        public boolean accept(String callingNumber) {
            if (!isStarted())
                return false;
            try {
                bindingSupport.put("callingNumber", callingNumber);
                Boolean res = accept;
                return res==null? false : res;
            } finally {
                bindingSupport.reset();
            }
        }

        public boolean isPermanent() {
            return true;
        }

        public String[] getDestinations() {
            IvrEndpoint endpoint = pool.reserveEndpoint(new ReserveEndpointRequestImpl(
                    reserveTimeout, conversationScenario));
            return endpoint==null? null : new String[]{endpoint.getAddress()};
        }

        public String[] getCallingNumbers() {
            return null;
        }

        public String getRuleKey() {
            return ruleKey;
        }

        public int getPriority() {
            return owner.getRouteRulePriority(EndpointPoolRouteRule.this);
        }
        
        @Override
        public String toString() {
            return "expressionRouteRule: name="+getName()+"; accept = "+getAttr(ACCEPT_ATTR).getRawValue()
                    + "; destination from ("+pool.getPath()+")";
        }
    }
}
