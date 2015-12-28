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
package org.onesec.raven.ivr.queue.actions;

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.conv.BindingScope;

/**
 *
 * @author Mikhail Titov
 */
public class ParkOperatorCallAction extends AbstractAction {
    
    public final static String NAME = "Park operator call";
    public final static String PARK_NUMBER_BINDING = "QUEUE_OPERATOR_PARK_DN";

    public ParkOperatorCallAction() {
        super(NAME, ACTION_EXECUTED_then_STOP);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        final IvrEndpointConversation conv = message.getConversation();
        CallQueueRequestController controller = (CallQueueRequestController) 
            conv.getConversationScenarioState().getBindings()
            .get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING);
        if (controller==null) 
            throw new Exception("CallQueueRequestController not found. Is't queue operator conversation?");
        String parkDN = conv.park();
        controller.getConversation().getConversationScenarioState().setBinding(
            PARK_NUMBER_BINDING, parkDN, BindingScope.POINT);
        if (getLogger().isDebugEnabled())
            getLogger().debug("Operator's call parked at ({})", parkDN);
        return ACTION_EXECUTED_then_STOP;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_STOP);
    }

//    @Override
//    protected void doExecute(IvrEndpointConversation conv) throws Exception {
//        CallQueueRequestController controller = (CallQueueRequestController) 
//            conv.getConversationScenarioState().getBindings()
//            .get(CommutationManagerCall.CALL_QUEUE_REQUEST_BINDING);
//        if (controller==null) 
//            throw new Exception("CallQueueRequestController not found. Is't queue operator conversation?");
//        String parkDN = conv.park();
//        controller.getConversation().getConversationScenarioState().setBinding(
//            PARK_NUMBER_BINDING, parkDN, BindingScope.POINT);
//        if (logger.isDebugEnabled())
//            logger.debug("Operator's call parked at ({})", parkDN);
//    }
}
