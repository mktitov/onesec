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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.IvrEndpointConversationListenerAdapter;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequestController;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
//TODO: need operator conversation listener
public class AbonentCommutationManagerImpl implements AbonentCommutationManager {
    
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
        try {
            endpointPool.requestEndpoint(new EndpointRequestListener());
        } catch (ExecutorServiceException ex) {
            //TODO: inform request that commutation finished
        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        //TODO: inform request that abonent ready to commutate
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void doCommutate(IvrEndpoint endpoint) {
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("abonentCommutationManager", this);
        endpoint.invite(abonentNumber, inviteTimeout, 0, new ConversationListener()
                , conversationScenario, bindings);
    }
    
    private class EndpointRequestListener implements EndpointRequest {

        public void processRequest(IvrEndpoint endpoint) {
            doCommutate(endpoint);
        }

        public long getWaitTimeout() {
            return endpointWaitTimeout;
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
    
    private class ConversationListener extends IvrEndpointConversationListenerAdapter {
        @Override
        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            //TODO: inform request that commutation finished
        }
    }
}
