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

import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.ivr.SayAnySubactionResult;
import org.onesec.raven.ivr.SubactionPauseResult;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyPauseSubactionTest {
    @Test
    public void test() throws Exception {
        SayAnyPauseSubaction pauseAction = new SayAnyPauseSubaction("10");
        SayAnySubactionResult res = pauseAction.getResult();
        assertTrue(res instanceof SubactionPauseResult);
        assertEquals(10, ((SubactionPauseResult)res).getPause());
    }
    
    @Test
    public void testSeconds() throws Exception {
        SayAnyPauseSubaction pauseAction = new SayAnyPauseSubaction("1s");
        SayAnySubactionResult res = pauseAction.getResult();
        assertTrue(res instanceof SubactionPauseResult);
        assertEquals(1000, ((SubactionPauseResult)res).getPause());
    }
}