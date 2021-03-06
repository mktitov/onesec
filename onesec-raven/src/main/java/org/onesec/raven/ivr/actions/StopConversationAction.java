/*
 *  Copyright 2009 Mikhail Titov.
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

import org.onesec.raven.ivr.CompletionCode;

/**
 *
 * @author Mikhail Titov
 */
public class StopConversationAction extends AbstractAction {
    public static final String NAME = "Stop conversation";

    public StopConversationAction() {
        super(NAME);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        sendExecuted(ACTION_EXECUTED_then_STOP);
        message.getConversation().stopConversation(CompletionCode.COMPLETED_BY_ENDPOINT);        
        return null;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_STOP);
    }

}
