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

import java.util.Map;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.*;
import org.onesec.raven.ivr.queue.AbonentCommutationManager;
import org.onesec.raven.ivr.queue.CallQueueRequestListener;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.onesec.raven.ivr.queue.event.DisconnectedQueueEvent;
import org.onesec.raven.ivr.queue.event.ReadyToCommutateQueueEvent;
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.slf4j.Logger;
/**
 *
 * @author Mikhail Titov
 */
public class AbonentCommutationManagerImplTest {
    
    private static IvrEndpointConversationListener listener;
    
    @Before
    public void prepare() {
        
    }
    
    @Test
    public void test() throws Exception {
        long waitTimeout = 1000;
        int priority = 1;
        int inviteTimeout = 100;
        String abonentNumber = "112233";
        String queueId = "queueId";
        
        DataContext context = createMock(DataContext.class);
        Node owner = createMock(Node.class);
        Logger logger = createMock(Logger.class);
        ReadyToCommutateQueueEvent readyEvent = createMock(ReadyToCommutateQueueEvent.class);
        CommutationManagerCall commutationManager = createMock(CommutationManagerCall.class);
        IvrEndpointPool pool = createMock(IvrEndpointPool.class);
        IvrEndpoint endpoint = createMock(IvrEndpoint.class);
        IvrConversationScenario scenario = createMock(IvrConversationScenario.class);
        IvrEndpointConversationEvent conversationEvent = createMock(IvrEndpointConversationEvent.class);
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        IvrEndpointConversationState convState = createMock(IvrEndpointConversationState.class);
        CallQueueRequestListener requestListener = createMock(CallQueueRequestListener.class);
        DisconnectedQueueEvent disconnectedEvent = createMock(DisconnectedQueueEvent.class);
        
        //logging
        expect(owner.isLogLevelEnabled(anyObject(LogLevel.class))).andReturn(true).anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        logger.debug(anyObject(String.class));
        expectLastCall().anyTimes();
        //handle ReadyToCommutate event
        expect(readyEvent.getCommutationManager()).andReturn(commutationManager);
        //initCallToAbonent
        pool.requestEndpoint(checkEndpointPoolRequest(owner, waitTimeout, priority, endpoint));
        //callToAbonent
        endpoint.invite(eq(abonentNumber), eq(inviteTimeout), eq(0), checkConversationListener(conversationEvent)
                , same(scenario), checkBindings());
        expect(conversationEvent.getConversation()).andReturn(conversation);
        requestListener.conversationAssigned(conversation);
        //abonentReadyToCommutate
        commutationManager.abonentReadyToCommutate(conversation);
        //abonent conversation stopped
        expect(conversation.getState()).andReturn(convState);
        expect(convState.getId()).andReturn(IvrEndpointConversationState.TALKING);
        pool.releaseEndpoint(endpoint);
        
        replay(context, owner, logger, readyEvent, commutationManager, pool, endpoint, scenario
                , conversationEvent, conversation, requestListener, disconnectedEvent, convState);
        
        AbonentCommutationManagerImpl manager = new AbonentCommutationManagerImpl(abonentNumber, 
                queueId, priority, owner, context, pool, scenario, inviteTimeout, waitTimeout);
        assertEquals(queueId, manager.getQueueId());
        assertSame(context, manager.getContext());
        assertNull(manager.getConversation());
        assertEquals(priority, manager.getPriority());
        assertNull(manager.getOperatorPhoneNumbers());
        assertTrue(manager.isCommutationValid());
        
        manager.addRequestListener(requestListener);
        manager.callQueueChangeEvent(readyEvent);
        manager.abonentReadyToCommutate(conversation);
        manager.callQueueChangeEvent(disconnectedEvent);
        assertFalse(manager.isCommutationValid());
        listener.conversationStopped(null);
        
        verify(context, owner, logger, readyEvent, commutationManager, pool, endpoint, scenario
                , conversationEvent, conversation, requestListener, disconnectedEvent, convState);
    }

    public static EndpointRequest checkEndpointPoolRequest(final Node owner, 
            final long waitTimeout, final int priority, final IvrEndpoint endpoint) 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                EndpointRequest request = (EndpointRequest) argument;
                assertSame(owner, request.getOwner());
                assertEquals(waitTimeout, request.getWaitTimeout());
                assertEquals(priority, request.getPriority());
                request.processRequest(endpoint);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    public static IvrEndpointConversationListener checkConversationListener(
            final IvrEndpointConversationEvent event) 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                listener = (IvrEndpointConversationListener) argument;
                listener.listenerAdded(event);
                listener.conversationStarted(null);
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }

    public static Map<String, Object> checkBindings() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                Map<String, Object> bindings = (Map<String, Object>) argument;
                assertNotNull(bindings);
                Object manager = bindings.get(AbonentCommutationManagerImpl.ABONENT_COMMUTATION_MANAGER_BINDING);
                assertNotNull(manager);
                assertTrue(manager instanceof AbonentCommutationManager);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}
