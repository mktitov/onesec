/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AbonentCommutationManagerImpl implements AbonentCommutationManager, EndpointRequest {
    
    private final String abonentNumber;
    private final CallQueueRequestController request;
    private final Node owner;
    private final IvrEndpointPool endpointPool;
    private final IvrConversationScenario conversationScenario;
    private final int inviteTimeout;
    private final long endpointWaitTimeout;
    private volatile String statusMessage;

    public AbonentCommutationManagerImpl(String abonentNumber, CallQueueRequestController request
            , Node owner, IvrEndpointPool endpointPool, IvrConversationScenario conversationScenario
            , int inviteTimeout, long endpointWaitTimeout, String statusMessage) 
    {
        this.abonentNumber = abonentNumber;
        this.request = request;
        this.owner = owner;
        this.endpointPool = endpointPool;
        this.conversationScenario = conversationScenario;
        this.inviteTimeout = inviteTimeout;
        this.endpointWaitTimeout = endpointWaitTimeout;
        this.statusMessage = statusMessage;
        statusMessage = "Searching for free endpoint in the pool for: "+request.toString();
    }

    public void commutate() {
//        endpointPool.re
    }

    public void processRequest(IvrEndpoint endpoint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getWaitTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getOwner() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getStatusMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getPriority() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private class EndpointRequestImpl implements EndpointRequest {

        public void processRequest(IvrEndpoint endpoint) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getWaitTimeout() {
        }

        public Node getOwner() {
            return owner;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public int getPriority() {
            return request.getPriority();
        }
    }
}
