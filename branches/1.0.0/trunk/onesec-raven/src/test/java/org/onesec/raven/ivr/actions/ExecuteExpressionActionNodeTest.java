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

import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.tree.impl.BaseNode;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class ExecuteExpressionActionNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test()
    {
        TestNode node = new TestNode();
        node.setName("test");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        ExecuteExpressionActionNode expr = new ExecuteExpressionActionNode();
        expr.setName("expr");
        node.addAndSaveChildren(expr);
        expr.setExpression("test.hello='hello'");
        
        assertTrue(expr.isConditionalNode());
        assertNull(expr.getEffectiveChildrens());
        assertNull(node.map.get("hello"));
        
        
        assertTrue(expr.start());
        assertTrue(expr.isConditionalNode());
        assertNull(expr.getEffectiveChildrens());
        assertEquals("hello", node.map.get("hello"));
    }

    private class TestNode extends BaseNode {
        public final Map map = new HashMap();

        @Override
        public void formExpressionBindings(Bindings bindings) {
            super.formExpressionBindings(bindings);
            bindings.put("test", map);
            bindings.put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
        }
    }
}