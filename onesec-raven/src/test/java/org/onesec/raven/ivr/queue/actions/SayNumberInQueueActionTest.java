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

import java.util.List;
import java.io.File;
import javax.script.Bindings;
import org.apache.commons.io.FileUtils;
import org.onesec.raven.ivr.actions.TestEndpointConversationNode;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.actions.SayAmountActionTest;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.conv.ConversationScenarioState;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class SayNumberInQueueActionTest extends OnesecRavenTestCase
{
    private TestEndpointConversationNode conv;
    private ExecutorServiceNode executor;
    private Node numbers;
    private AudioFileNode preamble;

    @Before
    public void prepare() throws Exception
    {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(10);
        executor.setMaximumPoolSize(10);
        assertTrue(executor.start());

        conv = new TestEndpointConversationNode();
        conv.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(conv);
        conv.setExecutorService(executor);
        conv.setFileName("target/number_in_queue.wav");
//        assertTrue(conv.start());

        numbers = SayAmountActionTest.createNumbersNode(tree);

        preamble = new AudioFileNode();
        preamble.setName("preamble");
        tree.getRootNode().addAndSaveChildren(preamble);
        preamble.getAudioFile().setDataStream(FileUtils.openInputStream(
                new File("src/test/wav/number_in_queue.wav")));
        assertTrue(preamble.start());
    }

    @Test(timeout=15000)
    public void test() throws Exception
    {
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        Node owner = createMock(Node.class);
        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);

        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_1";
        expect(bindings.get(key)).andReturn(null);
        expect(bindings.put(key, 10)).andReturn(null);
        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
        expect(owner.getId()).andReturn(1).anyTimes();

        replay(state, bindings, callStatus, owner);

        conv.setConversationScenarioState(state);
        assertTrue(conv.start());
        SayNumberInQueueAction action = new SayNumberInQueueAction(owner, numbers, 50, preamble);
        action.execute(conv);
        Thread.sleep(10000);
        
        verify(state, bindings, callStatus, owner);

    }

    @Test
    public void formWords() throws Exception
    {
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);
        Node owner = createMock(Node.class);

        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_1";
        expect(bindings.get(key)).andReturn(10);
        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
        expect(owner.getId()).andReturn(1).anyTimes();

        replay(state, bindings, callStatus, owner);

        conv.setConversationScenarioState(state);
        SayNumberInQueueAction action = new SayNumberInQueueAction(owner, numbers, 50, preamble);
        assertNull(action.formWords(conv));

        verify(state, bindings, callStatus, owner);
    }

    @Test
    public void formWords2() throws Exception
    {
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createMock(Bindings.class);
        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);
        Node owner = createMock(Node.class);

        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_1";
        expect(bindings.get(key)).andReturn(null);
        expect(bindings.put(key, 10)).andReturn(null);
        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
        expect(owner.getId()).andReturn(1).anyTimes();

        replay(state, bindings, callStatus, owner);

        conv.setConversationScenarioState(state);
        SayNumberInQueueAction action = new SayNumberInQueueAction(owner, numbers, 50, preamble);
        List words = action.formWords(conv);
        assertNotNull(words);
        assertEquals(2, words.size());
        assertArrayEquals(new Object[]{preamble, "10"}, words.toArray());

        verify(state, bindings, callStatus, owner);
    }

}