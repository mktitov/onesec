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

package org.onesec.raven.ivr.queue.actions;

import org.raven.tree.Node;
import java.util.concurrent.TimeUnit;
import javax.script.Bindings;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.conv.ConversationScenarioState;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class WaitForCallCommutationActionTest extends Assert
{
    private static String executedKey = "executed_org.onesec.raven.ivr.queue.actions.WaitForCallCommutationAction_1";
    
//    @Test
    public void errorTest()
    {
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        Node owner = createMock(Node.class);
        
//        expect(owner.getId()).andReturn(1).anyTimes();
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(null);
//        expect(bindings.containsKey(executedKey)).andReturn(false);
//        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);

        replay(conv, state, bindings, owner);

        WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
        try {
            action.doExecute(conv);
            fail();
        } catch (Exception ex) {
        }

        verify(conv, state, bindings, owner);
    }

    @Test
    public void normalTest() throws Exception {
        final IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        CommutationManagerCall manager = createMock(CommutationManagerCall.class);
        Node owner = createMock(Node.class);
        final WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
        
        expect(owner.getId()).andReturn(1).anyTimes();
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(manager);
        manager.operatorReadyToCommutate(conv);
        manager.addListener(action);
        expect(manager.isCommutationValid()).andReturn(Boolean.TRUE).anyTimes();
        expect(bindings.size()).andReturn(1).anyTimes();
        expect(bindings.containsKey(executedKey)).andReturn(false);
        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);
        
        replay(conv, state, bindings, manager, owner);


        Thread actionThread = new Thread(){
            @Override public void run() {
                try {
                    action.doExecute(conv);
                } catch (Exception ex) {
                    fail();
                }
            }
        };

        actionThread.start();
//        TimeUnit.MILLISECONDS.sleep(500);
        TimeUnit.MILLISECONDS.sleep(150000);
        assertTrue(actionThread.isAlive());
        action.cancel();
        TimeUnit.MILLISECONDS.sleep(150);
        assertFalse(actionThread.isAlive());

        verify(conv, state, bindings, manager, owner);
    }

//    @Test
    public void normalTest2() throws Exception {
        final IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        CommutationManagerCall manager = createMock(CommutationManagerCall.class);
        Node owner = createMock(Node.class);
        final WaitForCallCommutationAction action = new WaitForCallCommutationAction(owner);
        
        expect(owner.getId()).andReturn(1).anyTimes();
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING)).andReturn(manager);
        manager.addListener(action);
        manager.operatorReadyToCommutate(conv);
        expect(manager.isCommutationValid()).andReturn(Boolean.TRUE).times(5).andReturn(Boolean.FALSE);
        expect(bindings.containsKey(executedKey)).andReturn(false);
        expect(bindings.put(executedKey, Boolean.TRUE)).andReturn(null);

        replay(conv, state, bindings, manager, owner);


        Thread actionThread = new Thread(){
            @Override
            public void run() {
                try {
                    action.doExecute(conv);
                } catch (Exception ex) {
                    fail();
                }
            }
        };

        actionThread.start();
        TimeUnit.MILLISECONDS.sleep(400);
        assertTrue(actionThread.isAlive());
        TimeUnit.MILLISECONDS.sleep(200);
        assertFalse(actionThread.isAlive());

        verify(conv, state, bindings, manager, owner);
    }
}