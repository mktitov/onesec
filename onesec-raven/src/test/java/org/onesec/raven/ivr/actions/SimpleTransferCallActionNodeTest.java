/*
 * Copyright 2015 Mikhail Titov.
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

import java.util.Set;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SimpleTransferCallActionNodeTest extends OnesecRavenTestCase {

    @Override
    protected void configureRegistry(Set<Class> builder) {
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        OnesecRavenModule.ENABLE_LOADING_TEMPLATES = false;
        super.configureRegistry(builder); 
    }
    
    @Test
    public void test(
            @Mocked final Action.Execute execMessage,
            @Mocked final IvrEndpointConversation conv) 
        throws Exception 
    {
        SimpleTransferCallActionNode actionNode = new SimpleTransferCallActionNode();
        actionNode.setName("simple transfer");
        testsNode.addAndSaveChildren(actionNode);
        actionNode.setAddress("12345");
        assertTrue(testsNode.start());
        
        SimpleTransferCallAction action = (SimpleTransferCallAction) actionNode.createAction();
        assertNotNull(action);
        assertSame(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, action.processExecuteMessage(execMessage));
        
        new Verifications() {{
            conv.transfer("12345");
        }};
    }
}
