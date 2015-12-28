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

/**
 *
 * @author Mikhail Titov
 */
public class UnparkAbonentCallAction extends AbstractAction {
    
    public final static String NAME = "Unpark queue abonent call";

    public UnparkAbonentCallAction() {
        super(NAME, ACTION_EXECUTED_then_STOP);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        final IvrEndpointConversation conv = message.getConversation();
        String parkDN = (String) conv.getConversationScenarioState().getBindings().get(
            ParkOperatorCallAction.PARK_NUMBER_BINDING);
        if (parkDN==null)
            throw new Exception("Can't find parked operator call");
        conv.unpark(parkDN);
        return ACTION_EXECUTED_then_STOP;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_STOP);
    }
}
