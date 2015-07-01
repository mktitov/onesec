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
package org.onesec.raven.ivr.actions;

import org.junit.*;
import static org.junit.Assert.*;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class IfDtmfActionNodeTest extends OnesecRavenTestCase {
    private BindingSourceNode parent;
    private IfDtmfActionNode action;
    
    @Before
    public void prepare() {
        parent = new BindingSourceNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);
        assertTrue(parent.start());
        
        action = new IfDtmfActionNode();
        action.setName("action");
        parent.addAndSaveChildren(action);
        action.setDtmf("1");
        assertTrue(action.start());
        
        BaseNode child = new BaseNode("child");
        action.addAndSaveChildren(child);
        assertTrue(child.start());
    }
    
    @Test
    public void test() {
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "-");
        assertNull(action.getEffectiveChildrens());
        
        parent.addBinding(IvrEndpointConversation.DTMF_BINDING, "1");
        assertNotNull(action.getEffectiveChildrens());
    }
    
}
