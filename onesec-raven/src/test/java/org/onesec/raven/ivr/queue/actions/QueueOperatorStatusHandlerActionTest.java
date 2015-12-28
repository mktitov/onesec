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
package org.onesec.raven.ivr.queue.actions;

import java.util.List;
import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import static org.onesec.raven.ivr.queue.actions.QueueOperatorStatusHandlerAction.HELLO_PLAYED_BINDING;
import org.onesec.raven.ivr.queue.impl.CallsQueueOperatorNode;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.BindingSupportImpl;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class QueueOperatorStatusHandlerActionTest extends Assert {
    private QueueOperatorStatusHandlerActionNode actionNode;
    
//    @Before
//    public void prepare() {
//        actionNode = new QueueOperatorStatusHandlerActionNode();
//        actionNode.setName("oper status handler action");
//        testsNode.addAndSaveChildren(actionNode);
//        testsNode
//    }
//    
    @Test
    public void nullOperTest(
            @Mocked final QueueOperatorStatusHandlerActionNode actionNode,
            @Mocked final CallsQueuesNode queuesManager,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
//            @Mocked final CallsQueueOperatorNode oper
    ) throws Exception
    {
        new Expectations() {{
            bindings.get(IvrEndpointConversation.NUMBER_BINDING); result = "123";
            queuesManager.getOperatorByPhoneNumber("123"); result = null;
        }};
        
        QueueOperatorStatusHandlerAction action = new QueueOperatorStatusHandlerAction(actionNode);
        assertNull(action.formWords(conv));
    }
    
    @Test
    public void activeOperTest(
            @Mocked final QueueOperatorStatusHandlerActionNode actionNode,
            @Mocked final BindingSupportImpl bindingSupport,
            @Mocked final CallsQueuesNode queuesManager,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallsQueueOperatorNode oper,
            @Mocked final AudioFileNode helloAudio,
            @Mocked final AudioFileNode currentStatusAudio,
            @Mocked final AudioFileNode unavailableStatusAudio,
            @Mocked final AudioFileNode pressOneAudio
            
    ) throws Exception
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindingSupport.putAll(bindings);
            bindings.get(IvrEndpointConversation.NUMBER_BINDING); result = "123";
            actionNode.getCallsQueues(); result = queuesManager;
            queuesManager.getOperatorByPhoneNumber("123"); result = oper;
            bindings.get(IvrEndpointConversation.DTMF_BINDING); result = "1";
            //hello
            bindings.containsKey(HELLO_PLAYED_BINDING); result = false;
            actionNode.getHelloAudio(); result = helloAudio;
            state.setBinding(HELLO_PLAYED_BINDING, true, BindingScope.POINT);
            //current status
            actionNode.getCurrentStatusAudio(); result = currentStatusAudio;
            //
            actionNode.getUnavailableStatusAudio(); result = unavailableStatusAudio;
            actionNode.getPressOneToChangeStatusAudio(); result = pressOneAudio;
            bindingSupport.reset();
        }};
        new StrictExpectations() {{
            oper.getActive(); result = Boolean.TRUE; 
            oper.getActive(); result = Boolean.TRUE; 
            oper.setActive(false);
            oper.getActive(); result = false; 
            oper.getActive(); result = false; 
            
        }};
        QueueOperatorStatusHandlerAction action = new QueueOperatorStatusHandlerAction(actionNode);
        List<Object> words = action.formWords(conv);
        assertNotNull(words);
        assertEquals(1, words.size());
        assertTrue(words.get(0) instanceof List);
        List files = (List)words.get(0);
//        assertEquals(3, files.size());
        assertArrayEquals(new Object[]{helloAudio,currentStatusAudio, unavailableStatusAudio, pressOneAudio}, files.toArray());
    }
    
    @Test
    public void inactiveOperWithoutGreetingTest(
            @Mocked final QueueOperatorStatusHandlerActionNode actionNode,
            @Mocked final BindingSupportImpl bindingSupport,
            @Mocked final CallsQueuesNode queuesManager,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final CallsQueueOperatorNode oper,
            @Mocked final AudioFileNode helloAudio,
            @Mocked final AudioFileNode currentStatusAudio,
            @Mocked final AudioFileNode availableStatusAudio,
            @Mocked final AudioFileNode pressOneAudio
            
    ) throws Exception
    {
        new Expectations() {{
            conv.getConversationScenarioState(); result = state;
            state.getBindings(); result = bindings;
            bindingSupport.putAll(bindings);
            bindings.get(IvrEndpointConversation.NUMBER_BINDING); result = "123";
            actionNode.getCallsQueues(); result = queuesManager;
            queuesManager.getOperatorByPhoneNumber("123"); result = oper;
            bindings.get(IvrEndpointConversation.DTMF_BINDING); result = "1";
            //hello
            bindings.containsKey(HELLO_PLAYED_BINDING); result = true;
//            actionNode.getHelloAudio(); result = helloAudio;
//            state.setBinding(HELLO_PLAYED_BINDING, true, BindingScope.POINT);
            //current status
            actionNode.getCurrentStatusAudio(); result = currentStatusAudio;
            //
            actionNode.getAvailableStatusAudio(); result = availableStatusAudio;
            actionNode.getPressOneToChangeStatusAudio(); result = pressOneAudio;
            bindingSupport.reset();
        }};
        new StrictExpectations() {{
            oper.getActive(); result = false; 
            oper.getActive(); result = false; 
            oper.setActive(true);
            oper.getActive(); result = true; 
            oper.getActive(); result = true; 
            
        }};
        QueueOperatorStatusHandlerAction action = new QueueOperatorStatusHandlerAction(actionNode);
        List<Object> words = action.formWords(conv);
        assertNotNull(words);
        assertEquals(1, words.size());
        assertTrue(words.get(0) instanceof List);
        List files = (List)words.get(0);
//        assertEquals(3, files.size());
        assertArrayEquals(new Object[]{currentStatusAudio, availableStatusAudio, pressOneAudio}, files.toArray());
    }
    
//    private QueueOperatorStatusHandlerActionNode
}
