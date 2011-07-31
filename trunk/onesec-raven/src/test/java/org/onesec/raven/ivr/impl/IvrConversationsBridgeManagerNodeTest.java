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

package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.IvrConversationBridgeExeption;
import org.onesec.raven.ivr.IvrConversationsBridge;
import java.util.LinkedList;
import java.util.List;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.impl.CCMCallOperatorNode;
import org.onesec.raven.impl.ProviderNode;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.RtpStreamException;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class IvrConversationsBridgeManagerNodeTest extends OnesecRavenTestCase
{
    private static List<IvrEndpointConversationListener> conversationListeners;
    private static List<IncomingRtpStreamDataSourceListener> sourceListeners;

    @Before
    public void prepare()
    {
        conversationListeners = new LinkedList<IvrEndpointConversationListener>();
        sourceListeners = new LinkedList<IncomingRtpStreamDataSourceListener>();
    }


    @Test
    public void test() throws Exception
    {
        DataSource dataSource = new TestDataSource();
        ConversationMocks conv1Mocks = new ConversationMocks();
        trainConversation(conv1Mocks, "1");
        ConversationMocks conv2Mocks = new ConversationMocks();
        trainConversation(conv2Mocks, "2");

        IvrConversationsBridgeManagerNode manager = new IvrConversationsBridgeManagerNode();
        manager.setName("manager");
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());

        assertEquals(0, manager.getBridges().size());
        IvrConversationsBridge bridge = manager.createBridge(conv1Mocks.conv, conv2Mocks.conv);
        assertEquals(1, manager.getBridges().size());
        bridge.activateBridge();

//        for (IncomingRtpStreamDataSourceListener listener: sourceListeners)
//            listener.dataSourceCreated(dataSource);
        sourceListeners.get(0).streamClosing();
        assertEquals(0, manager.getBridges().size());


        conv1Mocks.verify();
        conv2Mocks.verify();
    }

    public void realTest() throws Exception
    {
        RtpStreamManagerNode manager = new RtpStreamManagerNode();
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
        provider.setName("88013-88014 provider");
        callOperator.getProvidersNode().addAndSaveChildren(provider);
        provider.setFromNumber(88013);
        provider.setToNumber(88037);
//        provider.setFromNumber(68050);
//        provider.setToNumber(68050);
        provider.setHost("10.16.15.1");
        provider.setPassword("cti_user1");
        provider.setUser("cti_user1");
        assertTrue(provider.start());

        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setMaximumPoolSize(15);
        executor.setCorePoolSize(10);
        assertTrue(executor.start());

        IvrConversationScenarioNode scenario = new IvrConversationScenarioNode();
        scenario.setName("scenario");
        tree.getRootNode().addAndSaveChildren(scenario);
        assertTrue(scenario.start());

        createPauseActionNode(scenario, 30000l);
        IvrEndpointNode endpoint1 = createEndpoint("88013", executor, manager, scenario);
//        endpoint1.
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

    private IvrEndpointNode createEndpoint(String number, ExecutorServiceNode executor
            , RtpStreamManagerNode rtpManager, IvrConversationScenarioNode scenario)
    {
        IvrEndpointNode endpoint = new IvrEndpointNode();
        endpoint.setName(number);
        tree.getRootNode().addAndSaveChildren(endpoint);
        endpoint.setExecutorService(executor);
        endpoint.setConversationScenario(scenario);
        endpoint.setAddress(number);
        endpoint.setRtpStreamManager(rtpManager);
        endpoint.setLogLevel(LogLevel.TRACE);
        
        return endpoint;
    }

    private void trainConversation(ConversationMocks mocks, String suffix) throws RtpStreamException
    {
        mocks.conv = createMock("conv"+suffix, IvrEndpointConversation.class);
        mocks.state = createMock("conv_state"+suffix, IvrEndpointConversationState.class);
        mocks.rtpStream = createMock("incoming_rtp"+suffix, IncomingRtpStream.class);
        mocks.audioStream = createMock("audio_stream"+suffix, AudioStream.class);

        expect(mocks.conv.getCallingNumber()).andReturn("num_"+suffix).anyTimes();
        mocks.conv.addConversationListener(checkConversationListener());
        expect(mocks.conv.getState()).andReturn(mocks.state);
        expect(mocks.state.getId()).andReturn(IvrEndpointConversationState.TALKING);
        expect(mocks.conv.getIncomingRtpStream()).andReturn(mocks.rtpStream);
        mocks.rtpStream.addDataSourceListener(
                checkDataSourceListener(), (ContentDescriptor) isNull(), (AudioFormat) isNull());
        expectLastCall().andReturn(true);
//        expect(mocks.conv.getAudioStream()).andReturn(mocks.audioStream);
//        mocks.audioStream.addSource(isA(DataSource.class));
        
        replay(mocks.conv, mocks.state, mocks.rtpStream, mocks.audioStream);
    }

    public static IvrEndpointConversationListener checkConversationListener()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                IvrEndpointConversationListener listener = (IvrEndpointConversationListener) arg;
                listener.listenerAdded();
                conversationListeners.add(listener);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    public static IncomingRtpStreamDataSourceListener checkDataSourceListener()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                sourceListeners.add((IncomingRtpStreamDataSourceListener)arg);
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }

    private class ConversationMocks {
        IvrEndpointConversation conv;
        IvrEndpointConversationState state;
        IncomingRtpStream rtpStream;
        AudioStream audioStream;

        public void replay(){
            EasyMock.replay(conv, state, rtpStream, audioStream);
        }

        public void verify(){
            EasyMock.verify(conv, state, rtpStream, audioStream);
        }
    }

}