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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.IvrActionExecutor;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ActionExecutorFacade implements IvrActionExecutor {
    final static String GET_COLLECTED_DTMFS = "GetCollectedDtmfs";
    final static String CANCEL_ACTIONS_EXECUTION = "CancelActionsExecution";
    
    private final DataProcessorFacade facade;
    private final IvrEndpointConversation conversation;
    private final ExecutorService executor;
    
    public ActionExecutorFacade(IvrEndpointConversation conversation, LoggerHelper logger) {
        this.conversation = conversation;
        this.executor = conversation.getExecutorService();
        facade = new DataProcessorFacadeConfig(
                "Actions executor", conversation.getOwner(), new ActionExecutorDP(conversation)
                , executor, logger).build();
    }

    @Override
    public void stop() {
        facade.stop();
    }

    @Override
    public void executeActions(Collection<Action> actions) throws ExecutorServiceException, InterruptedException {
        cancelActionsExecution();
        facade.send(new ExecuteActions(actions));
    }

    @Override
    public boolean hasDtmfProcessPoint(char dtmf) {
        return (Boolean)facade.ask(new HasDtmfProcessingPoint(dtmf)).getOrElse(false, 500);
    }

    @Override
    public List<Character> getCollectedDtmfs() {
        return (List<Character>)facade.ask(GET_COLLECTED_DTMFS).getOrElse(Collections.EMPTY_LIST, 500);
    }

    @Override
    public void cancelActionsExecution() throws InterruptedException {
        facade.ask(CANCEL_ACTIONS_EXECUTION, 2*CANCEL_TIMEOUT, TimeUnit.MILLISECONDS).getOrElse(null);
    }
    
    final class ExecuteActions {
        public final Collection<Action> actions;

        public ExecuteActions(Collection<Action> actions) {
            this.actions = actions;
        }

        @Override
        public String toString() {
            return "ExecuteActions";
        }
    }
    
    public final class HasDtmfProcessingPoint {
        public final char dtmf;

        public HasDtmfProcessingPoint(char dtmf) {
            this.dtmf = dtmf;
        }

        @Override
        public String toString() {
            return "HasDtmfProcessingPoint";
        }
    }
    
}
