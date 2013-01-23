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

package org.onesec.raven.ivr.queue.impl;

import java.util.Collection;
import javax.script.Bindings;
import org.onesec.raven.ivr.queue.BehaviourResult;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOnBusyBehaviourStep;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueueOnBusyBehaviourNode.class)
public class SendDataOnBusyBehaviourStepNode extends BaseNode
        implements DataSource, CallsQueueOnBusyBehaviourStep
{
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Not supported for pull requests.");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public BehaviourResult handleBehaviour(CallsQueue queue, CallQueueRequestController request)
    {
        Object data = request;
        if (useExpression) {
            try {
                bindingSupport.put(BindingNames.REQUEST_BINDING, request);
                bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, request.getContext());
                data = expression;
            } finally {
                bindingSupport.reset();
            }
        }
        DataSourceHelper.sendDataToConsumers(this, data, request.getContext());
        return new BehaviourResultImpl(true, BehaviourResult.StepPolicy.IMMEDIATELY_EXECUTE_NEXT_STEP);
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Object getExpression() {
        return expression;
    }

    public void setExpression(Object expression) {
        this.expression = expression;
    }

    public Boolean getUseExpression() {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression) {
        this.useExpression = useExpression;
    }
}
