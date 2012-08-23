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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
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
import org.raven.conv.ConversationScenarioPoint;
import org.raven.conv.impl.GotoNode;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.PushOnDemandDataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class AsyncIvrInformerTest extends OnesecRavenTestCase
{
    private IvrEndpointPoolNode pool;
    private ExecutorServiceNode executor;
    private IvrConversationScenarioNode scenario;
    private IvrInformerRecordSchemaNode schema;
    private PushDataSource dataSource;
    private PushOnDemandDataSource pullDataSource;
    private DataCollector dataCollector;
    private AsyncIvrInformer informer;
    private RtpStreamManagerNode manager;

    @Before
    public void prepare() throws Exception
    {
        manager = new RtpStreamManagerNode();
        manager.setName("rtpManager");
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());

        RtpAddressNode address = new RtpAddressNode();
        address.setName(getInterfaceAddress().getHostAddress());
        manager.addAndSaveChildren(address);
        address.setStartingPort(18384);
        assertTrue(address.start());

        CCMCallOperatorNode callOperator = new CCMCallOperatorNode();
        callOperator.setName("call operator");
        tree.getRootNode().addAndSaveChildren(callOperator);
        assertTrue(callOperator.start());

        ProviderNode provider = new ProviderNode();
        provider.setName("631609 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88000);
        provider.setToNumber(631799);
        provider.setHost("10.0.137.125");
        provider.setPassword(privateProperties.getProperty("ccm_dialer_proxy_kom"));
        provider.setUser("ccm_dialer_proxy_kom");
        assertTrue(provider.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(50);
        executor.setCorePoolSize(50);
        assertTrue(executor.start());

        scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        pool = new IvrEndpointPoolNode();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        pool.setExecutor(executor);
        pool.setLogLevel(LogLevel.TRACE);
//        assertTrue(pool.start());

//        createEndpoint("88013", 1234);
        createEndpoint("631799", 1234);

        schema = new IvrInformerRecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        dataSource = new PushDataSource();
        dataSource.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(dataSource);
        assertTrue(dataSource.start());

        pullDataSource = new PushOnDemandDataSource();
        pullDataSource.setName("pullDataSource");
        tree.getRootNode().addAndSaveChildren(pullDataSource);
        assertTrue(pullDataSource.start());

        informer = new AsyncIvrInformer();
        informer.setName("informer");
        tree.getRootNode().addAndSaveChildren(informer);
        informer.setConversationScenario(scenario);
        informer.setDataSource(dataSource);
        informer.setEndpointPool(pool);
        informer.setLogLevel(LogLevel.DEBUG);
        informer.setMaxSessionsCount(1);
        informer.setEndpointWaitTimeout(2000);
        informer.setRecordSchema(schema);
        TimeWindowNode timeWindow = new TimeWindowNode();
        timeWindow.setName("timeWindow");
        informer.addAndSaveChildren(timeWindow);
        timeWindow.setTimePeriods("0-23");
        assertTrue(timeWindow.start());

        dataCollector = new DataCollector();
        dataCollector.setName("dataCollector");
        tree.getRootNode().addAndSaveChildren(dataCollector);
        dataCollector.setDataSource(informer);
        assertTrue(dataCollector.start());
    }

    @After
    public void afterTest() {
        for (Node node: pool.getChildrens())
            node.stop();
    }

//    @Test(timeout=60000)
    public void tooManySessionsTest() throws Exception
    {
        assertTrue(pool.start());
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88027"));
        dataSource.pushData(createRecord(2, "abon1", "089128672947"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertEquals(
                AsyncIvrInformer.ERROR_TOO_MANY_SESSIONS
                , recs.get(2l).getValue(COMPLETION_CODE_FIELD));
    }

//    @Test(timeout=60000)
    public void tooManySessionsTest2() throws Exception
    {
        assertTrue(pool.start());
        informer.setWaitForSession(Boolean.TRUE);
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88024"));
        dataSource.pushData(createRecord(2, "abon1", "88027"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertTrue((Long)recs.get(2l).getValue(CONVERSATION_DURATION_FIELD)>0);
        printRecordsInformation(dataList);
    }

//    @Test(timeout=60000)
    public void noFreeTerminalTest() throws Exception
    {
        assertTrue(pool.start());
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88027"));
        dataSource.pushData(createRecord(2, "abon1", "88024"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertEquals(
                AsyncIvrInformer.ERROR_NO_FREE_ENDPOINT_IN_THE_POOL
                , recs.get(2l).getValue(COMPLETION_CODE_FIELD));
    }

    @Test(timeout=60000)
    public void alreadyInformingTest() throws Exception {
        createScenario();
        assertTrue(pool.start());
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88027"));
        dataSource.pushData(createRecord(2, "abon1", "88027"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        printRecordsInformation(dataList);
        Map<Long, Record> recs = getRecords(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertEquals(AsyncIvrInformer.ALREADY_INFORMING, recs.get(2l).getValue(COMPLETION_CODE_FIELD));
    }

//    @Test(timeout=60000)
    public void asyncTest() throws Exception {
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        createEndpoint("88014", 1236);
        assertTrue(pool.start());
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88024"));
        dataSource.pushData(createRecord(2, "abon1", "88027"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertTrue((Long)recs.get(2l).getValue(CONVERSATION_DURATION_FIELD)>0);
    }

    //Необходимо ответить на первый вызов. Второго вызова быть не должно
//    @Test(timeout=60000)
    public void groupTest() throws Exception {
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        informer.setGroupField(IvrInformerRecordSchemaNode.ABONENT_ID_FIELD);
        
        createEndpoint("88014", 1236);
        assertTrue(pool.start());
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88024"));
        dataSource.pushData(createRecord(2, "abon1", "88027"));
        dataSource.pushData(null);

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertEquals(AsyncIvrInformer.SKIPPED_STATUS
                , recs.get(2l).getValue(IvrInformerRecordSchemaNode.COMPLETION_CODE_FIELD));
    }

//    @Test(timeout=60000)
    public void startProcessingTest() throws Exception {
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        informer.setDataSource(pullDataSource);
        informer.setGroupField(IvrInformerRecordSchemaNode.ABONENT_ID_FIELD);

        createEndpoint("88014", 1236);
        assertTrue(pool.start());
        createScenario();
        assertTrue(informer.start());
        pullDataSource.addDataPortion(createRecord(1, "abon1", "88024"));
        pullDataSource.addDataPortion(createRecord(2, "abon1", "88027"));
        pullDataSource.addDataPortion(null);

        new Thread(){
            @Override public void run() {
                informer.startProcessing();
            }
        }.start();

        Thread.sleep(1500);
//        assertEquals(IvrInformerStatus.PROCESSING, informer.getInformerStatus());

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
        assertNotNull(recs.get(2l));
        assertEquals(AsyncIvrInformer.SKIPPED_STATUS, recs.get(2l).getValue(IvrInformerRecordSchemaNode.COMPLETION_CODE_FIELD));
    }

//    @Test(timeout=60000)
    public void startProcessingOnNotEmptySessionsTest() throws Exception {
        assertTrue(pool.start());
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        informer.setDataSource(pullDataSource);

        createScenario();
        assertTrue(informer.start());
        pullDataSource.addDataPortion(createRecord(1, "abon1", "88027"));
        pullDataSource.addDataPortion(null);

        Thread startProcessing = new Thread(){
            @Override
            public void run() {
                informer.startProcessing();
            }
        };
        
        Thread startProcessing2 = new Thread(){
            @Override
            public void run() {
                informer.startProcessing();
            }
        };

        startProcessing.start();
        Thread.sleep(500);
        startProcessing2.start();
        Thread.sleep(500);
//        assertEquals(IvrInformerStatus.PROCESSING, informer.getInformerStatus());

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(2, dataList.size());
        assertEquals(1, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
    }

//    @Test(timeout=60000)
    public void numberTranslatingTest() throws Exception
    {
        assertTrue(pool.start());
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        informer.setDataSource(pullDataSource);
        informer.setUseNumberTranslation(Boolean.TRUE);
        informer.setNumberTranslation("number.replace('x',record['ABONENT_ID']).replace('y','7')");

        createScenario();
        assertTrue(informer.start());
        pullDataSource.addDataPortion(createRecord(1, "2", "880xy"));
        pullDataSource.addDataPortion(null);

        new Thread(){
            @Override
            public void run() {
                informer.startProcessing();
            }
        }.start();

        Thread.sleep(500);

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(2, dataList.size());
        assertEquals(1, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
    }

//    @Test(timeout=60000)
    public void stopProcessingTest() throws Exception {
        informer.setWaitForSession(Boolean.FALSE);
        informer.setMaxSessionsCount(2);
        informer.setDataSource(pullDataSource);
        informer.setGroupField(IvrInformerRecordSchemaNode.ABONENT_ID_FIELD);

        createEndpoint("88014", 1236);
        assertTrue(pool.start());
        createScenario();
        assertTrue(informer.start());
        pullDataSource.addDataPortion(createRecord(1, "abon1", "88024"));
        pullDataSource.addDataPortion(createRecord(2, "abon1", "88027"));
        pullDataSource.addDataPortion(null);

        new Thread(){
            @Override public void run() {
                informer.startProcessing();
            }
        }.start();
        Thread.sleep(500);
        
        new Thread(){
            @Override public void run() {
                informer.stopProcessing();
            }
        }.start();

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(2, dataList.size());
        assertEquals(1, recs.size());
        assertNotNull(recs.get(1l));
        assertTrue((Long)recs.get(1l).getValue(CONVERSATION_DURATION_FIELD)>0);
    }

    //Не нужно брать трубку на первый вызов
//    @Test
    public void inviteTimeoutTest() throws Exception {
        assertTrue(pool.start());
        informer.setWaitForSession(Boolean.TRUE);
        informer.setMaxSessionsCount(1);
        informer.setMaxInviteDuration(20);
        createScenario();
        assertTrue(informer.start());
        dataSource.pushData(createRecord(1, "abon1", "88024"));
        dataSource.pushData(createRecord(2, "abon1", "88027"));

        while (informer.getSessionsCount()>0)
            TimeUnit.MILLISECONDS.sleep(500);

        List dataList = dataCollector.getDataList();
        Map<Long, Record> recs = getRecords(dataList);
        printRecordsInformation(dataList);
        assertEquals(4, dataList.size());
        assertEquals(2, recs.size());
        assertNotNull(recs.get(1l));
        assertEquals(AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS, recs.get(1l).getValue(IvrInformerRecordSchemaNode.COMPLETION_CODE_FIELD));
        assertNotNull(recs.get(2l));
        assertTrue((Long)recs.get(2l).getValue(CONVERSATION_DURATION_FIELD)>0);
    }
    
    private Map<Long, Record> getRecords(List list) throws RecordException
    {
        Map<Long, Record> recs = new HashMap<Long, Record>();
        if (list!=null)
            for (Object data: list)
                if (data instanceof Record)
                    recs.put((Long)((Record) data).getValue(ID_FIELD), (Record) data);
        return recs;
    }

    private void createEndpoint(String address, int port)
    {
        IvrEndpointNode endpoint = new IvrEndpointNode();
        endpoint.setName(address);
        pool.addAndSaveChildren(endpoint);
        endpoint.setRtpStreamManager(manager);
        endpoint.setExecutor(executor);
        endpoint.setConversationScenario(scenario);
        endpoint.setAddress(address);
        endpoint.setLogLevel(LogLevel.TRACE);
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

    private Record createRecord(long id, String abonId, String abonNumber) throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue(IvrInformerRecordSchemaNode.ABONENT_ID_FIELD, abonId);
        rec.setValue(IvrInformerRecordSchemaNode.ABONENT_NUMBER_FIELD, abonNumber);
        rec.setValue(IvrInformerRecordSchemaNode.ID_FIELD, id);
        return rec;
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
//        startEndpoints();
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
    
    private void createGotoNode(String name, Node owner, ConversationScenarioPoint point)
    {
        GotoNode gotoNode = new GotoNode();
        gotoNode.setName(name);
        owner.addAndSaveChildren(gotoNode);
        gotoNode.setConversationPoint(point);
        assertTrue(gotoNode.start());
    }

    private void waitForProvider() throws Exception
    {
        ProviderRegistry providerRegistry = registry.getService(ProviderRegistry.class);
        assertNotNull(providerRegistry);
        Thread.sleep(100);
        ProviderController provider = providerRegistry.getProviderControllers().iterator().next();
        assertNotNull(provider);
        StateWaitResult res = provider.getState().waitForState(
                new int[]{ProviderControllerState.IN_SERVICE}, 50000);
        assertFalse(res.isWaitInterrupted());
    }

    private void startEndpoints() throws Exception
    {
        for (Node child: pool.getChildrens())
        {
            IvrEndpointNode endpoint = (IvrEndpointNode) child;
            assertTrue(endpoint.start());
            StateWaitResult res = endpoint.getEndpointState().waitForState(
                    new int[]{IvrEndpointState.IN_SERVICE}, 2000);
        }
    }
}