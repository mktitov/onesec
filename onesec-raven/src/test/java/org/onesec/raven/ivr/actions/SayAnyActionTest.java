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
import org.junit.Before;
import org.onesec.raven.Constants;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SayAnyActionTest extends PlayActionTestHelper {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "logger", "", LoggerFactory.getLogger(PauseActionTest.class));
    private SayAnyActionNode actionNode;
    
    @Before
    @Override
    public void prepare() throws Exception {
        super.prepare();
        actionNode = new SayAnyActionNode();
        actionNode.setName("say any action");        
        testsNode.addAndSaveChildren(actionNode);
        actionNode.getAttr(SayAnyActionNode.WORDS_NODE_ATTR).setValue(Constants.TIME_WORDS_RESOURCE);
    }
    
    @Test
    public void test() throws Exception {
        conv.setFileName("target/say_any_1.wav");
        assertTrue(conv.start());
        actionNode.setActionsSequence("#wp=-200;r=(...)(...)(..)(..):9128672947");
//        actionNode.setActionsSequence("#wp=-150;r=(\\d\\d\\d)(\\d\\d\\d)(\\d\\d)(\\d\\d):9128672947 #12 ^минут #6 ^wp=-100:часов,прошлого,года @3s $wp=-150:123456.45");
        assertTrue(actionNode.start());
        SayAnyAction action = (SayAnyAction) actionNode.createAction();
        assertNotNull(action);
        action.execute(conv, null, logger);
        waitForAction(action);
    }
    
    @Test
    public void test2() throws Exception {
        conv.setFileName("target/say_any_2.wav");
        assertTrue(conv.start());
        actionNode.setActionsSequence("$z=y:0");
//        actionNode.setActionsSequence("#wp=-150;r=(\\d\\d\\d)(\\d\\d\\d)(\\d\\d)(\\d\\d):9128672947 #12 ^минут #6 ^wp=-100:часов,прошлого,года @3s $wp=-150:123456.45");
        assertTrue(actionNode.start());
        SayAnyAction action = (SayAnyAction) actionNode.createAction();
        assertNotNull(action);
        action.execute(conv, null, logger);
        waitForAction(action);
    }
    
 }
