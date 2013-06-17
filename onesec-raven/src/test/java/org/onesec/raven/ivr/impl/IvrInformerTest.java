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
import java.util.List;
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
import org.onesec.raven.ivr.IvrEndpointState;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.onesec.raven.ivr.actions.TransferCallActionNode;
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.impl.ConversationScenarioNode;
import org.raven.conv.impl.ConversationScenarioPointNode;
import org.raven.conv.impl.GotoNode;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.DataCollector;
import org.raven.test.DummyScheduler;
import org.raven.test.PushOnDemandDataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeError;

/**
 *
 * @author Mikhail Titov
 */
public class IvrInformerTest extends OnesecRavenTestCase
{
    private IvrEndpointNode endpoint;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private IvrInformerRecordSchemaNode schema;
    private PushOnDemandDataSource dataSource;
    private DataCollector dataCollector;
    private IvrInformer informer;
    private DummyScheduler startScheduler, stopScheduler;
    private RtpStreamManagerNode manager;

    @Before
    public void prepare()
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtpManager");
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());

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

        startScheduler = new DummyScheduler();
        startScheduler.setName("startScheduler");
        tree.getRootNode().addAndSaveChildren(startScheduler);
        assertTrue(startScheduler.start());

        stopScheduler = new DummyScheduler();
        stopScheduler.setName("stopScheduler");
        tree.getRootNode().addAndSaveChildren(stopScheduler);
        assertTrue(stopScheduler.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        endpoint = new IvrEndpointNode();
        endpoint.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(endpoint);
        endpoint.setExecutor(executor);
//        endpoint.setConversationScenario(scenario);
        endpoint.setAddress("88013");
        endpoint.setRtpStreamManager(manager);
        endpoint.setLogLevel(LogLevel.TRACE);

        schema = new IvrInformerRecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        dataSource = new PushOnDemandDataSource();
        dataSource.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(dataSource);
        assertTrue(dataSource.start());

        informer = new IvrInformer();
        informer.setName("informer");
        tree.getRootNode().addAndSaveChildren(informer);
        informer.setConversationScenario(scenario);
        informer.setDataSource(dataSource);
        informer.setEndpoint(endpoint);
        informer.setLogLevel(LogLevel.DEBUG);
        
        dataCollector = new DataCollector();
        dataCollector.setName("dataCollector");
        tree.getRootNode().addAndSaveChildren(dataCollector);
        dataCollector.setDataSource(informer);
        assertTrue(dataCollector.start());
    }

    @Test()
    public void test() throws Exception
    {
        createScenario();
        dataSource.addDataPortion(createRecord("abon1", "88024"));
        dataSource.addDataPortion(createRecord("abon1", "089128672947"));
        dataSource.addDataPortion(createRecord("abon2", "88024"));
        assertTrue(informer.start());
        informer.startProcessing();

        List recs = dataCollector.getDataList();
//        assertEquals(3, recs.size());
        printRecordsInformation(recs);
    }

//    @Test()
    public void maxCallDurationTest() throws Exception
    {
        createScenario();
        informer.setMaxCallDuration(30);
        dataSource.addDataPortion(createRecord("abon1", "88024"));
        dataSource.addDataPortion(createRecord("abon1", "089128672947"));
        dataSource.addDataPortion(createRecord("abon2", "88024"));
        assertTrue(informer.start());
        informer.startProcessing();

        List recs = dataCollector.getDataList();
//        assertEquals(3, recs.size());
        printRecordsInformation(recs);
    }

//    @Test()
    public void maxTriesTest() throws Exception
    {
        createScenario();
        dataSource.addDataPortion(createRecord("abon1", "88024"));
        dataSource.addDataPortion(createRecord("abon1", "089128672947"));
        Record rec = createRecord("abon2", "88027");
        rec.setValue(IvrInformerRecordSchemaNode.TRIES_FIELD, 1);
        dataSource.addDataPortion(rec);
        informer.setMaxTries((short)1);
        assertTrue(informer.start());
        informer.startProcessing();

        List recs = dataCollector.getDataList();
//        assertEquals(3, recs.size());
        printRecordsInformation(recs);
    }

//    @Test
    public void transferTest() throws Exception
    {
        createScenario2();
        dataSource.addDataPortion(createRecord("abon1", "089128672947"));
        dataSource.addDataPortion(createRecord("abon2", "089128672947"));
        assertTrue(informer.start());
        informer.startProcessing();

        List recs = dataCollector.getDataList();
//        assertEquals(3, recs.size());
        printRecordsInformation(recs);
    }

    private void printRecordsInformation(List recs) throws RecordException
    {
        for (Object recObj: recs)
        {
            Record rec = (Record) recObj;
            System.out.println("\n-------------RECORD--------------");
            if (rec==null)
                System.out.println("NULL");
            else
            {
                for (Map.Entry<String, Object> val: rec.getValues().entrySet())
                    System.out.println(val.getKey()+": "+val.getValue());
            }
            System.out.println("---------------------------------");
        }
    }

    private Record createRecord(String abonId, String abonNumber) throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue(IvrInformerRecordSchemaNode.ABONENT_ID_FIELD, abonId);
        rec.setValue(IvrInformerRecordSchemaNode.ABONENT_NUMBER_FIELD, abonNumber);
        return rec;
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

    private void createScenario() throws Exception, NodeError
    {
        AudioFileNode audioNode1 = createAudioFileNode("audio1", "src/test/wav/test2.wav");
        AudioFileNode audioNode2 = createAudioFileNode("audio2", "src/test/wav/test.wav");
        IfNode ifNode1 = createIfNode("if1", scenario, "dtmf=='1'||repetitionCount==1");
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
    }

    private void createScenario2() throws Exception, NodeError
    {
        AudioFileNode audioNode1 = createAudioFileNode("audio1", "src/test/wav/test2.wav");

        createPlayAudioActionNode("hello", scenario, audioNode1);

        TransferCallActionNode transfer = new TransferCallActionNode();
        transfer.setName("transfer to 88024");
        scenario.addAndSaveChildren(transfer);
        transfer.setAddress("88024");
        transfer.setMonitorTransfer(Boolean.TRUE);
        assertTrue(transfer.start());

        waitForProvider();
        assertTrue(endpoint.start());
        StateWaitResult res = endpoint.getEndpointState().waitForState(
                new int[]{IvrEndpointState.IN_SERVICE}, 2000);
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 10000);
        assertFalse(res.isWaitInterrupted());
    }
}