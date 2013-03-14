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
package org.onesec.raven.ivr.actions;

import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class SayNumberActionNodeTest extends OnesecRavenTestCase {
    
    private SayNumberActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new SayNumberActionNode();
        actionNode.setName("actionNode");
        testsNode.addAndSaveChildren(actionNode);
    }
    
    @Test
    public void withoutPatternsTest() {
        actionNode.setNumber("123");
        assertTrue(actionNode.start());
        Collection<Long> nums = actionNode.getNumbersSequence();
        assertNotNull(nums);
        assertArrayEquals(new Object[]{new Long(123)}, nums.toArray());
    }
    
    @Test
    public void withPatternsTest() {
        actionNode.setNumber("9128672947");
        assertTrue(actionNode.start());
        RegexpPattern pattern = new RegexpPattern();
        pattern.setName("pattern1");
        actionNode.addAndSaveChildren(pattern);
        pattern.setPattern("(\\d\\d\\d)(\\d\\d\\d)(\\d\\d)(\\d\\d)");
        assertTrue(pattern.start());
        Collection<Long> nums = actionNode.getNumbersSequence();
        assertNotNull(nums);
        assertArrayEquals(new Object[]{912l, 867l, 29l, 47l}, nums.toArray());
    }
    
}
