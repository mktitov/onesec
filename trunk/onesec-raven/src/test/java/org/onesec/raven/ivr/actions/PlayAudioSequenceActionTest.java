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

package org.onesec.raven.ivr.actions;

import org.raven.tree.Node;
import java.util.List;
import javax.script.Bindings;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.log.LogLevel;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioSequenceActionTest extends Assert
{
    @Test
    public void testRandom()
    {
        int seed = 19;
        for (int i=0; i<100; ++i) {
            int val = ((int)(Math.random() * 10 * seed)) % seed;
            System.out.println("!!!Val: "+val);
        }
    }

    @Test()
    public void getAudioFileTest()
    {
        List<AudioFile> audios = createStrictMock(List.class);
        Node owner = createMock(Node.class);
        IvrEndpointConversation conv = createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = createMock(ConversationScenarioState.class);
        Bindings bindings = createStrictMock(Bindings.class);

        expect(owner.getId()).andReturn(1).atLeastOnce();
        expect(owner.isLogLevelEnabled(anyObject(LogLevel.class))).andReturn(false).anyTimes();
        
        expect(audios.get(0)).andReturn(null);
        expect(audios.size()).andReturn(2);
        expect(audios.get(1)).andReturn(null);
        expect(audios.size()).andReturn(2);
        expect(audios.get(0)).andReturn(null);

        expect(conv.getConversationScenarioState()).andReturn(state).atLeastOnce();
        expect(conv.getOwner()).andReturn(owner).atLeastOnce();
        expect(state.getBindings()).andReturn(bindings).atLeastOnce();

        String key = PlayAudioSequenceAction.AUDIO_SEQUENCE_POSITION_BINDING+"_1";
        expect(bindings.get(key)).andReturn(null);
        expect(bindings.put(key, 0)).andReturn(null);
        expect(bindings.get(key)).andReturn(0);
        expect(bindings.put(key, 1)).andReturn(null);
        expect(bindings.get(key)).andReturn(1);
        expect(bindings.put(key, 0)).andReturn(null);
        
        replay(audios, owner, conv, state, bindings);

        PlayAudioSequenceAction action = new PlayAudioSequenceAction(owner, audios, false);

        action.getAudioFile(conv);
        action.getAudioFile(conv);
        action.getAudioFile(conv);

        verify(audios, owner, conv, state, bindings);
    }
}