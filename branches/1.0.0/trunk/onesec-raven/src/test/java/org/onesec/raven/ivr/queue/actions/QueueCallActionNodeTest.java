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

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrAction;

/**
 *
 * @author Mikhail Titov
 */
public class QueueCallActionNodeTest extends OnesecRavenTestCase {

    private QueueCallActionNode actionNode;

    @Before
    public void prepare() {
        actionNode = new QueueCallActionNode();
        actionNode.setName("queue action");
        tree.getRootNode().addAndSaveChildren(actionNode);
        actionNode.setQueueId("test queue");
        actionNode.setContinueConversationOnReadyToCommutate(Boolean.TRUE);
        actionNode.setContinueConversationOnReject(Boolean.FALSE);
        assertTrue(actionNode.start());
    }

    @Test
    public void createActionTest() {
        IvrAction action = actionNode.createAction();
        assertNotNull(action);
        assertTrue(action instanceof QueueCallAction);
        QueueCallAction queueAction = (QueueCallAction) action;
        assertTrue(queueAction.isContinueConversationOnReadyToCommutate());
        assertFalse(queueAction.isContinueConversationOnReject());
        assertEquals(10, queueAction.getPriority());
        assertEquals("test queue", queueAction.getQueueId());
        assertSame(actionNode, queueAction.getRequestSender());
    }
}