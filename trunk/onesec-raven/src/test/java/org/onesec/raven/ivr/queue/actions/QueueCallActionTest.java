/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.queue.actions;

import javax.script.Bindings;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.onesec.raven.ivr.queue.impl.CallQueueRequestImpl;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class QueueCallActionTest extends Assert
{
    @Test
    public void createRequestTest() throws Exception
    {
        CallQueueRequestSender requestSender = createMock(CallQueueRequestSender.class);
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        DataContext context = createMock(DataContext.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Node owner = createMock(Node.class);
        Bindings bindings = createMock(Bindings.class);

        expect(conversation.getOwner()).andReturn(owner).anyTimes();
        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(false).anyTimes();
        expect(conversation.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(null);
        state.setBinding(eq(QueueCallAction.QUEUED_CALL_STATUS_BINDING), checkCallQueueRequest()
                , eq(BindingScope.POINT));
        expect(requestSender.createDataContext()).andReturn(context);
        requestSender.sendCallQueueRequest(isA(CallQueueRequest.class), isA(DataContext.class));

        replay(requestSender, conversation, state, owner, bindings, context);

        QueueCallAction action = new QueueCallAction(requestSender, true, false, 10, "test queue", false);
        action.doExecute(conversation);

        verify(requestSender, conversation, state, owner, bindings, context);
    }

    @Test
    public void commutationPhaseTest() throws Exception
    {
        CallQueueRequestSender requestSender = createMock(CallQueueRequestSender.class);
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Node owner = createMock(Node.class);
        Bindings bindings = createMock(Bindings.class);
        QueuedCallStatus callStatus = createStrictMock(QueuedCallStatus.class);

        expect(conversation.getOwner()).andReturn(owner).anyTimes();
        expect(owner.isLogLevelEnabled((LogLevel)anyObject())).andReturn(false).anyTimes();
        expect(conversation.getConversationScenarioState()).andReturn(state);
        expect(state.getBindings()).andReturn(bindings);
        state.disableDtmfProcessing();
        state.enableDtmfProcessing();
        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
        expect(callStatus.isReadyToCommutate()).andReturn(true).anyTimes();
        expect(callStatus.getOperatorGreeting()).andReturn(null);
        callStatus.replayToReadyToCommutate();
        expect(callStatus.isCommutated()).andReturn(Boolean.TRUE).anyTimes();
        expect(callStatus.isDisconnected()).andReturn(Boolean.TRUE);

        replay(requestSender, conversation, state, owner, bindings, callStatus);

        QueueCallAction action = new QueueCallAction(requestSender, true, true, 10, "test queue", true);
        action.doExecute(conversation);

        verify(requestSender, conversation, state, owner, bindings, callStatus);
    }

    public static Object checkCallQueueRequest() 
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument)
            {
                assertTrue(argument instanceof CallQueueRequestImpl);
                CallQueueRequestImpl request = (CallQueueRequestImpl) argument;
                assertEquals(10, request.getPriority());
                assertEquals("test queue", request.getQueueId());
                assertTrue(request.isContinueConversationOnReadyToCommutate());
                assertFalse(request.isContinueConversationOnReject());
                assertTrue(request.isQueueing());
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
}