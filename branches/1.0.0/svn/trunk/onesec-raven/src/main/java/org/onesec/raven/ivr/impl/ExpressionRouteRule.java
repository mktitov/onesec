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

import java.util.Arrays;
import java.util.Collection;
import javax.script.Bindings;
import org.onesec.raven.ivr.CallRouteRule;
import org.onesec.raven.ivr.CallRouteRuleProvider;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CiscoCallsRouterNode.class)
public class ExpressionRouteRule extends BaseNode implements CallRouteRuleProvider {
    public final static String DESTINATIONS_ATTR = "destinations";
    public final static String ACCEPT_ATTR = "accept";
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean accept;
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Collection destinations;
    
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

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    public CallRouteRule getCallRouteRule() {
        return new RouteRule((CiscoCallsRouterNode)getParent());
    }

    public Boolean getAccept() {
        return accept;
    }

    public void setAccept(Boolean accept) {
        this.accept = accept;
    }

    public Collection getDestinations() {
        return destinations;
    }

    public void setDestinations(Collection destinations) {
        this.destinations = destinations;
    }
    
    private class RouteRule implements CallRouteRule {
        private final String ruleKey;
        private final CiscoCallsRouterNode owner;
        private final String[] dests;

        public RouteRule(CiscoCallsRouterNode owner) {
            this.ruleKey = owner.formRuleKey(ExpressionRouteRule.this);
            this.owner = owner;
            dests = createDestinations();
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
            if (!isStarted())
                return null;
            return dests;
        }

        public String[] getCallingNumbers() {
            return null;
        }

        public String getRuleKey() {
            return ruleKey;
        }

        public int getPriority() {
            return owner.getRouteRulePriority(ExpressionRouteRule.this);
        }

        @Override
        public String toString() {
            return "expressionRouteRule: name="+getName()+"; accept = "+getAttr(ACCEPT_ATTR).getRawValue()
                    + "; destinations="+Arrays.toString(dests);
        }

        private String[] createDestinations() {
            bindingSupport.enableScriptExecution();
            try {
                Collection destsCol = destinations;
                if (destsCol==null)
                    return null;
                else {
                    String[] res = new String[destsCol.size()];
                    int i=0;
                    for (Object dest: destinations)
                        res[i++] = dest.toString();
                    return res;
                }
            } finally {
                bindingSupport.reset();
            }
        }
    }
}
