/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.conference.impl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.sched.impl.ExecutorServiceNode;
import static org.easymock.EasyMock.*;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.After;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.conference.ConferenceSession;
import org.onesec.raven.ivr.conference.ConferenceSessionListener;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceNodeTest extends OnesecRavenTestCase {
    private ConferenceNode conference;
    private TestConferenceManager manager;
    private ExecutorServiceNode executor;
    private IMocksControl mocks;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(20);
        executor.setMaximumPoolSize(20);
        executor.setMaximumQueueSize(500);
        assertTrue(executor.start());
        
        manager = new TestConferenceManager();
        manager.setName("conference manager");
        testsNode.addAndSaveChildren(manager);
        assertTrue(manager.start());
        
        conference = new ConferenceNode();
        conference.setName("conference");
        manager.getPlannedConferencesNode().addAndSaveChildren(conference);
        conference.setConferenceName("super conference");
        conference.setChannelsCount(5);
        conference.setAccessCode("111");
        conference.setStartTime(new Date());
        conference.setEndTime(new Date(System.currentTimeMillis()+20000));
        conference.setExecutor(executor);
        conference.setNoiseLevel(0);
        conference.setMaxGainCoef(1);
    }
    
    @After
    public void finish() {
    }
    
//    @Test
    public void joinOnNotStartedTest() throws InterruptedException {
        mocks = createControl();
        ConferenceSessionListener listener = mocks.createMock(ConferenceSessionListener.class);
        IvrEndpointConversation conversation = mocks.createMock(IvrEndpointConversation.class);
        listener.conferenceNotActive();
        mocks.replay();
        
        conference.join(conversation, "xxx", listener);
        Thread.sleep(100);
        mocks.verify();
    }
    
//    @Test
    public void joinOnInvalidTimeTest() throws Exception {
        conference.setStartTime(new Date(System.currentTimeMillis()+1000));
        assertTrue(conference.start());
        joinOnNotStartedTest();
    }
    
//    @Test
    public void joinWithInvalidAccessCodeTest() throws Exception {
        assertTrue(conference.start());
        mocks = createControl();
        ConferenceSessionListener listener = mocks.createMock(ConferenceSessionListener.class);
        IvrEndpointConversation conversation = mocks.createMock(IvrEndpointConversation.class);
        listener.invalidAccessCode();
        mocks.replay();
        
        conference.join(conversation, "xxx", listener);
        Thread.sleep(100);
        mocks.verify();
    }
    
    @Test
    public void sessionCreatedAndConferenceStoppedTest() throws Exception {
        conference.setEndTime(new Date(System.currentTimeMillis()+3000));
        assertTrue(conference.start());
        mocks = createControl();
        ConferenceSessionListener listener = mocks.createMock(ConferenceSessionListener.class);
        IvrEndpointConversation conversation = trainConversation();
        listener.sessionCreated(isA(ConferenceSession.class));
        listener.conferenceStopped();
        final AtomicLong stopTime = new AtomicLong();
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                stopTime.set(System.currentTimeMillis());
                return null;
            }
        });
        mocks.replay();
        conference.join(conversation, "111", listener);
        Thread.sleep(3100);
        mocks.verify();
        assertTrue(stopTime.get()>=conference.getEndTime().getTime());
    }
    
    private IvrEndpointConversation trainConversation() {
        IvrEndpointConversation conversation = mocks.createMock(IvrEndpointConversation.class);
        expect(conversation.getCallingNumber()).andReturn("12345").anyTimes();
        return conversation;
    }
}
