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

package org.onesec.raven.ivr;

import org.onesec.raven.ivr.impl.IvrActionsExecutorImpl;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrAction
{
    /**
     * Returns the action name.
     */
    public String getName();
    /**
     * If returns true then {@link IvrActionsExecutorImpl actions executor} stops executing actions following
     * this action
     */
    public boolean isFlowControlAction();
    /**
     * Executes action for passed in the parameter endpoint
     * @throws IvrActionException
     */
    public void execute(IvrEndpointConversation conversation, ActionStopListener listener, LoggerHelper logger) throws Exception;
    /**
     * Returns the action status
     */
    public IvrActionStatus getStatus();
    /**
     * Cancels the action execution
     */
    public void cancel() throws IvrActionException;
    /**
     * Return the current action status message
     */
    public String getStatusMessage();

//    public void setLogPrefix(String prefix);
//    public void setLogger(LoggerHelper logger);
}
