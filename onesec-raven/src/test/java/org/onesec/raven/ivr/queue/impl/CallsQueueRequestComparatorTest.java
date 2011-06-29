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
package org.onesec.raven.ivr.queue.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.junit.Test;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class CallsQueueRequestComparatorTest extends Assert
{
    @Test
    public void diffPrioritiesTest()
    {
        CallQueueRequestWrapper req1 = createMock(CallQueueRequestWrapper.class);
        CallQueueRequestWrapper req2 = createMock(CallQueueRequestWrapper.class);
        
        expect(req1.getPriority()).andReturn(1).anyTimes();
        expect(req2.getPriority()).andReturn(2).anyTimes();
        
        replay(req1, req2);
        
        List reqs = Arrays.asList(req2, req1);
        Collections.sort(reqs, new CallsQueueRequestComparator());
        assertArrayEquals(new Object[]{req1, req2}, reqs.toArray());
        
        verify(req1, req2);
    }
    
    @Test
    public void samePrioritiesTest()
    {
        CallQueueRequestWrapper req1 = createMock(CallQueueRequestWrapper.class);
        CallQueueRequestWrapper req2 = createMock(CallQueueRequestWrapper.class);
        
        expect(req1.getPriority()).andReturn(1).anyTimes();
        expect(req1.getRequestId()).andReturn(1l).anyTimes();
        expect(req2.getPriority()).andReturn(1).anyTimes();
        expect(req2.getRequestId()).andReturn(2l).anyTimes();
        
        replay(req1, req2);
        
        List reqs = Arrays.asList(req2, req1);
        Collections.sort(reqs, new CallsQueueRequestComparator());
        assertArrayEquals(new Object[]{req1, req2}, reqs.toArray());
        reqs = Arrays.asList(req1, req2);
        Collections.sort(reqs, new CallsQueueRequestComparator());
        assertArrayEquals(new Object[]{req1, req2}, reqs.toArray());
        
        verify(req1, req2);
    }
}
