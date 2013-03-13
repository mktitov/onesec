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
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RegexpPatternTest extends OnesecRavenTestCase {
    
    @Test
    public void test() {
        RegexpPattern pattern = new RegexpPattern();
        pattern.setName("pattern");
        testsNode.addAndSaveChildren(pattern);
        pattern.setPattern("^(\\d\\d)(\\d\\d\\d).*");
        assertTrue(pattern.start());
        
        Collection<String> groups = pattern.matches("881234");
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertArrayEquals(new Object[]{"88", "123"}, groups.toArray());
    }
}
