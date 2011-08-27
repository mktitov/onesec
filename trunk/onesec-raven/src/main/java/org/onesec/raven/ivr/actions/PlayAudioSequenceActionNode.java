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
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.AudioFileRefNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, childNodes=AudioFileRefNode.class)
public class PlayAudioSequenceActionNode extends BaseNode implements IvrActionNode
{
    @NotNull @Parameter(defaultValue="false")
    private Boolean randomPlay;

    public Boolean getRandomPlay() {
        return randomPlay;
    }

    public void setRandomPlay(Boolean randomPlay) {
        this.randomPlay = randomPlay;
    }

    public IvrAction createAction()
    {
        List<AudioFileRefNode> files = NodeUtils.getChildsOfType(this, AudioFileRefNode.class);
        if (files.isEmpty())
            return null;
        return new PlayAudioSequenceAction(this, files, true);
    }
}
