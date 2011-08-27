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

import java.util.List;
import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.AudioFileRefNode;
import org.raven.RavenUtils;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioSequenceAction extends AbstractPlayAudioAction
{
    public final static String NAME = "Play audio sequence action";
    public final static String AUDIO_SEQUENCE_POSITION_BINDING = "audioSequencePosition";

    private final List<AudioFileRefNode> audioFiles;
    private final Node owner;
    private final boolean randomPlay;

    public PlayAudioSequenceAction(Node owner, List<AudioFileRefNode> audioFiles, boolean randomPlay)
    {
        super(NAME);
        this.audioFiles = audioFiles;
        this.owner = owner;
        this.randomPlay = randomPlay;
    }

    @Override
    protected AudioFileNode getAudioFile(IvrEndpointConversation conversation) 
    {
        String posId = RavenUtils.generateKey( AUDIO_SEQUENCE_POSITION_BINDING, owner);
        Bindings bindings = conversation.getConversationScenarioState().getBindings();
        Integer pos = (Integer) bindings.get(posId);
        if (pos!=null)
            ++pos;
        else if (randomPlay) {
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
                conversation.getOwner().getLogger().debug(logMess("Position (%s) selected randomly"));
            pos = getRandomPosition();
        }
        if (pos==null || pos<0 || pos>=audioFiles.size())
            pos = 0;
        bindings.put(posId, pos);
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(logMess("Selected audio file at position (%s)"));
        return audioFiles.get(pos).getAudioFileNode();
    }

    private int getRandomPosition()
    {
        return ((int)(Math.random()*10*audioFiles.size())) % audioFiles.size();
    }
}
