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

import fj.data.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.conference.Conference;
import org.onesec.raven.ivr.conference.ConferenceException;
import static org.onesec.raven.ivr.conference.ConferenceException.*;
import org.onesec.raven.ivr.conference.ConferenceInitiator;
import static org.onesec.raven.ivr.conference.impl.ConferenceManagerNode.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
/**
 *
 * @author Mikhail Titov
 */
public class ConferenceManagerNodeTest extends RavenCoreTestCase {
    private static List<Conference> emptyList = List.nil();
    private IMocksControl mocks;
    private ConferenceManagerNode manager;
    
    @Before
    public void prepare() {
        mocks = null;
        manager = new ConferenceManagerNode();
        manager.setName("Conference manager");
        testsNode.addAndSaveChildren(manager);
        manager.setChannelsCount(10);
    }
    
    @After
    public void finish() {
        if (mocks!=null)
            mocks.verify();
    }
    
    @Test
    public void isDatesIntersectsTest() {
        assertTrue(isDatesIntersects(1, 3, 0, 4));
        assertTrue(isDatesIntersects(0, 4, 1, 3));
        assertTrue(isDatesIntersects(1, 3, 1, 4));
        assertTrue(isDatesIntersects(1, 3, 3, 4));
        assertTrue(isDatesIntersects(1, 3, 0, 3));
        assertTrue(isDatesIntersects(1, 3, 0, 1));
        assertFalse(isDatesIntersects(1, 3, 4, 5));
        assertFalse(isDatesIntersects(4, 5, 1, 3));
    }
    
    @Test
    public void getIntersectionTest() {
        assertNull(getIntersection(1, 4, 5, 7));
        assertNull(getIntersection(5, 7, 1, 4));
        assertArrayEquals(new long[]{3, 5}, getIntersection(0, 7, 3, 5));
        assertArrayEquals(new long[]{3, 5}, getIntersection(0, 5, 3, 5));
        assertArrayEquals(new long[]{3, 5}, getIntersection(3, 7, 3, 5));
        assertArrayEquals(new long[]{3, 5}, getIntersection(3, 5, 0, 7));
        assertArrayEquals(new long[]{3, 5}, getIntersection(3, 5, 0, 5));
        assertArrayEquals(new long[]{3, 5}, getIntersection(3, 5, 3, 7));
    }
    
    @Test
    public void checkIntervalOnEmptyList() {
        assertTrue(checkChannels(0, 1, emptyList, 1, 1, null));
    }
    
    @Test
    public void checkIntervalTest1() {
        assertFalse(checkChannels(0, 1, emptyList, 1, 2, null));
    }
    
    @Test
    public void checkIntervalTest2() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2), createConference(2, 3));
        mocks.replay();
        assertTrue(checkChannels(4, 5, list, 1, 1, null));
    }
    
    @Test
    public void checkIntervalTest3() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2, 2), createConference());
        mocks.replay();
        assertFalse(checkChannels(0, 1, list, 2, 1, null));
    }
    
    @Test
    public void checkIntervalTest4() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2, 1), createConference(2, 3));
        mocks.replay();
        assertTrue(checkChannels(0, 1, list, 2, 1, null));
    }
    
    @Test
    public void checkIntervalTest5() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2, 1), createConference(1, 3, 1), 
                createConference());
        mocks.replay();
        assertFalse(checkChannels(0, 1, list, 2, 1, null));
    }
    
    @Test
    public void checkIntervalTest6() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2, 1), createConference(1, 3, 1), 
                createConference(2,3));
        mocks.replay();
        assertTrue(checkChannels(0, 1, list, 3, 1, null));
    }
    
    @Test
    public void generateAccessCodeTest() {
        String code = generateAccessCode(4);
        assertNotNull(code);
        assertEquals(4, code.length());
//        for (int i=0; i<100; ++i)
//            System.out.println("    !!! access code: "+generateAccessCode(5));
    }
    
    @Test
    public void initNodesTest() {
        assertNotNull(manager.getConferencesArchiveNode());
        assertNotNull(manager.getPlannedConferencesNode());
    }
    
    @Test
    public void checkCreateConferenceExceptions() {
        manager.setMaxConferenceDuration(1);
        manager.setMaxPlanDays(1);
        assertTrue(manager.start());
        checkCreateConferenceException(NULL_CONFERENCE_NAME, null, null, null, 0, null);
        checkCreateConferenceException(NULL_FROM_DATE, "name", null, null, 0, null);
        checkCreateConferenceException(NULL_TO_DATE, "name", new Date(), null, 0, null);
        checkCreateConferenceException(INVALID_CHANNELS_COUNT, "name", new Date(), new Date(), 0, null);
        checkCreateConferenceException(FROM_DATE_AFTER_TO_DATE, "name", new Date(), new Date(System.currentTimeMillis()-1000), 10, null);
        checkCreateConferenceException(DATE_AFTER_CURRENT_DATE, "name", new Date(System.currentTimeMillis()-1000), new Date(), 10, null);
        Date fd = new Date(System.currentTimeMillis()+10000);
        checkCreateConferenceException(CONFERENCE_TO_LONG, "name", fd, new Date(fd.getTime()+61*1000), 10, null);
        fd = new Date(System.currentTimeMillis()+TimeUnit.DAYS.toMillis(2));
        checkCreateConferenceException(CONFERENCE_TO_FAR_IN_FUTURE, "name", fd, new Date(fd.getTime()+30*1000), 10, null);
    }
    
    @Test
    public void createConferenceTest() throws Exception {
        manager.setChannelsCount(10);
        manager.setMinChannelsPerConference(5);
        assertTrue(manager.start());
        long time = (System.currentTimeMillis()+10000)/1000*1000;
        Date fd = new Date(time);
        Date td = new Date(fd.getTime()+60*1000);
        Conference conf = manager.createConference("c1", fd, td, 5, null);
        assertNotNull(conf);
        assertTrue(((Node)conf).isStarted());
        assertEquals("c1", conf.getConferenceName());
        assertEquals(new Integer(5), conf.getChannelsCount());
        assertEquals(fd, conf.getStartTime());
        assertEquals(td, conf.getEndTime());
//        assertNot
    }
    
    @Test
    public void createConferenceNodeTest() throws Exception {
        manager.setChannelsCount(10);
        manager.setMinChannelsPerConference(5);
        assertTrue(manager.start());
        long time = (System.currentTimeMillis()+10000)/1000*1000;
        Date fd = new Date(time);
        Date td = new Date(fd.getTime()+60*1000);
        
        ConferenceNode conf = createConferenceNode("c1", fd, td, 11);
        assertFalse(conf.start());
        conf.setChannelsCount(4);
        assertFalse(conf.start());
        conf.setChannelsCount(5);
        assertTrue(conf.start());
        
        ConferenceNode conf2 = createConferenceNode("c2", fd, td, 6);
        assertFalse(conf2.start());
        conf2.setChannelsCount(5);
        assertTrue(conf2.start());
        
        ConferenceNode conf3 = createConferenceNode("c3", fd, td, 5);
        assertFalse(conf3.start());
        conf3.setStartTime(new Date(time+61000));
        conf3.setEndTime(new Date(time+121000));
        assertTrue(conf3.start());
    }
    
    private Conference createConference() {
        return mocks.createMock(Conference.class);
    }
    
    private Conference createConference(long fd, long td) {
        Conference conf = mocks.createMock(Conference.class);
        expect(conf.getStartTime()).andReturn(new Date(fd)).atLeastOnce();
        expect(conf.getEndTime()).andReturn(new Date(td)).atLeastOnce();
        return conf;
    }
    
    private Conference createConference(long fd, long td, int channelsCount) {
        Conference conf = createConference(fd, td);
        expect(conf.getChannelsCount()).andReturn(channelsCount).atLeastOnce();
        return conf;
    }
    
    private ConferenceNode createConferenceNode(String name, Date fd, Date td, int channelsCount) {
        ConferenceNode conf = new ConferenceNode();
        conf.setName(name);
        manager.getPlannedConferencesNode().addAndSaveChildren(conf);
        conf.setConferenceName(name);
        conf.setStartTime(fd);
        conf.setEndTime(td);
        conf.setChannelsCount(channelsCount);
        conf.setAccessCode(generateAccessCode(5));
        return conf;
    }
    
    private void checkCreateConferenceException(int causeCode, String name, Date fd, Date td, int channelsCount, 
            ConferenceInitiator initiator) 
    {
        try {
            manager.createConference(name, fd, td, channelsCount, initiator);
            fail();
        } catch (ConferenceException e) {
            assertEquals(causeCode, e.getCauseCode());
        }
    }
}
