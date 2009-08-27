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
import org.junit.Before;
import org.junit.Test;
import org.onesec.core.StateWaitResult;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.ProviderRegistry;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointNodeTest extends OnesecRavenTestCase
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
        provider.setToNumber(88013);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(5);
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

    @Test
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
}