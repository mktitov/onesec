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

import java.util.List;
import java.util.LinkedList;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import static org.easymock.EasyMock.*;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
/**
 *
 * @author Mikhail Titov
 */
public class IvrConversationsBridgeImplTest extends OnesecRavenTestCase
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
    public void test() throws RtpStreamException
    {
        DataSource dataSource = new TestDataSource();
        ConversationMocks conv1Mocks = new ConversationMocks();
        trainConversation(conv1Mocks, "1", false);
        ConversationMocks conv2Mocks = new ConversationMocks();
        trainConversation(conv2Mocks, "2", true);

        BaseNode node = new BaseNode("owner");
        tree.getRootNode().addAndSaveChildren(node);
        node.setLogLevel(LogLevel.TRACE);
        assertTrue(node.start());
        IvrConversationsBridge br = new IvrConversationsBridgeImpl(
                conv1Mocks.conv, conv2Mocks.conv, node, null);

        IvrConversationsBridgeListener listener = createMock(IvrConversationsBridgeListener.class);
        IvrIncomingRtpStartedEvent rtpStartedEv = createStrictMock(IvrIncomingRtpStartedEvent.class);
        IncomingRtpStream inRtp = createMock(IncomingRtpStream.class);
        listener.bridgeActivated(br);
        listener.bridgeReactivated(br);
        listener.bridgeDeactivated(br);
        expect(rtpStartedEv.getConversation()).andReturn(conv1Mocks.conv);
        expect(rtpStartedEv.getIncomingRtpStream()).andReturn(conv1Mocks.rtpStream);
        expect(rtpStartedEv.getConversation()).andReturn(conv1Mocks.conv);
        expect(rtpStartedEv.getIncomingRtpStream()).andReturn(inRtp);
        expect(inRtp.addDataSourceListener(checkDataSourceListener(), (AudioFormat)isNull())).andReturn(true);

        replay(listener, rtpStartedEv, inRtp);

        br.addBridgeListener(listener);
        br.activateBridge();

        assertEquals(4, conversationListeners.size());
        assertEquals(2, sourceListeners.size());

        sourceListeners.get(0).dataSourceCreated(conv1Mocks.rtpStream, dataSource);
        sourceListeners.get(1).dataSourceCreated(conv2Mocks.rtpStream, dataSource);

        sourceListeners.clear();
        conversationListeners.get(0).incomingRtpStarted(rtpStartedEv);
        assertTrue(sourceListeners.isEmpty());
        conversationListeners.get(0).incomingRtpStarted(rtpStartedEv);
        assertEquals(1, sourceListeners.size());
        sourceListeners.get(0).dataSourceCreated(inRtp, dataSource);

        conversationListeners.get(0).conversationStopped(new IvrEndpointConversationStoppedEventImpl(
                conv1Mocks.conv, CompletionCode.COMPLETED_BY_ENDPOINT));

        verify(listener, rtpStartedEv, inRtp);
        
        conv1Mocks.verify();
        conv2Mocks.verify();

    }

    private void trainConversation(ConversationMocks mocks, String suffix, boolean addDsTwice) throws RtpStreamException
    {
        mocks.conv = createMock("conv"+suffix, IvrEndpointConversation.class);
        mocks.state = createMock("conv_state"+suffix, IvrEndpointConversationState.class);
        mocks.rtpStream = createMock("incoming_rtp"+suffix, IncomingRtpStream.class);
        mocks.audioStream = createMock("audio_stream"+suffix, AudioStream.class);

        expect(mocks.conv.getCallingNumber()).andReturn("num_"+suffix).anyTimes();
        expect(mocks.conv.getCalledNumber()).andReturn("!num_"+suffix).anyTimes();
        mocks.conv.addConversationListener(checkConversationListener(mocks.conv));
        expectLastCall().times(2);
        expect(mocks.conv.getState()).andReturn(mocks.state);
        expect(mocks.state.getId()).andReturn(IvrEndpointConversationState.TALKING);
        expect(mocks.conv.getIncomingRtpStream()).andReturn(mocks.rtpStream);
        mocks.rtpStream.addDataSourceListener(checkDataSourceListener(), (AudioFormat) isNull());
        expectLastCall().andReturn(true);
        expect(mocks.conv.getAudioStream()).andReturn(mocks.audioStream).times(1);
        mocks.audioStream.addSource(isA(DataSource.class));
//        expectLastCall().times(2);
        expectLastCall().times(addDsTwice?2:1);
        replay(mocks.conv, mocks.state, mocks.rtpStream, mocks.audioStream);
//        mocks.replay();
    }

    public static IvrEndpointConversationListener checkConversationListener(final IvrEndpointConversation conv)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object arg) {
                IvrEndpointConversationListener listener = (IvrEndpointConversationListener) arg;
                listener.listenerAdded(new IvrEndpointConversationEventImpl(conv));
                conversationListeners.add(listener);
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
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