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

package org.onesec.raven.impl;

import org.onesec.core.State;
import org.onesec.raven.StateToNodeLogger;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class StateToNodeLoggerImpl implements StateToNodeLogger
{
    private Node loggerNode;

    public synchronized void setLoggerNode(Node loggerNode)
    {
        this.loggerNode = loggerNode;
    }

    public synchronized Node getLoggerNode()
    {
        return loggerNode;
    }

    public synchronized void stateChanged(State state)
    {
        Node logger = null;
        if (state.getObservableObject() instanceof Node)
            logger = (Node) state.getObservableObject();
        else if (loggerNode!=null && Node.Status.STARTED.equals(loggerNode.getStatus()))
            logger = loggerNode;
        if (logger!=null) {
            String message = state.getObservableObject().getObjectDescription()
                        +" changed state to "+state.getIdName();
            if (state.hasError())
                logger.getLogger().error(message+". "+state.getErrorMessage());
            else
                logger.getLogger().debug(message);
        }
    }
}
