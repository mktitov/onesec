/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.actions;

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class TimeWindowsGroupActionNodeTest extends OnesecRavenTestCase {
    
    private TimeWindowsGroupActionNode group;
    
    @Before
    public void prepare() {
        group = new TimeWindowsGroupActionNode();
        group.setName("group");
        tree.getRootNode().addAndSaveChildren(group);
        assertTrue(group.start());
    }
    
    @Test
    public void timeWindowsNodeExistens() {
        Node node = group.getChildren(TimeWindowsNode.NAME);
        assertNotNull(node);
        assertTrue(node instanceof TimeWindowsNode);
    }
    
    @Test
    public void checkCurrentTimeInPeriodWithoutTimeWindows() {
        assertNull(group.getEffectiveChildrens());
        group.setInvertResult(Boolean.TRUE);
        assertNotNull(group.getEffectiveChildrens());
    }
    
    @Test
    public void checkCurrentTimeInPeriodWithTimeWindowNodes() {
        TimeWindowNode w1 = new TimeWindowNode();
        w1.setName("w1");
        group.getTimeWindows().addAndSaveChildren(w1);
        w1.setTimePeriods("00:00-23:59");
        assertNull(group.getEffectiveChildrens());
        
        assertTrue(w1.start());
        assertNotNull(group.getEffectiveChildrens());
        w1.setInvertResult(Boolean.TRUE);
        assertNull(group.getEffectiveChildrens());
        
        TimeWindowNode w2 = new TimeWindowNode();
        w2.setName("w2");
        group.getTimeWindows().addAndSaveChildren(w2);
        w2.setTimePeriods("00:00-23:59");
        assertNull(group.getEffectiveChildrens());
        assertTrue(w2.start());
        assertNotNull(group.getEffectiveChildrens());
    }
}
