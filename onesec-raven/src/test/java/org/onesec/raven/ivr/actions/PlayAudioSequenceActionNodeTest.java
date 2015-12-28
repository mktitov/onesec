/*
 * Copyright 2015 Mikhail Titov.
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

import java.io.ByteArrayInputStream;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.conv.ConversationScenarioState;
import org.raven.tree.DataFileException;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class PlayAudioSequenceActionNodeTest extends ActionTestCase {
    private PlayAudioSequenceActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new PlayAudioSequenceActionNode();
        actionNode.setName("action node");
        testsNode.addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void sequenceTest(
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state) 
        throws DataFileException  
    {
        final Bindings bindings = new SimpleBindings();
        new Expectations(){{
           conv.getConversationScenarioState(); result = state;
           state.getBindings(); result = bindings;
        }};
        AudioFile audio1 = addAudioFile("audio1", true);
        AudioFile audio2 = addAudioFile("audio2", false);
        AudioFile audio3 = addAudioFile("audio3", true);
        PlayAudioSequenceAction action = (PlayAudioSequenceAction) actionNode.createAction();
        assertSame(audio1, action.getAudio(conv));
        assertSame(audio3, action.getAudio(conv));
        assertSame(audio1, action.getAudio(conv));
    }
    
    private AudioFileNode addAudioFile(String name, boolean start) throws DataFileException {
        AudioFileNode audioFile = new AudioFileNode();
        audioFile.setName(name);
        actionNode.addAndSaveChildren(audioFile);
        audioFile.getAudioFile().setDataStream(new ByteArrayInputStream(new byte[]{1,2,3}));
        if (start)
            assertTrue(audioFile.start());
        return audioFile;
    }
}
