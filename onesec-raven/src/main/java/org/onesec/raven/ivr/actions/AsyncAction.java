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

import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.RavenPromise;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
@Deprecated
public abstract class AsyncAction extends IvrAbstractAction 
{
//    protected IvrEndpointConversation conversation;
//    protected LoggerHelper logger;
//    private final AtomicBoolean cancelRequest;
//    private volatile ActionStopListener stopListener;
//    private volatile boolean waitForExecuted;

    public AsyncAction(String actionName)
    {
        super(actionName);
//        cancelRequest = new AtomicBoolean(false);
    }

////    @Override
////    public void cancel() throws IvrActionException
////    {
////        if (logger.isDebugEnabled())
////            logger.debug("Canceling execution");
////        cancelRequest.set(true);
////    }
////
////    public boolean hasCancelRequest()
////    {
////        return cancelRequest.get();
//    }
    
//    protected void waitForExecutedEvent() {
//        waitForExecuted = true;
//    }
    
//    protected void executed() {
//        setStatus(IvrActionStatus.EXECUTED);
//        if (stopListener!=null)
//            stopListener.actionExecuted(this);        
//    }

    @Override
    public RavenFuture<IvrAction, IvrActionException> execute(final IvrEndpointConversation conversation, final LoggerHelper logger) 
    {
//        this.stopListener = stopListener;
//        this.waitForExecuted = false;
//        this.conversation = endpoint;
        final LoggerHelper actionLogger = new LoggerHelper(logger, getName()+". ");
        final RavenPromise<IvrAction, IvrActionException> completionPromise = new RavenPromise<>(conversation.getExecutorService());
        try {
//            setStatus(IvrActionStatus.EXECUTING);
            conversation.getExecutorService().execute(new ActionExecutor(conversation, completionPromise, actionLogger));
        } catch (ExecutorServiceException ex) {            
            completionPromise.completeWithError(new IvrActionException(this, ex));
//            setStatus(IvrActionStatus.EXECUTED);
//            throw new IvrActionException("Error executing async action", ex);
        }
        return completionPromise.getFuture();
    }
    
    //for tests purposes
//    public void setLogger(LoggerHelper logger) {
//        this.logger = logger;
//    }
//
//    @Override
//    public Node getTaskNode() {
//        return conversation.getOwner();
//    }

//    @Override
//    public void run() {
//        try {
//            try {
//                if (logger.isDebugEnabled())
//                    logger.debug("Executing...");
//                doExecute(conversation);
//                if (logger.isDebugEnabled()) {
//                    if (waitForExecuted)
//                        logger.debug("Wating for executed event...");
//                    else
//                        logger.debug(hasCancelRequest()? "Canceled" : "Executed");
//                }
//            } catch (Exception ex) {
//                if (logger.isErrorEnabled())
//                    logger.error("Execution error", ex);
//            }
//        } finally {
//            if (!waitForExecuted) {
//                setStatus(IvrActionStatus.EXECUTED);
//                if (stopListener!=null)
//                    stopListener.actionExecuted(this);
//            }
//        }
//    }

    /**
     * Returns true if action were executed. In this case when methods returns <b>true</b> AsyncAction automaticly 
     * completes a completionPromise. If methods returns false then method doExecute MUST themselves complete the completionPromise
     * @param conversation the conversation
     * @param completionPromise completion promise
     * @param logger action logger
     * @throws Exception 
     */
    protected abstract boolean doExecute(IvrEndpointConversation conversation, 
            RavenPromise<IvrAction, IvrActionException> completionPromise, 
            LoggerHelper logger) throws Exception;
    
    private class ActionExecutor extends AbstractTask {
        private final IvrEndpointConversation conversation;
        private final RavenPromise<IvrAction, IvrActionException> completionPromise;
        private final LoggerHelper logger;

        public ActionExecutor(final IvrEndpointConversation conversation, 
                final RavenPromise<IvrAction, IvrActionException> completionPromise, 
                final LoggerHelper logger) 
        {
            super(conversation.getOwner(), String.format("Executing action (%s)", getName()));
            this.conversation = conversation;
            this.completionPromise = completionPromise;
            this.logger = logger;
        }

        @Override
        public void doRun() throws Exception {
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Executing...");
                boolean completeExecution = doExecute(conversation, completionPromise, logger);
                if (completeExecution) {
                    if (!completionPromise.isCanceled())
                        completionPromise.completeWithValue(AsyncAction.this);
                    if (logger.isDebugEnabled())
                        logger.debug(completionPromise.isCanceled()? "Canceled" : "Executed");                    
                }
            } catch (Throwable e) {
                if (logger.isErrorEnabled())
                    logger.error("Execution error", e);
                completionPromise.completeWithError(new IvrActionException(AsyncAction.this, e));
            }
        }        
    }
}
