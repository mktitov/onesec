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
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.DtmfProcessPointAction;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.onesec.raven.ivr.impl.ActionExecutorFacade.*;
import static org.onesec.raven.ivr.impl.ActionExecutorFacade.*;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.NonUniqueNameException;
import org.raven.dp.impl.Behaviour;
import org.raven.ds.TimeoutMessageSelector;
/**
 *
 * @author Mikhail Titov
 */
public class ActionExecutorDP extends AbstractDataProcessorLogic {
    private final static String EXECUTE_NEXT_ACTION = "EXECUTE_NEXT_ACTION";
    public final static int CHECK_SPEED_AT_CYCLE = 32;
    private final static long MAX_TIME_PER_EXECUTION = 10;
    private final static long THROTTLE_DELAY = 200;
    private final IvrEndpointConversation conversation;
    private final Set<Character> deferredDtmfs = new HashSet<>();
    private final List<Character> collectedDtmfs = new ArrayList<>();
    private final Execute executeActionMessage;
    
    private Iterator<Action> actionsForExecute;
    private DataProcessorFacade currentAction;
//    private ActionStopListener actionStopListener = new StopListener();
    private long actionId = 0;
    private int speedCheckCounter = 0;
    private long lastCheckSpeedTime = 0;
    private boolean throttled = false;
    private long executionCycleId = 0;

    public ActionExecutorDP(IvrEndpointConversation conversation) {
        this.conversation = conversation;
        this.executeActionMessage = new Execute();        
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
            return message instanceof Action.ActionExecuted;
        }
    };
    
    //Behaviours
    private final Behaviour initialized = new Behaviour("Initialized") {
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof ExecuteActions) {
//                actionId=0;
                ++executionCycleId;
                if (throttled) {
                    getFacade().sendDelayed(THROTTLE_DELAY, new ThrottledActionExecution((ExecuteActions) message, executionCycleId));
                    return VOID;
                }
                if (speedCheckCounter==0)
                    lastCheckSpeedTime = System.currentTimeMillis();
                if (speedCheckCounter>=CHECK_SPEED_AT_CYCLE) {
                    speedCheckCounter = 0;
                    if (System.currentTimeMillis()-lastCheckSpeedTime < CHECK_SPEED_AT_CYCLE * MAX_TIME_PER_EXECUTION) {
                        //матюгаемся матом
                        if (getLogger().isWarnEnabled()) {
                            getLogger().warn("Detected TOO FAST ACTIONS REEXECUTION. Every reexecution will be throttled on {}ms", THROTTLE_DELAY);                            
                            getLogger().warn("Throttled actions sequence: "+getActionsNames(message));
                        }
                        throttled = true;
                        getContext().stash();
                        getContext().unstashAll();
                        return VOID;
                    }
                } else 
                    ++speedCheckCounter;
                initActionsExecution((ExecuteActions) message);
                return VOID;
            } else if (message instanceof ThrottledActionExecution) {
                ThrottledActionExecution wrapper = (ThrottledActionExecution) message;
                if (wrapper.executionCycleId!=executionCycleId)
                    return VOID;
                initActionsExecution(wrapper.executeActionsMessage);
                return VOID;
            } else if (message==CANCEL_ACTIONS_EXECUTION)
                return true;
            else 
                return UNHANDLED;
        }

        private StringBuilder getActionsNames(Object message) {
            StringBuilder actionNames = new StringBuilder();
            boolean first = true;
            for (Action action: ((ExecuteActions)message).actions) {
                if (!first)  actionNames.append(", ");
                else first = false;
                actionNames.append(action.getName());
            }
            return actionNames;
        }

        public void initActionsExecution(ExecuteActions message) {
            Collection<Action> actions = (message).actions;
            actionsForExecute = actions.iterator();
            getFacade().send(EXECUTE_NEXT_ACTION);
            deferredDtmfs.clear();
            collectedDtmfs.clear();
            currentAction = null;
            for (Action action: actions)
                if (action instanceof DtmfProcessPointAction)
                    for (char c: ((DtmfProcessPointAction)action).getDtmfs().toCharArray())
                        deferredDtmfs.add(c);
            become(executing);
        }
    }.andThen(processDtmfRequests);
    
    private final Behaviour executing = new Behaviour("Executing") {
        @Override public Object processData(Object message) throws Exception {
            if (message==EXECUTE_NEXT_ACTION) 
                return processExecuteNextAction();
//            {
//                if (actionsForExecute.hasNext())
//                    executeAction(actionsForExecute.next());
//                else {
//                    if (getLogger().isDebugEnabled())
//                        getLogger().debug("No more action to execute");
//                    become(initialized);
//                }
//                return VOID;
//            } 
            else if (message==CANCEL_ACTIONS_EXECUTION) 
                return processCancelActionExecuting();
//            {
//                if (currentAction!=null)
//                    currentAction.send(Action.CANCEL);                
//                actionsForExecute = Collections.emptyIterator();
//                getFacade().setReceiveTimeout(CANCEL_TIMEOUT, actionExecutedSelector);
//                become(canceling);
//                return getContext().stash();
//            } 
            else if (message instanceof Action.ActionExecuted) 
                return processActionExecutedEvent((Action.ActionExecuted)message);
            else
                return UNHANDLED;
//            {
//                if (currentAction==getSender()) {
//                    
//                } else if (getSender)
//                if (currentAction==((ActionExecuted)message).action) {
//                    if (getLogger().isDebugEnabled())
//                        getLogger().debug("<<< ({})", currentAction.getName());
//                    if (currentAction.isFlowControlAction()) {
//                        if (getLogger().isDebugEnabled())
//                            getLogger().debug("({}) action is a flow control action. Canceling...", currentAction.getName());
//                        actionsForExecute = Collections.emptyIterator();
//                        become(initialized);
//                    } else 
//                        getFacade().send(EXECUTE_NEXT_ACTION);            
//                }
//                else {
//                    if (getLogger().isDebugEnabled())
//                        getLogger().debug("Ignoring ACTION_EXECUTED message for action ({})", ((ActionExecuted)message).action.getName());
//                }
//                return VOID;
//            } else 
//                return UNHANDLED;
        }
    }.andThen(processDtmfRequests);
    
    private final Behaviour canceling = new Behaviour("Canceling execution") {
        @Override public Object processData(Object message) throws Exception {
            if (message==DataProcessorFacade.TIMEOUT_MESSAGE || message instanceof Action.ActionExecuted) {
                getFacade().resetReceiveTimeout();
                getContext().unstashAll();
                currentAction.stop();
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
    
    private Object processExecuteNextAction() throws Exception {
        if (actionsForExecute.hasNext())
            executeAction(actionsForExecute.next());
        else {
            if (getLogger().isDebugEnabled())
                getLogger().debug("No more action to execute");
            become(initialized);
        }
        return VOID;
    }
    
    private Object processCancelActionExecuting() throws Exception {
        if (currentAction==null) {
            become(initialized);
            return true;
        } else {
            currentAction.send(Action.CANCEL);                
            actionsForExecute = Collections.emptyIterator();
            getFacade().setReceiveTimeout(CANCEL_TIMEOUT, actionExecutedSelector);
            become(canceling);
            return getContext().stash();            
        }
    }
    
    private Object processActionExecutedEvent(Action.ActionExecuted message) throws Exception {
        try {
            if (getSender()==null) 
                throw new Exception("Received ActionExecuted message without sender!");
            else if (getSender()!=currentAction)
                return UNHANDLED;
            else {
                if (getLogger().isDebugEnabled())
                    getLogger().debug("<<< ({})", currentAction.getName());
                switch (message.getNextAction()) {
                    case EXECUTE_NEXT: 
                        getFacade().send(EXECUTE_NEXT_ACTION); 
                        break;
                    case STOP: 
                        actionsForExecute = Collections.emptyIterator();
                        become(initialized);
                        break;
                    default:
                        throw new Exception("UNKNOWN nextAction: "+message.getNextAction());
                }
                return VOID;
            }            
        } finally {
            if (currentAction!=null)
                currentAction.stop();
            currentAction = null;
        }
    }
    
    private void executeAction(final Action action) throws NonUniqueNameException {
        if (checkDtmfProcessingPoint(action)) {
            actionId++;
            if (getLogger().isDebugEnabled())
                getLogger().debug(">>> [id:{}] {}", actionId, action.getName());
            currentAction = getContext().addChild(getContext().createChild("[id:"+actionId+"] "+action.getName(), action));
            currentAction.send(executeActionMessage);
        } else if (getLogger().isDebugEnabled()) {
            getLogger().debug("Skipping execution of the action ({})", action.getName());
            getFacade().send(EXECUTE_NEXT_ACTION);
        }
        
    }
    
    private void _executeAction(final Action action) {
//        currentAction = action;
//        try {
////            action.setLogPrefix(logPrefix);
//            actionId++;
//            if (checkDtmfProcessingPoint(action)) {
//                if (getLogger().isDebugEnabled())
//                    getLogger().debug(">>> [id:{}] {}", actionId, action.getName());
////                action.execute(conversation, actionStopListener, new LoggerHelper(getLogger(), "[id:"+(actionId)+"] "));
//            } else if (getLogger().isDebugEnabled()) 
//                getLogger().debug("Skipping execution of the action ({})", action.getName());
//            //executing actions async!!! on my opinion
//        } catch (Exception ex) {
//            if (getLogger().isErrorEnabled()) {
//                getLogger().error(String.format("Action (%s) execution error", action.getName()), ex);
//                getLogger().error("Canceling...");
//            }
//            actionsForExecute = Collections.emptyIterator();
//            become(initialized);
//        }
    }
    
    private boolean checkDtmfProcessingPoint(final Action action) {
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
    
    private final class ThrottledActionExecution {
        private final ExecuteActions executeActionsMessage;
        private final long executionCycleId;

        public ThrottledActionExecution(ExecuteActions executeActionsMessage, long executionCycleId) {
            this.executeActionsMessage = executeActionsMessage;
            this.executionCycleId = executionCycleId;
        }        

        @Override
        public String toString() {
            return "THROTTLED_ACTION_EXECUTION";
        }        
    }
    
    private class Execute implements Action.Execute {
        @Override public IvrEndpointConversation getConversation() {
            return conversation;
        }

        @Override
        public String toString() {
            return "EXECUTE";
        }
    }
    
        
//    private final static class ActionExecuted {
//        public final IvrAction action;
//        public ActionExecuted(IvrAction action) {
//            this.action = action;
//        }
//
//        @Override
//        public String toString() {
//            return "ACTION_EXECUTED: "+action.getName();
//        }
//    }
//    
//    private class StopListener implements ActionStopListener {
//        @Override public void actionExecuted(IvrAction action) {
//            getFacade().send(new ActionExecuted(action));
//        }
//    }
}
