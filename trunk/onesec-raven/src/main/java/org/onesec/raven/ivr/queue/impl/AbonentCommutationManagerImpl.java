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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.impl.IvrEndpointConversationListenerAdapter;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.event.CallQueueEvent;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.event.ReadyToCommutateQueueEvent;
import org.onesec.raven.ivr.queue.event.RejectedQueueEvent;
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AbonentCommutationManagerImpl implements CallQueueRequest, AbonentCommutationManager {
    
    private final static AtomicLong counter = new AtomicLong();
    
    private final String abonentNumber;
    private final Node owner;
    private final IvrEndpointPool endpointPool;
    private final IvrConversationScenario conversationScenario;
    private final int inviteTimeout;
    private final long endpointWaitTimeout;
    private final DataContext context;
    private final long requestId = counter.incrementAndGet();
    
    private volatile String queueId;
    private volatile int priority;
    
    private volatile String statusMessage;
    private volatile IvrEndpointConversation conversation;
    private volatile boolean canceled = false;
    private volatile CommutationManagerCall commutationManager;
    private volatile boolean disconnected = false;
    private final List<CallQueueRequestListener> listeners = new LinkedList<CallQueueRequestListener>();

    public AbonentCommutationManagerImpl(String abonentNumber, String queueId, int priority
            , Node owner, DataContext context
            , IvrEndpointPool endpointPool, IvrConversationScenario conversationScenario
            , int inviteTimeout, long endpointWaitTimeout) 
    {
        this.abonentNumber = abonentNumber;
        this.owner = owner;
        this.context = context;
        this.endpointPool = endpointPool;
        this.conversationScenario = conversationScenario;
        this.inviteTimeout = inviteTimeout;
        this.endpointWaitTimeout = endpointWaitTimeout;
        this.priority = priority;
        this.queueId = queueId;
//        statusMessage = "Searching for free endpoint in the pool for: "+request.toString();
    }

    public void addRequestListener(CallQueueRequestListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    public void callQueueChangeEvent(CallQueueEvent event) {
        if (event instanceof ReadyToCommutateQueueEvent) {
            commutationManager = ((ReadyToCommutateQueueEvent)event).getCommutationManager();
            initiateCallToAbonent();
        } else if (event instanceof DisconnectedQueueEvent) {
            disconnected = true;
        } else if (event instanceof RejectedQueueEvent) {
            disconnected = true;
        }
    }

    public boolean isCommutationValid() {
        return !disconnected;
    }

    public DataContext getContext() {
        return context;
    }

    public IvrEndpointConversation getConversation() {
        return conversation;
    }

    public String getConversationInfo() {
        return conversation==null? conversation.getObjectName() : logMess("");
    }

    public String getOperatorPhoneNumbers() {
        return null;
    }

    public int getPriority() {
        return priority;
    }

    public String getQueueId() {
        return queueId;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setOperatorPhoneNumbers(String phoneNumbers) {
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    private void initiateCallToAbonent() {
        try {
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Initiating call to abonent"));
            endpointPool.requestEndpoint(new EndpointRequestListener());
        } catch (ExecutorServiceException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Error while requesting endpoint from the pool"), ex);
            fireRequestCanceledEvent();
        }
    }

    public void abonentReadyToCommutate(IvrEndpointConversation abonentConversation) {
        commutationManager.abonentReadyToCommutate(abonentConversation);
    }
    
    private void callToAbonent(IvrEndpoint endpoint) {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Calling to the abonent"));
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(ABONENT_COMMUTATION_MANAGER_BINDING, this);
        endpoint.invite(abonentNumber, inviteTimeout, 0, new ConversationListener()
                , conversationScenario, bindings);
    }
    
    private String logMess(String message, Object... args) {
        return "[reqId: "+requestId+"; abon number: "+abonentNumber+"] "
                +String.format(message, args);
    }
    
    private void fireRequestCanceledEvent() {
        synchronized(listeners) {
            for (CallQueueRequestListener listener: listeners)
                listener.requestCanceled();
        }
    }
    
    private void fireConversationAssignedEvent() {
        synchronized(listeners) {
            for (CallQueueRequestListener listener: listeners)
                listener.conversationAssigned(conversation);
        }
    }
    
    private class EndpointRequestListener implements EndpointRequest {

        public void processRequest(IvrEndpoint endpoint) {
            callToAbonent(endpoint);
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
            return priority;
        }
    }
    
    private class ConversationListener extends IvrEndpointConversationListenerAdapter {

        @Override
        public void listenerAdded(IvrEndpointConversationEvent event) {
            conversation = event.getConversation();
            fireConversationAssignedEvent();
        }
    }
}
