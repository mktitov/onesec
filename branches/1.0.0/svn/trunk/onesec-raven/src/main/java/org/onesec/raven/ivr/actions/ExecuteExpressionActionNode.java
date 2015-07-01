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

package org.onesec.raven.ivr.actions;

import java.util.Collection;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class ExecuteExpressionActionNode extends BaseNode
{
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object expression;

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens() {
        if (isStarted()) {
            Object res = expression;
        }
        return null;
    }

    public Object getExpression() {
        return expression;
    }

    public void setExpression(Object expression) {
        this.expression = expression;
    }
}
