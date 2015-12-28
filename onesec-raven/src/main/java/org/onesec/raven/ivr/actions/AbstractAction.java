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

import org.onesec.raven.ivr.Action;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAction extends AbstractDataProcessorLogic implements Action {
    public final static ActionExecuted ACTION_EXECUTED_then_EXECUTE_NEXT = new ActionExecutedImpl(NextAction.EXECUTE_NEXT);
    public final static ActionExecuted ACTION_EXECUTED_then_STOP = new ActionExecutedImpl(NextAction.STOP);    
    
    private final String name;
    private final ActionExecuted defaultActionExecutedMessage;

    public AbstractAction(String name) {
        this(name, ACTION_EXECUTED_then_EXECUTE_NEXT); 
    }

    public AbstractAction(String name, ActionExecuted defaultActionExecuted) {
        this.name = name;
        this.defaultActionExecutedMessage = defaultActionExecuted;
    }

    @Override
    public Object processData(Object message) throws Exception {
        try {
            if      (message instanceof Execute) {
                final ActionExecuted res = processExecuteMessage((Execute)message);
                if (res!=null) 
                    sendExecuted(res);
            } else if (message == CANCEL)          
                processCancelMessage();
            else 
                return UNHANDLED;
            return VOID;
        } catch (Exception e) {
            sendExecuted(defaultActionExecutedMessage);
            throw e;
        }
    }
    
    protected void sendExecuted(ActionExecuted message) {
        getFacade().sendTo(getContext().getParent(), message);
    }
    
    @Override
    public String getName() {
        return name;
    }

    protected abstract ActionExecuted processExecuteMessage(Execute message) throws Exception; 
    protected abstract void processCancelMessage() throws Exception; 
    
    protected static class ActionExecutedImpl implements ActionExecuted {
        private final NextAction nextAction;

        public ActionExecutedImpl(NextAction nextAction) {
            this.nextAction = nextAction;
        }

        @Override
        public NextAction getNextAction() {
            return nextAction;
        }        

        @Override
        public String toString() {
            return "ACTION_EXECUTED. Next action - "+nextAction;
        }
    }
    
    protected class CancelBehaviour extends Behaviour {
        final ActionExecuted responseMessage;

        public CancelBehaviour(ActionExecuted responseMessage) {
            super("Canceling");
            this.responseMessage = responseMessage;
        }

        @Override public Object processData(Object message) throws Exception {
            if (message==CANCEL) {
                sendExecuted(responseMessage);
                return VOID;
            }
            return UNHANDLED;
        }
    }
}
