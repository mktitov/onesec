/*
 * Copyright 2012 tim.
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

import org.junit.*;
import static org.junit.Assert.*;
import org.onesec.raven.BindingSourceNode;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.conv.ConversationScenario;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioActionNodeTest extends OnesecRavenTestCase {

    private BindingSourceNode parent;
    private PlayAudioActionNode action;
    
    @Before
    public void prepare() {
        parent = new BindingSourceNode();
        parent.setName("parent");
        tree.getRootNode().addAndSaveChildren(parent);
        assertTrue(parent.start());
        
        AudioFileNode audio = new AudioFileNode();
        audio.setName("audio");
        tree.getRootNode().addAndSaveChildren(audio);
        
        
        action = new PlayAudioActionNode();
        action.setName("action");
        parent.addAndSaveChildren(action);
        action.setAudioFile(audio);
        assertTrue(action.start());
    }
    
    @Test
    public void nullPlayAtRepetitionTest() {
        assertNotNull(action.createAction());
    }
    
    @Test
    public void playAtRepetitionTest1() {
        parent.addBinding(ConversationScenario.REPEITION_COUNT_PARAM, 1);
        action.setPlayAtRepetition(0);
        assertNotNull(action.createAction());
        action.setPlayAtRepetition(1);
        assertNull(action.createAction());
    }
    
}
