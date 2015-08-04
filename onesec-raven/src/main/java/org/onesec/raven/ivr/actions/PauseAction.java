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

import org.onesec.raven.ivr.ActionStopListener;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class PauseAction extends AbstractAction
{
    public static final String ACTION_NAME = "Pause action";
    private final long interval;
    private volatile ActionStopListener stopListener;
    private volatile LoggerHelper logger;
    private volatile PauseTask pauseTask;

    public PauseAction(long interval)
    {
        super(ACTION_NAME);
        this.interval = interval;
        setStatusMessage("Pausing");
    }

    public boolean isFlowControlAction() {
        return false;
    }

    @Override
    public void execute(IvrEndpointConversation conversation, ActionStopListener listener, LoggerHelper logger) 
            throws Exception 
    {
        this.stopListener = listener;
        this.logger = new LoggerHelper(logger, getName()+". ");
        if (this.logger.isDebugEnabled())
            this.logger.debug("Pausing on "+interval+" ms");
        pauseTask = new PauseTask(conversation.getOwner(), stopListener);
        conversation.getExecutorService().execute(interval, pauseTask);
    }

    @Override
    public void cancel() throws IvrActionException {
        if (pauseTask!=null) 
            pauseTask.cancel();
        stopListener.actionExecuted(this);
    }
//    @Override
//    protected void doExecute(IvrEndpointConversation conversation) throws Exception
//    {
//        if (logger.isDebugEnabled())
//            logger.debug("Pausing on "+interval+" ms");
//        long start = System.currentTimeMillis();
//        do {
//            TimeUnit.MILLISECONDS.sleep(10);
//        } while (System.currentTimeMillis()-start<interval && !hasCancelRequest());
//    }
    
    private class PauseTask extends AbstractTask {
        private final ActionStopListener stopListener;

        public PauseTask(Node taskNode, ActionStopListener stopListener) {
            super(taskNode, "Pausing on "+interval+" ms");
            this.stopListener = stopListener;
        }

        @Override
        public void doRun() throws Exception {
            this.stopListener.actionExecuted(PauseAction.this);
        }
    }
}
