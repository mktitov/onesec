/*
 *  Copyright 2009 Mikhail Titov.
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
import java.util.Map;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.ConversationScenario;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class PlayAudioActionNode extends BaseNode implements IvrActionNode, Viewable
{
    public final static String AUDIO_FILE_ATTR = "audioFile";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFileNode audioFile;
    
    @Parameter
    private Integer playAtRepetition;

    public AudioFileNode getAudioFile()
    {
        return audioFile;
    }

    public void setAudioFile(AudioFileNode audioFile)
    {
        this.audioFile = audioFile;
    }

    public IvrAction createAction() {
        Integer _playAtRepetition = playAtRepetition;
        if (_playAtRepetition==null)
            return new PlayAudioAction(audioFile);
        else {
            SimpleBindings bindings = new SimpleBindings();
            formExpressionBindings(bindings);
            int repetitionCount = ((Number) bindings.get(ConversationScenario.REPEITION_COUNT_PARAM)).intValue();
            return repetitionCount-1==_playAtRepetition? new PlayAudioAction(audioFile) : null;
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
        throws Exception
    {
        AudioFileNode audio = audioFile;
        if (audio!=null)
            return audio.getViewableObjects(refreshAttributes);
        else
            return null;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public Integer getPlayAtRepetition() {
        return playAtRepetition;
    }

    public void setPlayAtRepetition(Integer playAtRepetition) {
        this.playAtRepetition = playAtRepetition;
    }
    
}
