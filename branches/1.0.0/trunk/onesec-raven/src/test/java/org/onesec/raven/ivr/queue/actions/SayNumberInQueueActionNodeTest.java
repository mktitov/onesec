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
import javax.script.SimpleBindings;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titiov
 */
public class SayNumberInQueueActionNodeTest extends OnesecRavenTestCase
{
    private SayNumberInQueueActionNode actionNode;

    @Before
    public void preapare()
    {
        AudioFileNode audioNode = new AudioFileNode();
        audioNode.setName("audio node");
        tree.getRootNode().addAndSaveChildren(audioNode);

        Node numbersNode = new BaseNode("numbersNode");
        tree.getRootNode().addAndSaveChildren(numbersNode);

        actionNode = new SayNumberInQueueActionNode();
        actionNode.setName("action node");
        tree.getRootNode().addAndSaveChildren(actionNode);
        actionNode.setNumbersNode(numbersNode);
        actionNode.setPreambleAudio(audioNode);
        assertTrue(actionNode.start());
    }

    @Test
    public void acceptTest() throws Exception
    {
        NodeAttribute attr = actionNode.getNodeAttribute(SayNumberInQueueActionNode.ACCEPT_SAY_NUMBER_ATTR);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.setValue("accept");

        Bindings bindings = new SimpleBindings();
//        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_"+actionNode.getId();

        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        QueuedCallStatus status = createMock(QueuedCallStatus.class);
        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);

        expect(conv.getConversationScenarioState()).andReturn(state).atLeastOnce();
        expect(state.getBindings()).andReturn(bindings).anyTimes();
        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE).atLeastOnce();
        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();

        replay(conv, state, status, callStatus);

        bindings.put(QueueCallAction.QUEUED_CALL_STATUS_BINDING, callStatus);
        bindings.put("accept", true);
        SayNumberInQueueAction action = (SayNumberInQueueAction) actionNode.createAction();
        assertNotNull(action.formWords(conv));

        bindings.put("accept", false);
        assertNull(action.formWords(conv));

        verify(conv, state, status, callStatus);
    }
}