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
package org.onesec.raven.ivr.queue.actions;

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;

/**
 *
 * @author Mikhail Titov
 */
public class WaitForAbonentCommutationAction extends AbstractAction {
    
    public final static String NAME = "Abonent commutation";
    public final static String COMMUTATION_MANAGER_DISCONNECTED = "COMMUTATION_MANAGER_DISCONNECTED";

    public WaitForAbonentCommutationAction() {
        super(NAME);
    }

    @Override
    public Object processData(Object message) throws Exception {
        if (message==COMMUTATION_MANAGER_DISCONNECTED) {
            sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
            return VOID;
        } else
            return super.processData(message); 
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        final IvrEndpointConversation conversation = message.getConversation();
        Bindings bindings = conversation.getConversationScenarioState().getBindings();
        AbonentCommutationManager commutationManager = (AbonentCommutationManager) bindings.get(
                AbonentCommutationManager.ABONENT_COMMUTATION_MANAGER_BINDING);
        if (commutationManager==null) {
            if (getLogger().isErrorEnabled())
                getLogger().error("Not found abonent commutation manager binding");
            return ACTION_EXECUTED_then_EXECUTE_NEXT;
        } else {
            commutationManager.abonentReadyToCommutate(conversation);
            commutationManager.addRequestListener(new CommutationManagerListener());
            return null;
        }
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }


//    @Override
//    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
//        Bindings bindings = conversation.getConversationScenarioState().getBindings();
//        AbonentCommutationManager commutationManager = (AbonentCommutationManager) bindings.get(
//                AbonentCommutationManager.ABONENT_COMMUTATION_MANAGER_BINDING);
//        if (commutationManager==null)
//            throw new Exception("Not found abonent commutation manager binding");
//        commutationManager.abonentReadyToCommutate(conversation);
//        while (!hasCancelRequest() && commutationManager.isCommutationValid()) 
//            TimeUnit.MILLISECONDS.sleep(10);
//    }

    public boolean isFlowControlAction() {
        return false;
    }
    
    private class CommutationManagerListener implements CallQueueRequestListener {

        @Override public void requestCanceled(String cause) { }
        @Override public void conversationAssigned(IvrEndpointConversation conversation) { }
        @Override public void commutated() { }

        @Override
        public void disconnected() {
            getFacade().send(COMMUTATION_MANAGER_DISCONNECTED);
        }
    }
}
