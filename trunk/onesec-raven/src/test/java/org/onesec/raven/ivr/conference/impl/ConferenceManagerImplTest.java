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
import org.easymock.IMocksControl;
import static org.easymock.EasyMock.*;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.onesec.raven.ivr.conference.Conference;
import org.raven.test.RavenCoreTestCase;
import static org.onesec.raven.ivr.conference.impl.ConferenceManagerImpl.*;
/**
 *
 * @author Mikhail Titov
 */
public class ConferenceManagerImplTest extends RavenCoreTestCase {
    private static List<Conference> emptyList = List.nil();
    private IMocksControl mocks;
    
    @Before
    public void prepare() {
        mocks = null;
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
        assertTrue(checkInterval(0, 1, emptyList, 1, 1));
    }
    
    @Test
    public void checkIntervalTest1() {
        assertFalse(checkInterval(0, 1, emptyList, 1, 2));
    }
    
    @Test
    public void checkIntervalTest2() {
        mocks = createControl();
        List<Conference> list = List.list(createConference(1, 2), createConference(2, 3));
        mocks.replay();
        assertTrue(checkInterval(4, 5, list, 1, 1));
    }
    
    private void traintMocks1() {
        
    }
    
    private Conference createConference(long fd, long td) {
        Conference conf = mocks.createMock(Conference.class);
        expect(conf.getStartTime()).andReturn(new Date(fd)).anyTimes();
        expect(conf.getEndTime()).andReturn(new Date(td)).anyTimes();
        return conf;
    }
}
