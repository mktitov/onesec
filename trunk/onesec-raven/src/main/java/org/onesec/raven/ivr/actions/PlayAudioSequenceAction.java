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
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioSequenceAction extends AbstractPlayAudioAction
{
    public final static String NAME = "Play audio sequence action";

    private final List<AudioFileNode> audioFiles;
    private final Node owner;
    private final boolean randomPlay;

    public PlayAudioSequenceAction(Node owner, List<AudioFileNode> audioFiles, boolean randomPlay)
    {
        super(NAME);
        this.audioFiles = audioFiles;
        this.owner = owner;
        this.randomPlay = randomPlay;
    }

    @Override
    protected AudioFileNode getAudioFile(IvrEndpointConversation conversation) 
    {
        Integer pos = (Integer) conversation.getConversationScenarioState().getBindings().get(
                this.getClass().getName()+owner.getId());
        if (pos!=null)
            ++pos;
        else if (randomPlay)
            pos = getRandomPosition();
        return pos==null || pos<0 || pos>audioFiles.size()? audioFiles.get(0) : audioFiles.get(pos);
    }

    private int getRandomPosition()
    {
        return ((int)(Math.random()*10*audioFiles.size())) % audioFiles.size();
    }
}
