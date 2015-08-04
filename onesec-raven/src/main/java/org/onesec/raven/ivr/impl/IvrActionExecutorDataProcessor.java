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
package org.onesec.raven.ivr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.onesec.raven.ivr.ActionStopListener;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.onesec.raven.ivr.impl.IvrActionExecutorFacade.*;
import static org.onesec.raven.ivr.impl.IvrActionExecutorFacade.*;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.Behaviour;
import org.raven.ds.TimeoutMessageSelector;
/**
 *
 * @author Mikhail Titov
 */
public class IvrActionExecutorDataProcessor extends AbstractDataProcessorLogic {
    private final static String EXECUTE_NEXT_ACTION = "EXECUTE_NEXT_ACTION";
    private final IvrEndpointConversation conversation;
    private final Set<Character> deferredDtmfs = new HashSet<>();
    private final List<Character> collectedDtmfs = new ArrayList<>();
    
    private Iterator<IvrAction> actionsForExecute;
    private IvrAction currentAction;
    private ActionStopListener actionStopListener = new StopListener();

    public IvrActionExecutorDataProcessor(IvrEndpointConversation conversation) {
        this.conversation = conversation;
    }

    @Override
    public void postInit() {
        getContext().become(initialized, true);
    }
    
    private final DataProcessor processDtmfRequests = new DataProcessor() {
        @Override public Object processData(Object message) throws Exception {
            if      (message instanceof HasDtmfProcessingPoint)     return hasDtmfProcessPoint((HasDtmfProcessingPoint)message); 
            else if (message==GET_COLLECTED_DTMFS)                  return getCollectedDtmfs();
            else return UNHANDLED;
        }
    };
    
    private final TimeoutMessageSelector actionExecutedSelector = new TimeoutMessageSelector() {
        @Override public boolean resetTimeout(Object message) {
            return message instanceof ActionExecuted;
        }
    };
    
    //Behaviours
    private final Behaviour initialized = new Behaviour("Initialized") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof ExecuteActions) {
                Collection<IvrAction> actions = ((ExecuteActions)message).actions;
                actionsForExecute = actions.iterator();
                getFacade().send(EXECUTE_NEXT_ACTION);
                deferredDtmfs.clear();
                collectedDtmfs.clear();
                currentAction = null;
                for (IvrAction action: actions)
                    if (action instanceof DtmfProcessPointAction) {
                        for (char c: ((DtmfProcessPointAction)action).getDtmfs().toCharArray())
                            deferredDtmfs.add(c);
                    }
                become(executing);
                return VOID;
            } else if (message==CANCEL_ACTIONS_EXECUTION)
                return true;
            else 
                return UNHANDLED;
        }
    }.andThen(processDtmfRequests);
    
    private final Behaviour executing = new Behaviour("Executing") {
        @Override public Object processData(Object message) throws Exception {
            if (message==EXECUTE_NEXT_ACTION) {
                if (actionsForExecute.hasNext())
                    executeAction(actionsForExecute.next());
                else {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("No more action to execute");
                    become(initialized);
                }
                return VOID;
            } else if (message==CANCEL_ACTIONS_EXECUTION) {
                if (currentAction!=null)
                    currentAction.cancel();                
                actionsForExecute = Collections.emptyIterator();
                getFacade().setReceiveTimeout(CANCEL_TIMEOUT, actionExecutedSelector);
                become(canceling);
                return getContext().stash();
            } else if (message instanceof ActionExecuted) {
                if (currentAction==((ActionExecuted)message).action) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("<<< ({})", currentAction.getName());
                    if (currentAction.isFlowControlAction()) {
                        if (getLogger().isDebugEnabled())
                            getLogger().debug("({}) action is a flow control action. Canceling...", currentAction.getName());
                        actionsForExecute = Collections.emptyIterator();
                        become(initialized);
                    } else 
                        getFacade().send(EXECUTE_NEXT_ACTION);            
                }
                return VOID;
            } else 
                return UNHANDLED;
        }
    }.andThen(processDtmfRequests);
    
    private final Behaviour canceling = new Behaviour("Canceling execution") {
        @Override public Object processData(Object message) throws Exception {
            if (message==DataProcessorFacade.TIMEOUT_MESSAGE || message instanceof ActionExecuted) {
                getFacade().resetReceiveTimeout();
                getContext().unstashAll();
                become(canceled);
                return VOID;
            } else return UNHANDLED;
        }
    }.andThen(processDtmfRequests);
    
    private final Behaviour canceled = new Behaviour("Execution canceled") {
        @Override public Object processData(Object message) throws Exception {
            if (message==CANCEL_ACTIONS_EXECUTION) {
                currentAction = null;
                become(initialized);
                return true;
            } else 
                return UNHANDLED;
        }
    };
    
    private void executeAction(final IvrAction action) {
        currentAction = action;
        try {
//            action.setLogPrefix(logPrefix);
            if (checkDtmfProcessingPoint(action)) {
                if (getLogger().isDebugEnabled())
                    getLogger().debug(">>> ({})", action.getName());
                action.execute(conversation, actionStopListener, getLogger());
            } else if (getLogger().isDebugEnabled()) 
                getLogger().debug("Skipping execution of the action ({})", action.getName());
            //executing actions async!!! on my opinion
        } catch (Exception ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error(String.format("Action (%s) execution error", action.getName()), ex);
                getLogger().error("Canceling...");
            }
            actionsForExecute = Collections.emptyIterator();
            become(initialized);
        }
    }
    
    private boolean checkDtmfProcessingPoint(final IvrAction action) {
        if (action instanceof DtmfProcessPointAction) {
            List dtmfs = new ArrayList(collectedDtmfs.size());
            String validDtmfs = ((DtmfProcessPointAction)action).getDtmfs();
            for (char c: collectedDtmfs)
                if (validDtmfs.indexOf(c)>=0)
                    dtmfs.add(c);
            for (char c: validDtmfs.toCharArray())
                deferredDtmfs.remove(c);
            if (dtmfs.size()>0) {
                conversation.getConversationScenarioState().getBindings().put(
                        IvrEndpointConversation.DTMFS_BINDING, dtmfs);
                return true;
            } else
                return false;
        } else
            return true;
    }
    
    private boolean hasDtmfProcessPoint(HasDtmfProcessingPoint message) {
        if (deferredDtmfs.contains(message.dtmf)){
            collectedDtmfs.add(message.dtmf);
            return true;
        } else
            return false;
    }

    private List<Character> getCollectedDtmfs() {
        return collectedDtmfs.isEmpty()? Collections.EMPTY_LIST : new ArrayList<>(collectedDtmfs);
    }

    @Override
    public Object processData(Object dataPackage) throws Exception {
        return UNHANDLED;
//        return VOID;
    }
        
    private final static class ActionExecuted {
        public final IvrAction action;
        public ActionExecuted(IvrAction action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return "ACTION_EXECUTED: "+action.getName();
        }
    }
    
    private class StopListener implements ActionStopListener {
        @Override public void actionExecuted(IvrAction action) {
            getFacade().send(new ActionExecuted(action));
        }
    }
}
