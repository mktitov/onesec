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
import javax.script.Bindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.actions.PauseActionTest;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import static org.onesec.raven.ivr.queue.actions.SayNumberInQueueAction.LAST_SAYED_NUMBER;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class SayNumberInQueueActionTest extends Assert
{
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    
//    private TestEndpointConversationNode conv;
//    private ExecutorServiceNode executor;
//    private Node numbers;
//    private AudioFileNode preamble;
//    private SayNumberInQueueActionNode owner;
//    private ResourceManager resourceManager;
    
    @Test
    public void testSkipByNullCallStatus(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = null;
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSkipByNotQueueingCallStatus(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            callStatus.isQueueing(); result = false;
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSkipBySerialNumberLowerThanOne(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            callStatus.isQueueing(); result = true;
            callStatus.getSerialNumber(); result = 0;
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSkipBySerialNumberLowerThanLastSayedNumber(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            bindings.get(key); result = 5;
            callStatus.isQueueing(); result = true;
            callStatus.getSerialNumber(); result = 6;
            bindingSupport.putAll(bindings); times = 0;
            
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSkipByDisabledAccept(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            bindings.get(key); result = 5;
            callStatus.isQueueing(); result = true;
            callStatus.getSerialNumber(); result = 4;
            bindingSupport.putAll(bindings); 
            actionNode.getAcceptSayNumber(); result = null;
            
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSkipByDisabledAccept2(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            bindings.get(key); result = 5;
            callStatus.isQueueing(); result = true;
            callStatus.getSerialNumber(); result = 4;
            bindingSupport.putAll(bindings); 
            actionNode.getAcceptSayNumber(); result = false;
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        assertNull(action.formWords(conv));
    }

    @Test
    public void testSay(
            @Mocked final SayNumberInQueueActionNode actionNode,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioFileNode preamble,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final QueuedCallStatus callStatus
            
    ) throws Exception 
    {
        final String key = LAST_SAYED_NUMBER+"_"+111;
        new Expectations() {{
            actionNode.getId(); result = 111;
            bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING); result = callStatus;
            bindings.get(key); result = 5;
            callStatus.isQueueing(); result = true;
            callStatus.getSerialNumber(); result = 4;
            bindingSupport.putAll(bindings); 
            actionNode.getAcceptSayNumber(); result = true;
            bindings.put(key, 4);
            bindingSupport.reset();
        }};
        SayNumberInQueueAction action = new SayNumberInQueueAction(actionNode, bindingSupport, null, 10, preamble, null);
        List res = action.formWords(conv);
        assertNotNull(res);
        assertEquals(1, res.size());
        assertTrue(res.get(0) instanceof List);
        assertArrayEquals(new Object[]{preamble, "4"}, ((List)res.get(0)).toArray());
    }

//    @Before
//    public void prepare() throws Exception
//    {
//        resourceManager = registry.getService(ResourceManager.class);
//        ResourcesNode resources = (ResourcesNode) tree.getRootNode().getChildren(ResourcesNode.NAME);
//        resources.setDefaultLocale(new Locale("ru"));
//        
//        executor = new ExecutorServiceNode();
//        executor.setName("executor");
//        tree.getRootNode().addAndSaveChildren(executor);
//        executor.setCorePoolSize(20);
//        executor.setMaximumPoolSize(20);
//        assertTrue(executor.start());
//
//        conv = new TestEndpointConversationNode();
//        conv.setName("endpoint");
//        tree.getRootNode().addAndSaveChildren(conv);
//        conv.setExecutorService(executor);
//        conv.setFileName("target/number_in_queue.wav");
////        assertTrue(conv.start());
//
////        numbers = SayAmountActionTest.createNumbersNode(tree);
//        numbers = resourceManager.getResource(Constants.NUMBERS_FEMALE_RESOURCE, null);
//        assertNotNull(numbers);
//
//        preamble = new AudioFileNode();
//        preamble.setName("preamble");
//        tree.getRootNode().addAndSaveChildren(preamble);
//        preamble.getAudioFile().setDataStream(FileUtils.openInputStream(
//                new File("src/test/wav/number_in_queue.wav")));
//        assertTrue(preamble.start());
//
//        owner = new SayNumberInQueueActionNode();
//        owner.setName("say number in queue");
//        tree.getRootNode().addAndSaveChildren(owner);
////        assertTrue(owner.start());
//    }

//    @Test(timeout=15000)
//    public void test() throws Exception
//    {
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        BindingSupport bindingSupport = createMock(BindingSupport.class);
//        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);
//
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
//        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_"+owner.getId();
//        expect(bindings.get(key)).andReturn(null);
//        expect(bindings.put(key, 10)).andReturn(null);
//        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
//        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
//        bindingSupport.putAll(bindings);
//        expectLastCall().atLeastOnce();
//        bindingSupport.reset();
//
//        replay(state, bindings, callStatus, bindingSupport);
//
//        conv.setConversationScenarioState(state);
//        assertTrue(conv.start());
//        SayNumberInQueueAction action =
//                new SayNumberInQueueAction(owner, bindingSupport, numbers, 50, preamble, resourceManager);
//        action.execute(conv, null, logger);
//        Thread.sleep(10000);
//        
//        verify(state, bindings, callStatus, bindingSupport);
//    }
//
////    @Test
//    public void formWords() throws Exception
//    {
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);
//        BindingSupport bindingSupport = createMock(BindingSupport.class);
//
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
//        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_"+owner.getId();
//        expect(bindings.get(key)).andReturn(10);
//        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
//        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
//
//        replay(state, bindings, callStatus, bindingSupport);
//
//        conv.setConversationScenarioState(state);
//        SayNumberInQueueAction action = new SayNumberInQueueAction(owner, bindingSupport, numbers, 50, 
//            preamble, resourceManager);
//        assertNull(action.formWords(conv));
//
//        verify(state, bindings, callStatus, bindingSupport);
//    }
//
////    @Test
//    public void formWords2() throws Exception
//    {
//        ConversationScenarioState state = createMock(ConversationScenarioState.class);
//        Bindings bindings = createMock(Bindings.class);
//        QueuedCallStatus callStatus = createMock(QueuedCallStatus.class);
//        BindingSupport bindingSupport = createMock(BindingSupport.class);
//
//        expect(state.getBindings()).andReturn(bindings);
//        expect(bindings.get(QueueCallAction.QUEUED_CALL_STATUS_BINDING)).andReturn(callStatus);
//        String key = SayNumberInQueueAction.LAST_SAYED_NUMBER+"_"+owner.getId();
//        expect(bindings.get(key)).andReturn(null);
//        expect(bindings.put(key, 10)).andReturn(null);
//        expect(callStatus.isQueueing()).andReturn(Boolean.TRUE);
//        expect(callStatus.getSerialNumber()).andReturn(10).anyTimes();
//        bindingSupport.putAll(bindings);
//        bindingSupport.reset();
//
//        replay(state, bindings, callStatus, bindingSupport);
//
//        conv.setConversationScenarioState(state);
//        SayNumberInQueueAction action = new SayNumberInQueueAction(owner, bindingSupport, numbers, 50, 
//            preamble, resourceManager);
//        List words = action.formWords(conv);
//        assertNotNull(words);
//        assertEquals(2, words.size());
//        assertArrayEquals(new Object[]{preamble, "10"}, words.toArray());
//
//        verify(state, bindings, callStatus, bindingSupport);
//    }


}