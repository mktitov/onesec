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

package org.onesec.raven.ivr.impl;

import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.onesec.raven.ivr.actions.TransferCallActionNode;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.impl.ConversationScenarioNode;
import org.raven.conv.impl.ConversationScenarioPointNode;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointNodeTest 
        extends OnesecRavenTestCase implements ConversationCompletionCallback
{
    private IvrEndpointNode endpoint;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;

    @Before
    public void prepare()
    {
        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("88013 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88013);
        provider.setToNumber(88024);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(10);
        assertTrue(executor.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        endpoint = new IvrEndpointNode();
        endpoint.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(endpoint);
        endpoint.setExecutorService(executor);
        endpoint.setConversationScenario(scenario);
        endpoint.setAddress("88013");
        endpoint.setIp("10.50.1.134");
        endpoint.setLogLevel(LogLevel.TRACE);
    }

//    @Test(timeout=10000)
//    @Test
    public void startStopTest() throws Exception
    {
        waitForProvider();
        System.out.println("Starting...");
        assertTrue(endpoint.start());
        assertEquals(IvrEndpointState.OUT_OF_SERVICE, endpoint.getEndpointState().getId());
        System.out.println("Waiting for IN_SERVICE state");
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        assertFalse(res.isWaitInterrupted());
        System.out.println("Stoping...");
        endpoint.stop();
        System.out.println("Waiting for OUT_OF_SERVICE state");
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.OUT_OF_SERVICE}, 2000);
        assertFalse(res.isWaitInterrupted());
    }

//    @Test
    public void simpleConversationTest() throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName("audio file");
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream("src/test/wav/test.wav");
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());

        PlayAudioActionNode playAudioActionNode = new PlayAudioActionNode();
        playAudioActionNode.setName("Play audio");
        scenario.addAndSaveChildren(playAudioActionNode);
        playAudioActionNode.setAudioFile(audioFileNode);
        assertTrue(playAudioActionNode.start());

        StopConversationActionNode stopConversationActionNode = new StopConversationActionNode();
        stopConversationActionNode.setName("stop conversation");
        scenario.addAndSaveChildren(stopConversationActionNode);
        assertTrue(stopConversationActionNode.start());
        
        waitForProvider();
        assertTrue(endpoint.start());
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.ACCEPTING_CALL}, 20000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.TALKING}, 5000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 5000);
        Thread.sleep(1000);
    }

//    @Test
    public void simpleConversationTest2() throws Exception
    {
        AudioFileNode audioNode1 = createAudioFileNode("audio1", "src/test/wav/test2.wav");
        AudioFileNode audioNode2 = createAudioFileNode("audio2", "src/test/wav/test.wav");

        IfNode ifNode1 = createIfNode("if1", scenario, "dtmf=='1'||repetitionCount==3");
        IfNode ifNode2 = createIfNode("if2", scenario, "dtmf=='-'||dtmf=='#'");
        createPlayAudioActionNode("hello", ifNode2, audioNode1);
        createPauseActionNode(ifNode2, 5000l);
        createGotoNode("replay", ifNode2, scenario);
        createPlayAudioActionNode("bye", ifNode1, audioNode2);

        StopConversationActionNode stopConversationActionNode = new StopConversationActionNode();
        stopConversationActionNode.setName("stop conversation");
        ifNode1.addAndSaveChildren(stopConversationActionNode);
        assertTrue(stopConversationActionNode.start());

        waitForProvider();
        assertTrue(endpoint.start());
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.ACCEPTING_CALL}, 200000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.TALKING}, 250000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 50000);

    }

//    @Test
    public void inviteTest() throws Exception
    {
        AudioFileNode audioNode1 = createAudioFileNode("audio1", "src/test/wav/test2.wav");
        AudioFileNode audioNode2 = createAudioFileNode("audio2", "src/test/wav/test.wav");

        IfNode ifNode1 = createIfNode("if1", scenario, "dtmf=='1'||repetitionCount==3");
        IfNode ifNode2 = createIfNode("if2", scenario, "dtmf=='-'||dtmf=='#'");
        createPlayAudioActionNode("hello", ifNode2, audioNode1);
        createPauseActionNode(ifNode2, 5000l);
        createGotoNode("replay", ifNode2, scenario);
        createPlayAudioActionNode("bye", ifNode1, audioNode2);

        StopConversationActionNode stopConversationActionNode = new StopConversationActionNode();
        stopConversationActionNode.setName("stop conversation");
        ifNode1.addAndSaveChildren(stopConversationActionNode);
        assertTrue(stopConversationActionNode.start());

        waitForProvider();
        assertTrue(endpoint.start());
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
//        endpoint.invite("089128672947", scenario, this);
        endpoint.invite("88024", scenario, this, null);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.INVITING}, 30000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.TALKING}, 250000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 50000);

    }

//    @Test
    public void transferTest() throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName("audio file");
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream("src/test/wav/test.wav");
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());

        PlayAudioActionNode playAudioActionNode = new PlayAudioActionNode();
        playAudioActionNode.setName("Play audio");
        scenario.addAndSaveChildren(playAudioActionNode);
        playAudioActionNode.setAudioFile(audioFileNode);
        assertTrue(playAudioActionNode.start());

        TransferCallActionNode transfer = new TransferCallActionNode();
        transfer.setName("transfer to 89128672947");
        scenario.addAndSaveChildren(transfer);
        transfer.setAddress("089128672947");
        assertTrue(transfer.start());

        waitForProvider();
        assertTrue(endpoint.start());
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.ACCEPTING_CALL}, 20000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.TALKING}, 5000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 5000);
        Thread.sleep(1000);
    }

    @Test
    public void inviteWithTransferTest() throws Exception
    {
        AudioFileNode audioNode1 = createAudioFileNode("audio1", "src/test/wav/test2.wav");

        createPlayAudioActionNode("hello", scenario, audioNode1);

        TransferCallActionNode transfer = new TransferCallActionNode();
        transfer.setName("transfer to 88024");
        scenario.addAndSaveChildren(transfer);
        NodeAttribute attr = transfer.getNodeAttribute("address");
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.setValue("tim_address");
        attr.save();
        transfer.setMonitorTransfer(Boolean.TRUE);
        assertTrue(transfer.start());

        waitForProvider();
        assertTrue(endpoint.start());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tim_address", "88024");
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        endpoint.invite("089128672947", scenario, this, params);
//        endpoint.invite("88024", scenario, this);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.INVITING}, 30000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.TALKING}, 250000);
        res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 50000);

    }

    private void createGotoNode(String name, Node owner, ConversationScenarioPoint point)
    {
        GotoNode gotoNode = new GotoNode();
        gotoNode.setName(name);
        owner.addAndSaveChildren(gotoNode);
        gotoNode.setConversationPoint(point);
        assertTrue(gotoNode.start());
    }

    private ConversationScenarioPointNode createConversationPoint(
            String name, ConversationScenarioPoint nextPoint, Node owner)
    {
        ConversationScenarioPointNode point = new ConversationScenarioNode();
        point.setName(name);
        owner.addAndSaveChildren(point);
        assertTrue(point.start());
        return point;
    }

    private AudioFileNode createAudioFileNode(String nodeName, String filename) throws Exception
    {
        AudioFileNode audioFileNode = new AudioFileNode();
        audioFileNode.setName(nodeName);
        tree.getRootNode().addAndSaveChildren(audioFileNode);
        FileInputStream is = new FileInputStream(filename);
        audioFileNode.getAudioFile().setDataStream(is);
        assertTrue(audioFileNode.start());
        return audioFileNode;
    }

    private PlayAudioActionNode createPlayAudioActionNode(
            String name, Node owner, AudioFileNode audioFileNode)
    {
        PlayAudioActionNode playAudioActionNode = new PlayAudioActionNode();
        playAudioActionNode.setName("Play audio");
        owner.addAndSaveChildren(playAudioActionNode);
        playAudioActionNode.setAudioFile(audioFileNode);
        assertTrue(playAudioActionNode.start());
        return playAudioActionNode;
    }

    private PauseActionNode createPauseActionNode(Node owner, Long interval)
    {
        PauseActionNode pauseAction = new PauseActionNode();
        pauseAction.setName("pause");
        owner.addAndSaveChildren(pauseAction);
        pauseAction.setInterval(interval);
        assertTrue(pauseAction.start());
        return pauseAction;
    }

    private IfNode createIfNode(String name, Node owner, String expression) throws Exception
    {
        IfNode ifNode = new IfNode();
        ifNode.setName(name);
        owner.addAndSaveChildren(ifNode);
        ifNode.setUsedInTemplate(Boolean.FALSE);
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue(expression);
        assertTrue(ifNode.start());
        return ifNode;
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 4000);
        assertFalse(res.isWaitInterrupted());
    }

    public void conversationCompleted(ConversationResult res)
    {
        System.out.println("\n-----------CONVERSATION RESULT-------------");
        System.out.println("Complition code: "+res.getCompletionCode());
        System.out.println("call start time: "+new Date(res.getCallStartTime()));
        System.out.println("call end time: "+new Date(res.getCallEndTime()));
        System.out.println("call duration (sec): "+res.getCallDuration());
        System.out.println("conversation start time: "+new Date(res.getConversationStartTime()));
        System.out.println("conversation duration (sec): "+res.getConversationDuration());
        System.out.println("\ntransfer completion code: "+res.getTransferCompletionCode());
        System.out.println("transfer address: "+res.getTransferAddress());
        System.out.println("transfer time: "+new Date(res.getTransferTime()));
        System.out.println("transfer conversation start time: "+new Date(res.getTransferConversationStartTime()));
        System.out.println("transfer conversation duration: "+res.getTransferConversationDuration());
        System.out.println("----------------------------------------------\n");
    }
}